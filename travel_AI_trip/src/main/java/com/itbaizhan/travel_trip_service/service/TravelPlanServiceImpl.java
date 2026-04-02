package com.itbaizhan.travel_trip_service.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itbaizhan.travel_trip_service.config.RedisKeyProperties;
import com.itbaizhan.travel_trip_service.config.RedisPromptProperties;
import com.itbaizhan.travel_trip_service.constant.TransConstant;
import com.itbaizhan.travel_trip_service.constant.TripConstant;
import com.itbaizhan.travel_trip_service.entity.ChangeTargets;
import com.itbaizhan.travel_trip_service.mapper.AiTripTokenMapper;
import com.itbaizhan.travel_trip_service.utils.PromptUtil;
import com.itbaizhan.travel_trip_service.utils.VerifyUtil;
import com.itbaizhan.travelcommon.mq.dto.TripProgressEvent;
import com.itbaizhan.travel_trip_service.sse.TripSseEmitterRegistry;
import com.itbaizhan.travel_trip_service.utils.MdUtils;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanRequest;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanResponse;
import com.itbaizhan.travelcommon.pojo.AiModuleConfig;
import com.itbaizhan.travelcommon.pojo.AiTripToken;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class TravelPlanServiceImpl implements TravelPlanService {

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;
    @Value("${model.chat}")
    private String modelChat;
    @Value("${model.precheck}")
    private String modelPrecheck;
    @Autowired
    private AiTripTokenMapper aiTripTokenService;
    @Autowired
    private TripsService tripsService;
    @Autowired
    private MdUtils mdUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedisKeyProperties redisKeyProperties;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Value("${demo.rocketmq.topic}")
    private String topic;
    @Autowired
    private TripSseEmitterRegistry emitterRegistry;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private PromptSelectService promptService;
    @Autowired
    private AiModuleConfigService aiModuleConfigService;
    @Autowired
    private RedisPromptProperties promptProperties;
    @Autowired
    private VerifyUtil verifyUtil;
    @Autowired
    @Qualifier(value = "tripTools")
    private ToolCallbackProvider toolCallbackProvider;


    private static final DateTimeFormatter DEFAULT_TIME = DateTimeFormatter.ofPattern("yyyy年MM月dd HH:mm:ss");

    private record EventRequest(String destination,Integer days){}
    private record GenerationPrecheckResult(ChangeTargets changeTargets, String refinedRequirement){}

    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local expire = tonumber(ARGV[2])
            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, expire)
            end
            if current > limit then
                redis.call('DECR', key)
                return -1
            end
            return current
            """;

    @Override
    //@TripLocks(user = LockMode.READ, userIdIndex = 2, waitMillis = 0, leaseMillis = 3000000)
    public void generatePlanStream(TravelPlanRequest request, SseEmitter emitter, Long userId) {
        if(!detect(request,emitter)){
            return;
        }
        String key = redisKeyProperties.buildCountKey(userId);
        // 使用 Lua 脚本保证原子性：limit=5, expire=1800s (30min)
        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class),
                Collections.singletonList(key),
                "5", "1800"
        );

        if (result == -1) {
            // 发送错误提示给前端
            sendEvent(emitter, "error", Map.of("message", "您正在生成的计划过多，请等待其他任务完成后再试（限制同时 5 个）"));
            emitter.complete(); // 关闭连接
            return; // 结束方法，不执行后续逻辑
        }
        //ChatClient chatClient = chatClientBuilder.build();

        CompletableFuture.runAsync(() -> {
            String tripId = UUID.randomUUID().toString();

            String tripIdKey = tripId + ":" + TripConstant.GENERATE_TRIP_ID;
            registerStream(tripIdKey,userId,emitter);
            EventRequest eventRequest = new EventRequest(request.getDestination(), request.getDays());
            AtomicLong seq = new AtomicLong(0);

            RLock userLock = redissonClient.getReadWriteLock("lock:user:" + userId + ":trips").readLock();
            RLock tripLock = redissonClient.getReadWriteLock("lock:trip:" + userId + ":" + tripId + ":" + TripConstant.NO_BACKUP).writeLock();


            boolean userLocked = false;
            boolean tripLocked = false;
            try {
                // 1. 尝试加锁
                try {
                    userLocked = userLock.tryLock(10000L, 300000L, TimeUnit.MILLISECONDS);
                    if (!userLocked) {
                        throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "系统繁忙，请稍后重试");
                    }
                    tripLocked = tripLock.tryLock(10000L, 300000L, TimeUnit.MILLISECONDS);
                    if (!tripLocked) {
                        throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "当前行程正在被修改或操作，请稍后");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "操作被中断");
                }


                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.GENERATE_TRIP_ID, "progress", "正在解析您的旅行需求...", seq.incrementAndGet());
                Thread.sleep(1000);

                // 2. 模拟搜索
                String searchMsg = "正在搜索 " + (request.getDestination() != null ? request.getDestination() : "目的地") + " 的热门景点和美食...";
                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.GENERATE_TRIP_ID, "progress", searchMsg, seq.incrementAndGet());
                // Thread.sleep(10000); // 移除模拟延迟，使用真实调用
                GenerationPrecheckResult changeTargets = null;
                if(StringUtils.hasText(request.getRawRequirement())){
                    changeTargets = precheckGenerationRequirement(request.getRawRequirement(),tripId,userId);
                    request.setRawRequirement(changeTargets.refinedRequirement);
                }else {
                    changeTargets = createDefaultChangeTargets();
                }

                String poiCandidatesJson = callChatClientContentWithRetry(
                        buildPoiSearchPrompt(request,changeTargets.changeTargets),
                        request.getDestination(),
                        () -> publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.GENERATE_TRIP_ID, "progress", "工具调用异常，正在重试检索...", seq.incrementAndGet()),
                        tripId,userId
                );

                poiCandidatesJson = cleanJson(poiCandidatesJson);
                //String poiCandidatesJson = "sdjo--------------------------------";
                // 3. 模拟路线规划
                String planningMsg = "正在规划最佳游玩路线...";
                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.GENERATE_TRIP_ID, "progress", planningMsg, seq.incrementAndGet());
                 Thread.sleep(1000);

                // 4. 生成最终 JSON
                String generatingMsg = "正在生成详细行程单...";
                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.GENERATE_TRIP_ID, "progress", generatingMsg, seq.incrementAndGet());
                Thread.sleep(1000);
                String promptText = buildPrompt(request, poiCandidatesJson, changeTargets.changeTargets);
                //System.out.println(promptText);
                String jsonResult = callChatClientContentWithRetry(
                        promptText,
                        request.getDestination(),
                        () -> publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.GENERATE_TRIP_ID, "progress", "工具调用异常，正在重试...", seq.incrementAndGet()),
                        tripId,userId
                );

                jsonResult = cleanJson(jsonResult);
                TravelPlanResponse response = objectMapper.readValue(jsonResult, TravelPlanResponse.class);
                response.setTripId(tripId);
                response.setTotalDays(request.getDays());
                response.setTravelerCount(request.getPeople());
                if (request.getStartTime() != null && request.getDays() != null) {
                    int i = 0;
                    LocalDateTime startTime = request.getStartTime();
                    for (TravelPlanResponse.DayPlan day : response.getDays()) {
                        day.setDateTime(startTime.plusDays(i).toLocalDate());
                        i++;
                    }
                }
                response.setTravelStyle(request.getTravelStyle());
                response.setCompleteStatus(TripConstant.DRAFT_TRIP_ID);

                // 保存到数据库
                tripsService.insertTrip(response, userId);
                // 插入redis缓存
                redisTemplate.opsForValue().set(redisKeyProperties.buildPlanKey(userId, response.getTripId())
                        , response, 7, TimeUnit.DAYS);
                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.GENERATE_TRIP_ID, "done", "已完成", seq.incrementAndGet());

            } catch (Exception e) {
                log.error("Error generating travel plan", e);
                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.GENERATE_TRIP_ID, "error", "生成行程失败，请稍后重试", seq.incrementAndGet());
            } finally {
                stringRedisTemplate.opsForValue().decrement(key);
                // 3. 释放锁
                if (tripLocked && tripLock.isHeldByCurrentThread()) {
                    tripLock.unlock();
                }
                if (userLocked && userLock.isHeldByCurrentThread()) {
                    userLock.unlock();
                }
            }
        });
    }

    private void registerStream(String tripIdKey,Long userId,SseEmitter emitter){
        emitterRegistry.register(userId, tripIdKey, emitter);
        emitter.onCompletion(() -> emitterRegistry.remove(userId, tripIdKey, emitter));
        emitter.onTimeout(() -> emitterRegistry.remove(userId, tripIdKey, emitter));
        emitter.onError((ex) -> emitterRegistry.remove(userId, tripIdKey, emitter));
    }

    /**
     * 对修改意见做轻量预检，输出结构化结果用于控制改动范围与识别不支持诉求。
     *
     * @param question 用户修改意见原文
     * @param trip     当前行程（用于补充目的地等上下文，可为空）
     * @param onRetry  预检失败时的重试回调（可为空）
     * @return 预检结果；失败时返回默认对象（不会抛出异常）
     */
    private RequirementPrecheck precheckModifyInstruction(String question, TravelPlanResponse trip, Runnable onRetry,String tripId,Long userId) {
        String prompt = buildModifyPrecheckPrompt(question, trip);
        try {
            String json = callChatClientContentNoToolWithRetry(prompt, onRetry,tripId,userId);
            //String json = "\n test \n";
            json = cleanJson(json);
            RequirementPrecheck parsed = objectMapper.readValue(json, RequirementPrecheck.class);
            /*JSONObject jsonObject = JSONObject.parseObject(json);
            String refinedRequirement = (String)jsonObject.get("changeTargets");
            JSONObject changeJson = JSONObject.parseObject(refinedRequirement);

            changeJson.forEach((key, value) -> {
                parsed.changeTargets().set(key,(Boolean)value);
            });*/
            return parsed == null ? RequirementPrecheck.empty() : parsed;
        } catch (Exception e) {
            return RequirementPrecheck.empty();
        }
    }


    /**
     * 对“纯 JSON 输出且禁止工具调用”的任务做轻量重试调用。
     *
     * @param promptText 提示词
     * @param onRetry    重试回调（可为空）
     * @return 模型输出内容（失败时返回空串，不会抛出异常）
     */
    private String callChatClientContentNoToolWithRetry(String promptText, Runnable onRetry,String tripId,Long userId) {
        String basePrompt = promptText == null ? "" : promptText;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                long start = System.currentTimeMillis();
                ChatResponse response = chatClient.prompt(new Prompt(new UserMessage(basePrompt)))
                        .options(DashScopeChatOptions.builder()
                                .withModel(modelPrecheck)
                                .build())
                        .call()
                        .chatResponse();
                long end = System.currentTimeMillis();
                if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                    Integer totalTokens = response.getMetadata().getUsage().getTotalTokens();
                    insertAiTripToken(tripId,userId,totalTokens,end - start,TripConstant.DRAFT_TRIP_ID);
                }
                return response == null ? "" : response.getResult().getOutput().getText();
            } catch (RuntimeException e) {
                if (attempt == 0 && onRetry != null) {
                    onRetry.run();
                }
            }
        }
        return "";
    }
    private GenerationPrecheckResult precheckGenerationRequirement(String rawRequirement,String tripId,Long userId) {
        if (rawRequirement == null || rawRequirement.isBlank()) {
            return createDefaultChangeTargets();
        }
        StringBuilder struct = new StringBuilder();
        StringBuilder capabilitiesDesc = new StringBuilder();
        for (AiModuleConfig aiModuleConfig : aiModuleConfigService.getAiModuleToolEnable()) {
            capabilitiesDesc.append("- ");
            capabilitiesDesc.append(aiModuleConfig.getDescription());
            capabilitiesDesc.append("\n");
            struct.append("\"").append(aiModuleConfig.getModuleKey()).append("\"")
                    .append(":").append("true").append(",");
        }
        struct.deleteCharAt(struct.length() - 1);


        String preGenPro = promptService.getPrompt(promptProperties.getPrecheckGeneration());
        String prompt = PromptUtil.renderPromptTemplate(preGenPro, Map.of(
                "capabilitiesDesc", capabilitiesDesc.toString(),
                "jsonStructure", struct.toString(),
                "rawRequirement", rawRequirement
        ));
        try {
            String json = callChatClientContentNoToolWithRetry(prompt, null,tripId,userId);
            /*String json = """
                    {
                    "refinedRequirement" : "不需要提供住宿",
                    "transport" : true,
                    "accommodation" : false,
                    "scenic" : true,
                    "catering" : true
                    }
                    """;*/
            json = cleanJson(json);

            return this.convertGenerationPrecheckResult(json);
        } catch (Exception e) {
            log.warn("Precheck generation requirement failed, defaulting to all enabled", e);
            return createDefaultChangeTargets();
        }
    }
    private GenerationPrecheckResult convertGenerationPrecheckResult(String json){
        JSONObject jsonObject = JSONObject.parseObject(json);
        String refinedRequirement = (String)jsonObject.remove("refinedRequirement");
        ChangeTargets targets = new ChangeTargets();
        jsonObject.forEach((key, value) -> {
            targets.set(key,(Boolean)value);
        });
        return new GenerationPrecheckResult(targets, refinedRequirement);
    }

    /**
     * 构造修改计划阶段的预检提示词（禁止工具，仅输出 JSON）。
     *
     * @param question 用户修改意见
     * @param trip     当前行程（可为空）
     * @return 提示词文本
     */
    private String buildModifyPrecheckPrompt(String question, TravelPlanResponse trip) {
        String q = question == null ? "" : question;
        String destination = trip == null || trip.getDestination() == null ? "" : trip.getDestination();
        String departure = trip == null || trip.getDeparture() == null ? "" : trip.getDeparture();
        String startDate = trip == null || trip.getStartDate() == null ? "" : trip.getStartDate().toString();
        String endDate = trip == null || trip.getEndDate() == null ? "" : trip.getEndDate().toString();
        String days = trip == null ? "" : String.valueOf(trip.getTotalDays());
        String people = trip == null ? "" : String.valueOf(trip.getTravelerCount());

        List<AiModuleConfig> aiModuleEnable = aiModuleConfigService.getAiModuleToolEnable();
        StringBuilder capabilitiesDesc = new StringBuilder();
        StringBuilder jsonStructure = new StringBuilder();
        jsonStructure.append("\"changeTargets\": {");
        for (AiModuleConfig aiModuleConfig : aiModuleEnable) {
            capabilitiesDesc.append("- ");
            capabilitiesDesc.append(aiModuleConfig.getDescription());
            capabilitiesDesc.append("\n");
            jsonStructure.append("\"").append(aiModuleConfig.getModuleKey()).append("\": false, ");
        }
        return PromptUtil.renderPromptTemplate(promptService.getPrompt(promptProperties.getPrecheckModify()), Map.of(
                "capabilitiesDesc", capabilitiesDesc.toString(),
                "jsonStructure", jsonStructure.toString(),
                "destination", destination,
                "departure", departure,
                "startDate", startDate,
                "endDate", endDate,
                "days", days,
                "people", people,
                "question", q,
                "poi_type",aiModuleConfigService.getPoiTypeWithAiModuleId(null)
        ));
    }

    /**
     * 对“需要工具调用”的任务做重试调用，并记录 token 消耗。
     *
     * @param promptText  提示词
     * @param destination 目的地
     * @param onRetry     重试回调（可为空）
     * @return 模型输出内容（失败时抛出异常）
     */
    private String callChatClientContentWithRetry(String promptText, String destination, Runnable onRetry,String tripId,Long userId) {
        String city = destination == null || destination.isBlank() ? "目的地" : destination.trim();
        String argHint = PromptUtil.renderPromptTemplate(promptService.getPrompt(promptProperties.getArgHint()), Map.of("city",city));
        String quotaHint = promptService.getPrompt(promptProperties.getQuotaHint());

        String composedPrompt = promptText;
        RuntimeException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                long start = System.currentTimeMillis();
                ChatResponse response = chatClient.prompt(new Prompt(new UserMessage(composedPrompt)))
                        .options(DashScopeChatOptions.builder().withModel(modelChat).build())
                        .toolCallbacks(toolCallbackProvider)
                        .call()
                        .chatResponse();
                long end = System.currentTimeMillis();
                if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                    Integer totalTokens = response.getMetadata().getUsage().getTotalTokens();
                    insertAiTripToken(tripId,userId,totalTokens,end - start,TripConstant.MODIFY_TRIP_ID);
                }
                return response == null ? "" : response.getResult().getOutput().getText();
            } catch (RuntimeException e) {
                last = e;
                if (!isRetryableToolCallError(e) || attempt == 2) {
                    throw e;
                }
                if (attempt == 0 && onRetry != null) {
                    onRetry.run();
                }
                if (isToolQuotaExceeded(e)) {
                    composedPrompt = promptText + "\n" + quotaHint;
                } else {
                    composedPrompt = composedPrompt + "\n" + argHint;
                }
            }
        }
        throw last == null ? new RuntimeException("callChatClientContentWithRetry failed") : last;
    }

    private void insertAiTripToken(String tripId,Long userId,Integer totalTokens,Long processingTime,Integer type) {
        AiTripToken aiTripToken  = new AiTripToken();
        aiTripToken.setTripId(tripId);
        aiTripToken.setUserid(userId);
        aiTripToken.setUseToken(totalTokens);
        aiTripToken.setProcessingTime(processingTime);
        aiTripToken.setType(type);
        aiTripTokenService.insert(aiTripToken);
    }

    private boolean isRetryableToolCallError(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null) {
                if (msg.contains("No content to map due to end-of-input")) {
                    return true;
                }
                if (msg.contains("No ToolCallback found for tool name:")) {
                    return true;
                }
                if (msg.contains("Functional bean with name") && msg.contains("does not exist in the context")) {
                    return true;
                }
                if (msg.contains("Error calling tool:")) {
                    return true;
                }
                if (msg.contains("Conversion from JSON to java.util.Map")) {
                    return true;
                }
                if (msg.contains("JsonEOFException")) {
                    return true;
                }
                if (msg.contains("Unexpected end-of-input")) {
                    return true;
                }
                if (msg.contains("CUQPS_HAS_EXCEEDED_THE_LIMIT")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private boolean isToolQuotaExceeded(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null) {
                if (msg.contains("CUQPS_HAS_EXCEEDED_THE_LIMIT")) {
                    return true;
                }
                if (msg.contains("HAS_EXCEEDED_THE_LIMIT")) {
                    return true;
                }
                if (msg.contains("Text Search failed")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * 发送行程生成进度事件到 RocketMQ（同时携带用于 Redis 快照回显的字段）。
     *
     * @param request  原始请求（用于填充目的地、天数）
     * @param userId   用户ID
     * @param tripId   行程ID
     * @param streamType 流类型：1=生成，2=修改
     * @param type     progress/done/error
     * @param status   状态描述
     * @param seq      递增序号（用于幂等）
     */
    private void publishTripProgressEvent(EventRequest request, Long userId, String tripId, Integer streamType, String type, String status, long seq) {
        TripProgressEvent event = new TripProgressEvent();
        event.setUserId(userId);
        event.setTripId(tripId);
        event.setStreamType(streamType);
        event.setType(type);
        event.setStatus(status);
        event.setSeq(seq);
        event.setTs(System.currentTimeMillis());
        String destination = request == null ? null : request.destination();
        event.setDestination(destination == null || destination.isBlank() ? "未知目的地" : destination);
        event.setTotalDays(request == null ? null : request.days());

        rocketMQTemplate.convertAndSend(topic, JSON.toJSONString(event));
    }

    @Override
    //@TripLocks(user = LockMode.READ, trip = LockMode.WRITE, userIdIndex = 3, tripIdIndex = 4, waitMillis = 10000L, leaseMillis = 300000L)
    public void chat(String question, String current, SseEmitter emitter, Long userId, String tripId, Integer isBackup) {
        if(StringUtils.hasText(current)) {
            tripsService.checkTravelPlanResponse(JSONObject.parseObject(current,TravelPlanResponse.class));
        }
        TravelPlanResponse trip = tripsService.getTripById(tripId, userId);
        if(trip == null) {
            sendEvent(emitter, "error",Map.of("message","当前计划不存在"));
            emitter.complete();
            return;
        }
        if(!trip.getCompleteStatus().equals(TripConstant.DRAFT_TRIP_ID)){
            throw new BusException(CodeEnum.TRIP_MODIFY_ERROR);
        }
        // Ai修改时，其他的关于这个trip的操作应该关闭

        CompletableFuture.runAsync(() -> {
            RLock userLock = redissonClient.getReadWriteLock("lock:user:" + userId + ":trips").readLock();
            RLock tripLock = redissonClient.getReadWriteLock("lock:trip:" + userId + ":" + tripId + ":" + TripConstant.NO_BACKUP).writeLock();
            String tripIdKey = tripId + ":" + TripConstant.MODIFY_TRIP_ID;

            registerStream(tripIdKey,userId,emitter);
            EventRequest eventRequest = new EventRequest(trip.getDestination(),trip.getTotalDays());
            AtomicLong seq = new AtomicLong(0);
            boolean userLocked = false;
            boolean tripLocked = false;
            boolean flag = false;
            try {
                // 1. 尝试加锁
                try {
                    userLocked = userLock.tryLock(10000L, 300000L, TimeUnit.MILLISECONDS);
                    if (!userLocked) {
                        throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "系统繁忙，请稍后重试");
                    }
                    tripLocked = tripLock.tryLock(10000L, 300000L, TimeUnit.MILLISECONDS);
                    if (!tripLocked) {
                        throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "当前行程正在被修改或操作，请稍后");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "操作被中断");
                }
                tripsService.updateCompleteStatus(tripId,userId,TripConstant.MODIFY_TRIP_ID);
                // 2. 执行业务逻辑

                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.MODIFY_TRIP_ID, "progress", "准备中...", seq.incrementAndGet());
                /*BackupInfo backup = null;
                try {
                    backup = tripsService.getBackup(tripId, userId);
                } catch (BusException e) {
                    System.out.println(e.getMessage() + "----------------跳过");
                }*/
                Thread.sleep(1000);

                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.MODIFY_TRIP_ID, "progress", "正在分析修改意见...", seq.incrementAndGet());
                //hread.sleep(10000);

                RequirementPrecheck precheck = precheckModifyInstruction(question, trip, null,tripId,userId);
                String precheckHint = buildModifyPrecheckHint(precheck);
                // 如果用户没有提出具体修改意见，则不将其加入提示词
                String modificationInstruction = (precheck.normalizedRequirement() == null || precheck.normalizedRequirement().isBlank())
                        ? ((question == null || question.isBlank()) ? "请根据上下文优化当前的旅行计划。": question)
                        : precheck.normalizedRequirement();
                String prompt = buildModifyPrompt(trip, current, modificationInstruction, precheckHint,precheck.changeTargets);
                /*if(backup != null) {
                    prompt = buildModifyPrompt(trip, current, modificationInstruction, precheckHint,precheck.changeTargets);
                }else {
                    prompt = buildModifyPrompt(trip, null, modificationInstruction, precheckHint,precheck.changeTargets);
                }*/

                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.MODIFY_TRIP_ID, "progress", "正在修改计划...", seq.incrementAndGet());
                String destination = trip.getDestination();
                String jsonResult = callChatClientContentWithRetry(
                        prompt,
                        destination,
                        null,
                        tripId,userId
                );
                //Thread.sleep(10000);
                jsonResult = cleanJson(jsonResult);
                TravelPlanResponse response = objectMapper.readValue(jsonResult, TravelPlanResponse.class);
                if (response == null) {
                    throw new BusException(CodeEnum.TRIP_AI_MODIFY_ERROR);
                }
                response.setTripId(tripId);
                //response.setCompleteStatus(1);
                response.setIsSave(TripConstant.NO_SAVE);
                tripsService.insertRedisTravel(response, userId, Objects.equals(isBackup, TripConstant.BACKUP) ? isBackup : TripConstant.NO_BACKUP);
                flag = true;
                //tripsService.updateCompleteStatus(tripId, userId, TripConstant.DRAFT_TRIP_ID);
                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.MODIFY_TRIP_ID, "done", "已完成", seq.incrementAndGet());

            } catch (Exception e) {
                 //sendEvent(emitter, "error", Map.of("message", e.getMessage()));
                log.error("Error updating travel plan", e);
                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.MODIFY_TRIP_ID, "error", "修改失败，请重试", seq.incrementAndGet());

                //emitter.complete();
            } finally {
                if(!flag) {
                    tripsService.updateCompleteStatus(tripId, userId, TripConstant.DRAFT_TRIP_ID);
                }
                // 3. 释放锁
                if (tripLocked && tripLock.isHeldByCurrentThread()) {
                    tripLock.unlock();
                }
                if (userLocked && userLock.isHeldByCurrentThread()) {
                    userLock.unlock();
                }
            }
        });
    }

    /**
     * 构造“修改行程”的提示词：在保留原 JSON 结构的前提下，允许按生成规则调用工具并返回完整 JSON。
     *
     * @param originalPlan            原旅行计划（数据库读取）
     * @param currentPlan         用户当前旅行计划（可能为空）
     * @param modificationInstruction 用户修改意见指令
     * @param precheckHint            预检结论（用于控制改动范围与防跑题）
     * @return 最终提示词
     */
    private String buildModifyPrompt(TravelPlanResponse originalPlan, String currentPlan, String modificationInstruction, String precheckHint, ChangeTargets changeTargets) throws Exception {
        String originalJson = objectMapper.writeValueAsString(originalPlan);

        String currentJson = (currentPlan == null || currentPlan.isBlank()) ? "{}" : currentPlan;
        String toolRules = null;

        if(changeTargets != null){
            toolRules = PromptUtil.renderPromptTemplate(promptService.getPrompt(promptProperties.getToolRestriction()),
                    Map.of("POI_TYPE", promptService.getAllGaodeType())) +
                    "\n" +
                    this.getPromptWithChangeTargets(changeTargets, aiModuleConfigService.getAiModuleToolEnable());
        }else {
            toolRules = promptService.getToolRestriction(); // TODO: PromptUtil.getAllToolPrompt() may also need update or replacement in future
        }

        String template = promptService.getPrompt(promptProperties.getModify());
        return PromptUtil.renderPromptTemplate(template, Map.of(
                "TOOL_RULES", toolRules,
                "ORIGINAL_JSON", originalJson,
                "CURRENT_JSON", currentJson,
                "MODIFICATION_INSTRUCTION", modificationInstruction == null ? "" : modificationInstruction,
                "PRECHECK_HINT", precheckHint == null ? "" : precheckHint,
                "POI_TYPE",promptService.getAllGaodeType()
        ));
    }


    /**
     * 将预检 JSON 转为短提示，减少 token 并用于提示词开关。
     *
     * @param precheck 预检结果
     * @return 预检摘要提示词
     */
    private String buildModifyPrecheckHint(RequirementPrecheck precheck) {
        if (precheck == null) return "";
        String risk = precheck.riskLevel() == null ? "LOW" : precheck.riskLevel();
        String unsupported = precheck.unsupportedRequests() == null ? "[]" : precheck.unsupportedRequests().toString();
        ChangeTargets t = precheck.changeTargets();
        String targets = t == null ? "{}" : (t.getTargets() == null ? "{}" : t.getTargets().toString());
        String normalized = precheck.normalizedRequirement() == null ? "" : precheck.normalizedRequirement();
        return """
                precheckRisk=%s
                isOffTopic=%s
                unsupportedRequests=%s
                changeTargets=%s
                normalized=%s
                """.formatted(risk, precheck.isOffTopic(), unsupported, targets, normalized);
    }


    @Override
    public void generateMd(String tripId, Long userId, SseEmitter emitter) {
        TravelPlanResponse response = tripsService.getTripById(tripId, userId);
        if(response == null || !response.getCompleteStatus().equals(TripConstant.DRAFT_TRIP_ID)){
            throw new BusException(CodeEnum.TRIP_STATUS_ERROR);
        }
        CompletableFuture.runAsync(() -> {
            RLock userLock = redissonClient.getReadWriteLock("lock:user:" + userId + ":trips").readLock();
            RLock tripLock = redissonClient.getReadWriteLock("lock:trip:" + userId + ":" + tripId + ":" + TripConstant.NO_BACKUP).readLock();

            boolean userLocked = false;
            boolean tripLocked = false;


            EventRequest eventRequest = new EventRequest(response.getDestination(),response.getTotalDays());
            AtomicLong seq = new AtomicLong(0);
            String sseKey = tripId + ":" + TripConstant.PROCESSING_MD_ID;
            registerStream(sseKey,userId,emitter);
            boolean status = true;

            try {
                // 1. 尝试加锁
                try {
                    userLocked = userLock.tryLock(10000L, 300000L, TimeUnit.MILLISECONDS);
                    if (!userLocked) {
                        throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "系统繁忙，请稍后重试");
                    }
                    tripLocked = tripLock.tryLock(10000L, 300000L, TimeUnit.MILLISECONDS);
                    if (!tripLocked) {
                        throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "当前行程正在被修改或操作，请稍后");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), "操作被中断");
                }
                try {
                    String ongoingKey = redisKeyProperties.buildOngoingPlanKey(userId);
                    redisTemplate.opsForHash().delete(ongoingKey, tripId + ":" + TripConstant.PROCESSING_MD_ID);
                } catch (Exception ignored) {
                }
                tripsService.updateCompleteStatus(tripId,userId,TripConstant.PROCESSING_MD_ID);
                // 1. 获取行程数据

                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.PROCESSING_MD_ID, "progress", "正在生成...", seq.incrementAndGet());
                //Thread.sleep(100000);

                String tripJson = objectMapper.writeValueAsString(response);

                // 2. 调用 AI 生成 Markdown 内容
                String prompt = PromptUtil.renderPromptTemplate(promptService.getPrompt(promptProperties.getGenerateMd()),
                        Map.of("tripJson", tripJson));
                long start = System.currentTimeMillis();
                ChatResponse chatResponse = chatClient.prompt(new Prompt(new UserMessage(prompt)))
                        .options(DashScopeChatOptions.builder().withModel(modelChat).build())
                        .call()
                        .chatResponse();
                long end = System.currentTimeMillis();
                if (chatResponse != null && chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                    Integer totalTokens = chatResponse.getMetadata().getUsage().getTotalTokens();
                    insertAiTripToken(tripId,userId,totalTokens,end - start,TripConstant.Complete_trip_ID);
                }
                String markdownContent = chatResponse == null ? "" : chatResponse.getResult().getOutput().getText();

                // 3. 上传 Markdown 文件到 MinIO
                MdFile mdFile = generateAndUploadMarkdown(userId, markdownContent, tripId);
                String key = redisKeyProperties.buildMdKey(userId, tripId);
                stringRedisTemplate.opsForValue().set(key, mdFile.url(), 1, TimeUnit.DAYS);
                tripsService.saveMdUrl(tripId, userId, mdFile.path());
                //response.setCompleteStatus();
                //tripsService.updateCompleteStatus(tripId, userId, 2);
                // 4. 发送结果给前端
                //sendEvent(emitter, "done", mdFile.url());
                //emitter.complete();

                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.PROCESSING_MD_ID, "done", "生成成功", seq.incrementAndGet());
            } catch (Exception e) {
                status = false;

                log.error("Error generating markdown for trip: " + tripId, e);
                /*sendEvent(emitter, "error", Map.of("message", "生成攻略失败，请稍后重试"));
                emitter.completeWithError(e);*/
                publishTripProgressEvent(eventRequest, userId, tripId, TripConstant.PROCESSING_MD_ID, "error", "生成失败", seq.incrementAndGet());

            }finally {
                if(status){
                    tripsService.updateCompleteStatus(tripId,userId,TripConstant.Complete_trip_ID);
                }else {
                    tripsService.updateCompleteStatus(tripId,userId,TripConstant.DRAFT_TRIP_ID);
                }
                // 3. 释放锁
                if (tripLocked && tripLock.isHeldByCurrentThread()) {
                    tripLock.unlock();
                }
                if (userLocked && userLock.isHeldByCurrentThread()) {
                    userLock.unlock();
                }
            }
        });
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            // ignore
        }
    }

    private MdFile generateAndUploadMarkdown(Long userId, String markdown, String tripId) throws Exception {
        byte[] bytes = markdown == null ? new byte[0] : markdown.getBytes(StandardCharsets.UTF_8);
        String objectKey = mdUtils.uploadMd(bytes, "text/markdown; charset=utf-8", userId, tripId);
        String url = mdUtils.getTemporaryUrlMd(objectKey);
        return new MdFile(objectKey, url);
    }

    private record MdFile(String path, String url) {
    }

    private record RequirementPrecheck(
            boolean isOffTopic,
            String riskLevel,
            List<String> unsupportedRequests,
            List<String> messages,
            ChangeTargets changeTargets,
            TransportPreference transportPreference,
            Constraints constraints,
            String normalizedRequirement
    ) {
        static RequirementPrecheck empty() {
            return new RequirementPrecheck(false, "LOW", List.of(), List.of(), null, null, null, "");
        }
        private record TransportPreference(
                String mode,
                List<String> avoid,
                String notes
        ) {
        }

        private record Constraints(
                boolean nearAirport,
                boolean lessWalking,
                boolean avoidSpicy,
                boolean replaceAccommodation
        ) {
        }
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        if (text.contains("```json")) {
            text = text.substring(text.indexOf("```json") + 7);
            if (text.contains("```")) text = text.substring(0, text.indexOf("```"));
        } else if (text.contains("```")) {
            text = text.substring(text.indexOf("```") + 3);
            if (text.contains("```")) text = text.substring(0, text.indexOf("```"));
        }
        return text.trim();
    }

    private String getPromptWithChangeTargets(ChangeTargets changeTargets,List<AiModuleConfig> allPoiTypeAiModule) {
        StringBuilder tool = new StringBuilder();
        if(allPoiTypeAiModule != null && !allPoiTypeAiModule.isEmpty()){
            for (AiModuleConfig aiModuleConfig : allPoiTypeAiModule) {
                if(changeTargets.isEnabled(aiModuleConfig.getModuleKey())){
                    tool.append(promptService.getPrompt(aiModuleConfig.getPromptId())).append('\n');
                }
            }
        }
        return tool.toString();
    }

    /**
     * 构造 POI 检索提示词。
     *
     * @param req 生成计划请求
     * @return 提示词文本
     */
    private String buildPoiSearchPrompt(TravelPlanRequest req,ChangeTargets targets) {
        String raw = req == null || req.getRawRequirement() == null ? "" : req.getRawRequirement();
        
/*        String diningSection = targets.isEnabled("dining") ? poiDiningPrompt : "";
        String attractionSection = targets.isEnabled("attraction") ? poiAttractionPrompt : "";*/


        return PromptUtil.renderPromptTemplate(promptService.getPrompt(promptProperties.getPoiSearch()), Map.of(
                "gaoDe_type",aiModuleConfigService.getPoiTypeWithAiModuleId(targets.getTargets()),
                "TOOL_JSON",this.getPromptWithChangeTargets(targets,aiModuleConfigService.getAllPoiTypeAiModule()),
                "destination", req.getDestination() == null ? "" : req.getDestination(),
                "startTime", req.getStartTime() == null ? "" : req.getStartTime().toString(),
                "days", req.getDays() == null ? "3" : req.getDays().toString(),
                "people", req.getPeople() == null ? "2" : req.getPeople().toString(),
                "budget", req.getBudget() == null ? "舒适标准" : req.getBudget(),
                "travelStyle", req.getTravelStyle() == null ? "休闲" : req.getTravelStyle().toString(),
                "rawRequirement", raw
        ));
    }

    private String handleGenTools(ChangeTargets targets){
        String prompt = null;
        if(!targets.isAllBoolean(false)){
            prompt = promptService.getAllGenPrompt();
        }else {
            /*StringBuilder sb = new StringBuilder();
            for (AiModuleConfig aiModuleConfig : ) {
                if(targets.isEnabled(aiModuleConfig.getModuleKey())){
                    sb.append(promptService.getPrompt(aiModuleConfig.getPromptId())).append("\n");
                }
            }*/
            prompt = PromptUtil.renderPromptTemplate(promptService.getPrompt(promptProperties.getGenerate())
                    , Map.of("TOOL_RULES", this.getPromptWithChangeTargets(targets,aiModuleConfigService.getAiModuleToolEnable())));
        }
        return prompt;
    }

    private String buildPrompt(TravelPlanRequest req, String poiCandidatesJson, ChangeTargets targets) {
        String prompt = handleGenTools(targets);


        String flightInfo = "";
        if (TransConstant.AIRPLANE.equals(req.getTransportation())) {
            flightInfo = String.format("\\n出发机场：%s\\n到达机场：%s",
                    req.getFlightDepAirport() != null ? req.getFlightDepAirport() : "未指定",
                    req.getFlightArrAirport() != null ? req.getFlightArrAirport() : "未指定");
        }
        StringBuilder stringBuilder = new StringBuilder();
        targets.getTargets().forEach((k,v) -> {
            stringBuilder.append(k).append(":").append(v).append("\n");
        });

        // 自驾相关提示
        /*String selfDrivingPrompt = "";
        String trans = req.getTransportation() != null ? req.getTransportation().toLowerCase() : "";
        if (trans.contains("car") || trans.contains("自驾") || trans.contains("drive") || trans.contains("租车")) {
            selfDrivingPrompt = """
                    \\n【自驾特别指令】
                    用户选择了自驾/租车出行。请务必调用地图/路线规划工具：
                    1. 规划每日的详细驾驶路线，确保路线顺路，不绕路。
                    2. 在每日行程或活动中，明确标注“预计驾驶距离(km)”和“预计驾驶时间”。
                    3. 推荐沿途值得停留的风景点或服务区。
                    """;
        }*/
        Map<String, String> map = new HashMap<>();
        map.put("gaoDe_type",aiModuleConfigService.getPoiTypeWithAiModuleId(targets.getTargets()));
        map.put("POI_TYPE", promptService.getAllGaodeType());
        map.put("startTime", req.getStartTime() != null ? req.getStartTime().toString() : "近期");
        map.put("destination", req.getDestination());
        map.put("departure", req.getDeparture());
        map.put("days", req.getDays() == null ? "3" : req.getDays().toString());
        map.put("people", req.getPeople() == null ? "2" : req.getPeople().toString());
        map.put("budget", req.getBudget());
        map.put("travelStyle", req.getTravelStyle() == null ? "休闲" : req.getTravelStyle().toString());
        map.put("transportation", req.getTransportation() == null ? "未指定交通工具" : req.getTransportation());
        map.put("flightInfo", flightInfo);
        map.put("rawRequirement", req.getRawRequirement() == null ? "" : req.getRawRequirement());
        map.put("poiCandidatesJson", poiCandidatesJson == null ? "[]" : poiCandidatesJson);
        map.put("PRECHECK_HINT",stringBuilder.toString());


        return PromptUtil.renderPromptTemplate(prompt, map);
    }

    private GenerationPrecheckResult createDefaultChangeTargets() {
        ChangeTargets t = new ChangeTargets();
        List<AiModuleConfig> aiModuleEnable = aiModuleConfigService.getAiModuleToolEnable();
        for (AiModuleConfig aiModuleConfig : aiModuleEnable) {
            t.set(aiModuleConfig.getModuleKey(),true);
        }
        return new GenerationPrecheckResult(t,"");
    }

    private boolean detect(TravelPlanRequest request,SseEmitter emitter){
        if(!insureRequest(request)){
            sendEvent(emitter, "done", Map.of("message", "规划没有填写完成: "));
            emitter.complete();
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(request.getStartTime())) {
            sendEvent(emitter, "done", Map.of("message", "时间不得早于当前时间: " + now.format(DEFAULT_TIME)));
            emitter.complete();
            return false;
        }
        if(TransConstant.AIRPLANE.equals(request.getTransportation())){
            if(request.getFlightArrAirport() == null || request.getFlightDepAirport() == null){
                sendEvent(emitter, "done", Map.of("message", "选择飞机，必须填写机场"));
                emitter.complete();
                return false;
            }
            if(verifyUtil.verifyNotAirport(request.getFlightArrAirport()) || verifyUtil.verifyNotAirport(request.getFlightDepAirport())){
                sendEvent(emitter, "done", Map.of("message", "机场填写错误"));
                emitter.complete();
                return false;
            }
        }
        return true;
    }
    private boolean insureRequest(TravelPlanRequest travelPlanRequest) {
        if(travelPlanRequest == null) {
            return false;
        }
        if(travelPlanRequest.getDeparture() == null || travelPlanRequest.getDestination() == null) {
            return false;
        }
        String dep = travelPlanRequest.getDeparture().endsWith("市") ? travelPlanRequest.getDeparture().trim() :
                (travelPlanRequest.getDeparture().trim() + "市");
        String des = travelPlanRequest.getDestination().endsWith("市") ? travelPlanRequest.getDestination().trim() :
                (travelPlanRequest.getDestination().trim() + "市");
        if(verifyUtil.verifyNotCity(dep) || verifyUtil.verifyNotCity(des)) {
            return false;
        }
        if(travelPlanRequest.getBudget() == null || travelPlanRequest.getPeople() == null) {
            return false;
        }
        if(travelPlanRequest.getTravelStyle() == null || travelPlanRequest.getDays() == null) {
            return false;
        }
        return travelPlanRequest.getStartTime() != null;
    }
    public String getString(){
        return """
                # 上海三日情侣休闲游：外滩夜色、豫园雅韵与舌尖浪漫
                
                > **行程概览** \s
                > - **目的地**：上海 \s
                > - **旅行天数**：3天2晚（2026年3月7日—3月9日） \s
                > - **出行人数**：2人（情侣） \s
                > - **旅行风格**：豪华享受 · 情侣专属 · 文化漫步 · 美食体验 \s
                > - **总预算**：¥6,500（人均约¥1,083/天） \s
                > - **天气概况**：多云转阴，气温4–13℃，体感微凉，适宜轻装慢游 \s
                
                ---
                
                ## 第1天：抵达魔都 · 外滩夜色初遇
                
                **主题**：云端降落，黄浦江畔的初见浪漫 \s
                
                ### 🌤 上午：乘机抵沪，开启精致旅程
                - **航班信息**：HU7601 海南航空 \s
                  - 北京首都机场 T3 → 上海虹桥机场 T2 \s
                  - 起飞：07:25｜抵达：09:35（飞行时长约1小时40分钟） \s
                - 抵达后建议打车或预约专车前往酒店办理入住，轻松开启旅程。
                
                ### 🏙 下午：漫步外滩，穿越百年风云
                - **景点**：[外滩](http://store.is.autonavi.com/showpic/b9c402b7d34ea98654cc915e567761dd) \s
                  - **开放时间**：全天开放（建议14:00–17:00游览） \s
                  - **亮点**：万国建筑博览群与陆家嘴现代天际线隔江对望，历史与未来在此交汇。 \s
                  - **情侣Tips**：沿中山东一路缓步而行，在和平饭店前合影，或登上外白渡桥感受《情深深雨蒙蒙》的浪漫场景。 \s
                  - **评分**：⭐️ 4.9 / 5（高德用户口碑）
                
                ### 🌆 傍晚至夜晚：米其林风味晚餐 + 五星级酒店安眠
                - **晚餐**：[叙宴（上海首店）](https://aos-comment.amap.com/B0KA6HCKJ5/comment/77E0FFB0_C2CE_4A8F_97AB_F1E5C0ACFB9F_L0_001_1536_204_1765803849514_49133479.jpg) \s
                  - **地址**：汶水路40号 \s
                  - **人均消费**：¥525 \s
                  - **推荐菜**：金风抚月温菽乳、福禄樱桃俏鹅肝、玄山素雪焗牛肉 \s
                  - **氛围**：新中式美学空间，私密雅致，适合情侣约会 \s
                  - **预订建议**：提前致电 400-996-7855 预留靠窗位 \s
                - **住宿**：[上海大酒店](https://store.is.autonavi.com/showpic/835e6f654a62664190ba0aeecce21982) \s
                  - **地址**：九江路505号（近南京东路，步行至外滩约10分钟） \s
                  - **特色**：老牌五星级，英伦风格建筑，服务周到，地理位置绝佳 \s
                  - **入住时间**：20:30起（连住两晚）
                
                > 💡 **当日小贴士**：外滩傍晚风大，建议携带薄外套；叙宴距离外滩约6公里，建议打车前往（约20分钟）。
                
                ---
                
                ## 第2天：豫园寻幽 · 江南园林里的慢时光
                
                **主题**：在明清园林与市井烟火中，共度诗意一日 \s
                
                ### 🌸 上午：探秘豫园，一步一景皆画卷
                - **景点**：[上海豫园](http://store.is.autonavi.com/showpic/faeb0264854cd82fbb315cb2ccacea0f) \s
                  - **开放时间**：08:30–17:00（建议09:30入园避人流） \s
                  - **门票**：成人¥40（现场可购） \s
                  - **亮点**：九曲桥、点春堂、玉玲珑太湖石，亭台楼阁错落有致，尽显江南园林精巧。 \s
                  - **情侣玩法**：在湖心亭茶室小憩，共饮一壶龙井，看锦鲤戏水。
                
                ### 🥟 中午：城隍庙小吃巡礼
                - **景点**：[上海城隍庙](http://store.is.autonavi.com/showpic/518f824f343115a063effa52ea4cb224) \s
                  - **地址**：方浜中路249号（紧邻豫园） \s
                  - **开放时间**：全天开放（商铺约09:00–20:00） \s
                  - **必尝美食**：南翔小笼包、宁波汤圆、梨膏糖、五香豆 \s
                  - **注意**：景区内人流密集，建议浅尝即止，保留胃口给晚餐。
                
                ### 🌙 晚餐：地道清真风味 · 贯贯吉餐厅
                - **餐厅**：[贯贯吉清真餐厅](http://store.is.autonavi.com/showpic/2bed688eb61c7bd2f43b1c21bc5ae1c3) \s
                  - **地址**：浙江中路70-2号（近人民广场） \s
                  - **人均消费**：¥105 \s
                  - **招牌菜**：羊肉串、大盘鸡、手抓羊肉、自制酸奶、八宝茶 \s
                  - **特色**：老字号清真馆子，食材新鲜，分量足，性价比极高 \s
                  - **交通**：从豫园打车约15分钟，或地铁10号线→8号线直达
                
                > 💡 **当日小贴士**：豫园周末及节假日人潮汹涌，工作日相对清净；穿舒适平底鞋，园内多石板路与台阶。
                
                ---
                
                ## 第3天：人民广场晨光 · 满载回忆返程
                
                **主题**：都市绿洲的最后一瞥，带着甜蜜回家 \s
                
                ### ☀ 上午：人民广场文化漫步
                - **景点**：人民广场（免费开放） \s
                  - **推荐路线**：上海博物馆（青铜器/书画展）→ 上海城市规划馆 → 喷泉广场 \s
                  - **时间安排**：建议07:30–09:00（避开人流高峰） \s
                  - **情侣彩蛋**：在广场鸽群中牵手散步，或在博物馆咖啡厅共享一杯热拿铁。
                
                ### ✈ 中午：乘机返京，结束完美旅程
                - **航班信息**：MU5099 中国东方航空 \s
                  - 上海虹桥机场 T2 → 北京首都机场 T3 \s
                  - 起飞：07:00｜抵达：09:20 \s
                - **提醒**：需提前2小时抵达机场（约05:00出发），建议前一晚整理行李，预留充足时间。
                
                > 💡 **返程提示**：航班较早，可提前联系酒店安排叫醒服务与打包早餐。
                
                ---
                
                ## 🛏 酒店信息
                
                | 酒店名称 | 上海大酒店 |
                |--------|----------|
                | **星级** | ★★★★★（五星级） |
                | **地址** | 上海市黄浦区九江路505号 |
                | **电话** | 021-53538888 |
                | **优势** | 地处南京东路商圈，步行至外滩10分钟，地铁2/10号线南京东路站直达 |
                | **设施** | 行政酒廊、健身房、中西餐厅、24小时客房服务 |
                
                ---
                
                ## ✈ 交通信息
                
                | 航段 | 航班号 | 航空公司 | 时间 | 舱位 |
                |------|--------|----------|------|------|
                | 去程 | HU7601 | 海南航空 | 3月7日 07:25–09:35 | 经济舱 |
                | 返程 | MU5099 | 中国东方航空 | 3月9日 07:00–09:20 | 经济舱 |
                
                > **交通贴士**： \s
                > - 虹桥机场至市区：打车约¥80–100，地铁2号线直达市中心（约40分钟） \s
                > - 市内出行：推荐使用滴滴或高德打车，景点间车程普遍在15–30分钟内
                
                ---
                
                ## 🧥 实用旅行贴士
                
                ### 🌦 天气与穿衣建议
                - **气温**：4–13℃，早晚温差大，体感偏凉 \s
                - **推荐穿搭**： \s
                  - 白天：长袖衬衫/针织衫 + 风衣/薄呢大衣 \s
                  - 夜晚：加绒内搭 + 围巾（外滩江风较大） \s
                  - 鞋履：防滑平底鞋或短靴（豫园石板路湿滑）
                
                ### 💰 预算分配参考（总¥6,500）
                - **交通**：约¥3,200（往返机票） \s
                - **住宿**：约¥2,000（两晚五星级） \s
                - **餐饮+门票**：约¥1,300（含两顿正餐及小吃）
                
                ### ⚠ 注意事项
                - 所有餐厅建议提前电话预约，尤其是叙宴等热门店 \s
                - 豫园与城隍庙区域谨防“拉客”和高价纪念品，理性消费 \s
                - 随身携带身份证，酒店入住及机场安检必需 \s
                - 使用支付宝/微信支付全覆盖，无需大量现金
                
                ---
                
                > **结语**： \s
                > 这趟上海三日之旅，既有外滩的璀璨夜色，也有豫园的静谧雅致；既有米其林的精致仪式感，也有街头巷尾的人间烟火。在春寒料峭的三月，与爱人携手漫步于这座摩登与古典交织的城市，每一刻都值得珍藏。
                """;
    }
}
