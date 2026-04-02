package com.itbaizhan.travel_ai_service.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itbaizhan.travel_ai_service.agent.TripAgent;
import com.itbaizhan.travel_ai_service.config.PromptProperties;
import com.itbaizhan.travel_ai_service.config.RedissonLock;
import com.itbaizhan.travel_ai_service.rag.QueryRewriter;
import com.itbaizhan.travel_ai_service.rag.TravelRagCustomAdvisorFactory;
import com.itbaizhan.travel_ai_service.service.AiAssistantServiceImpl;
import com.itbaizhan.travel_ai_service.service.FaqServiceImpl;
import com.itbaizhan.travelcommon.info.AiMessageEvent;
import com.itbaizhan.travelcommon.pojo.AiConversations;
import com.itbaizhan.travelcommon.pojo.AiMessages;
import com.itbaizhan.travelcommon.pojo.TravelFaq;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.FileService;
import com.itbaizhan.travelcommon.util.JWTUtil;
import com.itbaizhan.travelcommon.util.SensitiveTextUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.Model;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.memory.ChatMemory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/assistant")
public class AiAssistantController {
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private AiAssistantServiceImpl aiAssistantService;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> audioTranscriptionModel;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private PromptProperties promptProperties;
    @Autowired
    private FaqServiceImpl faqService;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private QueryRewriter queryRewriter;
    @Autowired
    private RedissonLock redissonLock;
    @Autowired
    private SensitiveTextUtil sensitiveTextUtil;
    @DubboReference
    private FileService fileService;
    @Value("{spring.ai.imageOption}")
    private String imageOption;

    public String assistantBlockHandle(){
        return "目前访问人数过多，请稍后再试";
    }

    @PostMapping(value = "/audio", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SentinelResource(value = "audioTranscription", blockHandler = "assistantBlockHandle")
    public Flux<String> transcription(@RequestParam("file") MultipartFile file,
                                            @RequestHeader String authorization) {
        verifyToken(authorization);
        if (audioTranscriptionModel == null) {
            throw new BusException(CodeEnum.SYSTEM_ERROR.getCode(), "语音识别服务未启用");
        }
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource);
            AudioTranscriptionResponse response = audioTranscriptionModel.call(prompt);
            // 保持前端一致性，使用流式返回
            return Flux.just(response.getResult().getOutput());
        } catch (IOException e) {
            throw new BusException(CodeEnum.SYSTEM_ERROR);
        }
    }

    @PostMapping(value = "/image", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SentinelResource(value = "imageTranscription", blockHandler = "assistantBlockHandle")
    public Flux<String> imageRecognition(@RequestParam(value = "sessionId") String sessionId,
                                         @RequestParam(value = "file") MultipartFile file,
                                         @RequestHeader String authorization) throws IOException {
        Map<String, Object> map = verifyToken(authorization);
        Long userId = (Long) map.get("userId");
        if(!aiAssistantService.existConversation(sessionId)) {
            throw new BusException(CodeEnum.AI_SESSION_IS_EXIST_ERROR);
        }
        if (redissonLock.lock(sessionId,20000)) {
            try {
                // 2. 验证文件
                if (file == null || file.isEmpty()) {
                    throw new BusException(CodeEnum.AI_IMAGE_ERROR);
                }

                // 3. 验证文件类型
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new BusException(CodeEnum.AI_IMAGE_FORMAT_ERROR);
                }

                byte[] bytes = file.getBytes();

                String imgUrl = fileService.upload(bytes, file.getOriginalFilename(),(Long) map.get("userId"));
                // 4. 解析 MimeType
                MimeType mimeType;
                try {
                    mimeType = MimeType.valueOf(contentType);
                } catch (InvalidMimeTypeException e) {
                    mimeType = MimeTypeUtils.IMAGE_JPEG;
                }
                try {
                    Media media = new Media(mimeType,file.getResource());
                    // 构建包含图片的用户消息
                   /* UserMessage userMessage = UserMessage.builder().text(promptProperties.getImagePrompt())
                            .media(List.of(Media.builder().mimeType(mimeType).data(dataUri).build())).build();*/
                    UserMessage userMessage = UserMessage.builder().text(promptProperties.getImagePrompt())
                            .media(media)
                            .metadata(Map.of(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE))
                            .build();

                    DashScopeChatOptions options = DashScopeChatOptions.builder()
                            .withModel(imageOption)
                            .withMultiModel(true)
                            .withStream(true)
                            .build();
                    // 手动管理内存：仅在流结束时添加 AI 回答，不添加带图片的用户消息（避免 token 消耗过大）
                    StringBuilder sb = new StringBuilder();
                    long startTime = System.currentTimeMillis();
                    AtomicInteger tokensUsed = new AtomicInteger(0);
                    return chatClient.prompt(new Prompt(userMessage,options))
                            //.system(promptProperties.getSystemPrompt())
                        .stream()
                        .chatResponse()
                        .map(response -> {
                            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                                return "";
                            }
                            String text = response.getResult().getOutput().getText();
                            if (text != null) {
                                sb.append(text);
                            }
                            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                                Integer totalTokens = response.getMetadata().getUsage().getTotalTokens();
                                if (totalTokens != null) {
                                    tokensUsed.set(totalTokens);
                                }
                            }
                            return text == null ? "" : text;
                        })
                        .filter(chunk -> !chunk.isEmpty())
                        .doOnComplete(() -> {
                            if (sessionId != null && !sb.isEmpty()) {
                                long processingTime = System.currentTimeMillis() - startTime;
                                // 1. 仅添加 AI 的回答到聊天记忆中（保持上下文纯净，不含图片数据）
                                chatMemory.add(sessionId, new AssistantMessage(sb.toString()));

                                // 2. [新增] 发布事件以持久化消息
                                // 将图片 URL 格式化为 Markdown 图片链接，这样前端在渲染历史记录时可以直接显示图片
                                String questionWithImage = "![](" + imgUrl + ")";

                                // 生成一个消息 ID
                                String msgUid = java.util.UUID.randomUUID().toString();

                                // 发布事件，监听器会将这条记录保存到数据库
                                publisher.publishEvent(new AiMessageEvent(this, sessionId, questionWithImage, sb.toString(), msgUid,1,tokensUsed.get(),processingTime,userId));
                            }
                        });
                } catch (Exception e) {
                    throw new BusException(CodeEnum.SYSTEM_ERROR);
                }
            } finally {
                redissonLock.unlock(sessionId);
            }
        }else {
            return null; //如果拿不到锁，线程已经中断了，这里执行不到
        }
    }

    @PostMapping("/session")
    public BaseResult<AiConversations> createSession(@RequestBody com.itbaizhan.travelcommon.AiSessionDto.SessionRequest sessionRequest,
                                                     @RequestHeader String authorization){
        Map<String, Object> verify = verifyToken(authorization);
        sensitiveTextUtil.checkUserInput(sessionRequest != null ? sessionRequest.getTitle() : null);
        Long userId = (Long) verify.get("userId");
        AiConversations session = aiAssistantService.createSession(sessionRequest, userId);
        return BaseResult.success(session);
    }


    /**
     * AI 助手聊天接口
     * 使用 Server-Sent Events (SSE) 流式返回结果
     * @param message 用户消息
     * @return 流式响应内容
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SentinelResource(value = "chatTranscription", blockHandler = "assistantBlockHandle")
    public Flux<String> chat(@RequestParam(value = "message") String message,
                             @RequestParam(value = "sessionId") String sessionId,
                             @RequestParam(value = "msgId") String msgUid,
                             // 新增可选的经纬度参数
                             @RequestParam(value = "lat", required = false) Double lat,
                             @RequestParam(value = "lon", required = false) Double lon,
                             @RequestHeader String authorization) {

        Map<String, Object> stringObjectMap = verifyToken(authorization);
        Long userId = (Long) stringObjectMap.get("userId");
        if(!aiAssistantService.existConversation(sessionId)) {
            throw new BusException(CodeEnum.AI_SESSION_IS_EXIST_ERROR);
        }
        sensitiveTextUtil.checkUserInput(message);
        if (redissonLock.lock(sessionId,20000)) {
            try {
                // 如果有位置信息，拼接到消息中
                String finalMessage = message;
                if (lat != null && lon != null) {
                    finalMessage = String.format("(当前位置 经度:%f, 纬度:%f) %s", lon, lat, message);
                }
        /*TravelFaq travelFaq = faqService.searchBestAnswer(finalMessage);
        if(travelFaq != null) {
            // 找到匹配的 FAQ，直接返回答案
            String answer = travelFaq.getAnswer();
            // 发布事件以持久化消息
            publisher.publishEvent(new AiMessageEvent(this, sessionId, message, answer, msgUid,0));
            return Flux.just(answer);
        }*/
                PromptTemplate promptTemplate = new PromptTemplate(promptProperties.getChat());
                promptTemplate.render(
                        Map.of("user_id",userId,
                                "current_date",java.time.LocalDate.now().toString(),
                                "user_question",finalMessage));
                StringBuffer sb = new StringBuffer();
                long startTime = System.currentTimeMillis();
                AtomicInteger tokensUsed = new AtomicInteger(0);
                return chatClient.prompt(finalMessage)
                        //.system(promptProperties.getSystemPrompt())
                        .advisors( a -> a.param(CONVERSATION_ID, sessionId))
                        .advisors(TravelRagCustomAdvisorFactory.createTravelRagCustomAdvisor(vectorStore, documents -> {
                            List<String> ids = documents.stream()
                                    .map(Document::getId)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                            faqService.increaseUseCountBatch(ids);
                        }))
                        .stream().chatResponse()
                        .map(response -> {
                            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                                return "";
                            }
                            String text = response.getResult().getOutput().getText();
                            if (text != null) {
                                sb.append(text);
                            }
                            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                                Integer totalTokens = response.getMetadata().getUsage().getTotalTokens();
                                if (totalTokens != null) {
                                    tokensUsed.set(totalTokens);
                                }
                            }
                            return text == null ? "" : text;
                        })
                        .filter(chunk -> !chunk.isEmpty())
                        .doOnComplete(() -> {
                            // 2. 流结束时，发布事件
                            long processingTime = System.currentTimeMillis() - startTime;
                            publisher.publishEvent(new AiMessageEvent(this,sessionId,message,sb.toString(),msgUid,0,tokensUsed.get(),processingTime,userId));
                        })
                        .doOnError(e -> {
                            // 处理异常情况
                            throw new BusException(CodeEnum.SYSTEM_ERROR);
                        });
            } finally {
                redissonLock.unlock(sessionId);
            }
        }else {
            return null;
        }
    }

    public Map<String, Object> verifyToken(String authorization) {
        String token = authorization.replace("Bearer ","");
        return JWTUtil.verify(token);
    }

    /**
     * 获取对话列表
     * @param authorization jwt
     * @param page 页码
     * @param size 条数
     * @param contextType 绘画类型
     * @return
     */
    @GetMapping("/sessions")
    public BaseResult<IPage<AiConversations>> getSessions(@RequestHeader String authorization,
                                                          @RequestParam(value = "page",defaultValue = "1")Integer page,
                                                          @RequestParam(value = "size",defaultValue = "10")Integer size,
                                                          @RequestParam(value = "contextType")Integer contextType) {
        Map<String, Object> verify = verifyToken(authorization);
        Long userId = (Long) verify.get("userId");
        IPage<AiConversations> sessions = aiAssistantService.getSessions(page, size, userId, contextType);
        return BaseResult.success(sessions);
    }

    /**
     * 获取会话历史
     * @param authorization
     * @param sessionId
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/history")
    public BaseResult<IPage<AiMessages>> history(@RequestHeader String authorization,
                                                 @RequestParam String sessionId,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        verifyToken(authorization);
        IPage<AiMessages> history = aiAssistantService.getHistoryMessages(sessionId, page, size);
        return BaseResult.success(history);
    }

    /**
     * 重命名会话
     * @param sessionId
     * @param title
     * @param authorization
     * @return
     */
    //暴露自增 ID 给前端进行 UPDATE/DELETE 操作是不安全的实践,分库分表不适合使用自增id
    @PutMapping("updateSession")
    public BaseResult<?> update(@RequestParam(value = "sessionId")String sessionId,
                                @RequestParam(value = "title")String title,
                                @RequestHeader String authorization) {
        verifyToken(authorization);
        sensitiveTextUtil.checkUserInput(title);
        aiAssistantService.updateSession(sessionId, title);
        return BaseResult.success();
    }

    /**
     * 删除会话
     * @param sessionId
     * @param authorization
     * @return
     */
    @DeleteMapping("/deleteSession")
    public BaseResult<?> delete(@RequestParam(value = "conversationId")String sessionId,
                                @RequestHeader String authorization) {
        verifyToken(authorization);
        aiAssistantService.deleteSession(sessionId);
        return BaseResult.success();
    }

    /**
     * 删除消息
     * @param messageId
     * @param authorization
     * @return
     */
    @DeleteMapping("/deleteMessage")
    public BaseResult<?> deleteMessage(@RequestParam(value = "messageId")String messageId,
                                       @RequestHeader String authorization) {
        verifyToken(authorization);
        aiAssistantService.deleteMessage(messageId);
        return BaseResult.success();
    }

    @Autowired
    private TripAgent tripAgent;
    @GetMapping("/agent")
    @SentinelResource(value = "agentTranscription", blockHandler = "assistantBlockHandle")
    public SseEmitter agent(@RequestHeader String authorization,
                            @RequestParam(value = "conversationId")String sessionId,
                            @RequestParam(value = "message") String message) {
        Map<String, Object> map = verifyToken(authorization);
        if(!aiAssistantService.existConversation(sessionId)){
            throw new BusException(CodeEnum.AI_CHAT_STATUS_ERROR);
        }
        sensitiveTextUtil.checkUserInput(message);
        if(redissonLock.lock(sessionId,20000)) {
            try {
                return tripAgent.stream(sessionId, message,(Long) map.get("userId"));
            } finally {
                redissonLock.unlock(sessionId);
            }
        }else {
            return null;
        }
    }
}
