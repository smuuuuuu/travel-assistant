package com.itbaizhan.travel_trip_service.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travel_trip_service.config.RedisKeyProperties;
import com.itbaizhan.travel_trip_service.constant.TripConstant;
import com.itbaizhan.travel_trip_service.lock.LockMode;
import com.itbaizhan.travel_trip_service.lock.TripLocks;
import com.itbaizhan.travel_trip_service.mapper.*;
import com.itbaizhan.travel_trip_service.utils.BackupUtil;
import com.itbaizhan.travel_trip_service.utils.DedupKeyUtils;
import com.itbaizhan.travel_trip_service.utils.MdUtils;
import com.itbaizhan.travel_trip_service.utils.VerifyUtil;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanResponse;
import com.itbaizhan.travelcommon.info.BackupDetails;
import com.itbaizhan.travelcommon.info.BackupInfo;
import com.itbaizhan.travelcommon.pojo.*;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.TripsService;
import com.itbaizhan.travelcommon.vo.TripVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.itbaizhan.travel_trip_service.sse.TripSseEmitterRegistry;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* @author smuuuu
* @description 针对表【trips(行程计划表)】的数据库操作Service实现
* @createDate 2025-10-19 22:34:48
*/
@DubboService
@Service
@Slf4j
public class TripsServiceImpl implements TripsService {
    @Autowired
    private TripsMapper tripsMapper;
    @Autowired
    private TripSchedulesMapper tripSchedulesMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private TripTransportationMapper tripTransportationMapper;
    @Autowired
    private RedisKeyProperties redisKeyProperties;
    @Autowired
    private TripMdMapper tripMdMapper;
    @Autowired
    private MdUtils mdUtils;
    @Autowired
    private TripGaoDeMapper tripGaoDeMapper;
    @Autowired
    private TripDayItemMapper tripDayItemMapper;
    @Autowired
    private TripBackupMapper tripBackupMapper;
    @Autowired
    private TripSseEmitterRegistry emitterRegistry;
    @Autowired
    private VerifyUtil verifyUtil;

    private static final String ZSET_SCRIPT = """
        local zset = KEYS[1]
        local backupKey = ARGV[1]
        local noBackupKey = ARGV[2]
        local triggerTimes = ARGV[3]
        local isBackup = tonumber(ARGV[4])

        if isBackup == 1 then
          local score = redis.call('ZSCORE', zset, noBackupKey)
          if score then
            redis.call('ZREM', zset, noBackupKey)
          end
          redis.call('ZADD', zset, triggerTimes, backupKey)
          return 1
        else
          local score = redis.call('ZSCORE', zset, backupKey)
          if (not score) then
            redis.call('ZADD', zset, triggerTimes, noBackupKey)
            return 1
          end
          return 0
        end
    """;
    private static final String UPDATE_ZSET = """
            local zset = KEYS[1]
            local fromMember = ARGV[1]
            local toMember = ARGV[2]
            local score = redis.call('ZSCORE', zset, fromMember)
            if (not score) then
              return 0
            end
            redis.call('ZREM', zset, fromMember)
            redis.call('ZADD', zset, score, toMember)
            return 1
            """;
    /**
     * 订阅某个行程的“生成中”状态变化（基于 Redis ongoing hash）。
     *
     * @param tripId  行程ID
     * @param userId  用户ID
     * @param emitter SSE emitter
     * @param isMod  修改标志
     */
    private void streamOngoingStatus(String tripId, Long userId, SseEmitter emitter,Integer isMod){
        String id = tripId + ":" + isMod;
        emitterRegistry.register(userId, id, emitter);
        emitter.onCompletion(() -> emitterRegistry.remove(userId, id, emitter));
        emitter.onTimeout(() -> emitterRegistry.remove(userId, id, emitter));
        emitter.onError((ex) -> emitterRegistry.remove(userId, id, emitter));

        String ongoingKey = redisKeyProperties.buildOngoingPlanKey(userId);
        Object raw = redisTemplate.opsForHash().get(ongoingKey, id);
        if (raw == null) {
            sendSseEvent(emitter, "done", Map.of("status", "计划已结束", "tripId", tripId, "streamType", isMod));
            emitter.complete();
            return;
        }

        try {
            Map<String, Object> map = JSON.parseObject(String.valueOf(raw));
            Object s = map.get("status");
            String status = s == null ? null : String.valueOf(s);
            String type = (String) map.get("type");
            if(StringUtils.hasText(status) && StringUtils.hasText(type) && ("done".equals(type) || "error".equals(type))) {
                sendSseEvent(emitter, type, Map.of("status", status, "tripId", tripId, "streamType", isMod));
                emitter.complete();
                return;
            }
            if (StringUtils.hasText(status)) {
                sendSseEvent(emitter, type == null ? "progress": type, Map.of("status", status, "tripId", tripId, "streamType", isMod));
            }
        } catch (Exception ignored) {
        }
    }


    @Override
    public void generateAndModifyStream(String tripId, Long userId, SseEmitter emitter,boolean isModify) {
        String id = tripId == null ? "" : tripId.trim();
        if (!StringUtils.hasText(id)) {
            sendSseEvent(emitter, "error", Map.of("message", "tripId 不能为空"));
            emitter.complete();
            return;
        }
        if(isModify){
            streamOngoingStatus(tripId, userId, emitter,TripConstant.MODIFY_TRIP_ID);
        }else {
            streamOngoingStatus(tripId, userId, emitter,TripConstant.GENERATE_TRIP_ID);
        }
    }
    @Override
    public void generateMdStream(String tripId, Long userId, SseEmitter emitter) {
        String id = tripId == null ? "" : tripId.trim();
        if (!StringUtils.hasText(id)) {
            sendSseEvent(emitter, "error", Map.of("message", "tripId 不能为空"));
            emitter.complete();
            return;
        }
        streamOngoingStatus(tripId, userId, emitter,TripConstant.PROCESSING_MD_ID);
    }

    private void sendSseEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            emitter.complete();
        }
    }

    @Override
    public Page<TripVo> getTripsVoById(Integer page, Integer size, Long userId) {
        // 1. 查询数据库中的行程
        Page<TripVo> tripVoPage = tripsMapper.getTripVoByUserId(new Page<>(page, size), userId);
        
        // 2. 如果是第一页，尝试合并 Redis 中正在生成的行程
        if (page == 1) {
            String ongoingKey = redisKeyProperties.buildOngoingPlanKey(userId);
            Map<Object, Object> ongoingMap = redisTemplate.opsForHash().entries(ongoingKey);
            
            if (!ongoingMap.isEmpty()) {
                List<TripVo> generatingTrips = new ArrayList<>();
                List<TripVo> modifyTrips = new ArrayList<>();
                for (Object value : ongoingMap.values()) {
                    try {
                        String json = (String) value;
                        // 解析 JSON 构造临时 TripVo
                        // 假设 JSON 格式：{"tripId":"...", "destination":"...", "status":"...", ...}
                        // 这里使用 FastJSON2 解析
                        Map<String, Object> map = JSON.parseObject(json);
                        String type = (String) map.get("type");
                        if(type == null || "done".equals(type) || "error".equals(type)) {
                            continue;
                        }
                        Integer streamType = null;
                        Object st = map.get("streamType");
                        if (st != null) {
                            try {
                                streamType = Integer.valueOf(String.valueOf(st));
                            } catch (Exception ignored) {
                            }
                        }
                        TripVo vo = new TripVo();
                        vo.setTripId((String) map.get("tripId"));
                        vo.setDestination((String) map.get("destination"));
                        vo.setTotalDays(map.get("totalDays") != null ? Integer.parseInt(String.valueOf(map.get("totalDays"))) : 5);
                        vo.setTitle((String) map.get("status")); // 用状态描述作为标题，或者 "正在前往 " + destination

                        Object updatedAt = map.get("updatedAt");
                        if (updatedAt != null) {
                            String text = String.valueOf(updatedAt);
                            if (StringUtils.hasText(text)) {
                                try {
                                    vo.setCreatedAt(LocalDateTime.parse(text));
                                } catch (Exception ignored) {
                                    try {
                                        vo.setCreatedAt(LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                                    } catch (Exception ignored2) {
                                    }
                                }
                            }
                        }
                        if (streamType != null && streamType.equals(TripConstant.MODIFY_TRIP_ID)) {
                            vo.setStatus(TripConstant.MODIFY_TRIP_ID);
                            modifyTrips.add(vo);
                        }else if(streamType != null && streamType.equals(TripConstant.GENERATE_TRIP_ID)) {
                            vo.setStatus(TripConstant.GENERATE_TRIP_ID);
                            generatingTrips.add(vo);
                        }
                    } catch (Exception e) {
                        log.error("Error parsing ongoing trip json", e);
                    }
                }
                
                // 将生成中的行程插到列表最前面
                if (!generatingTrips.isEmpty() || !modifyTrips.isEmpty()) {
                    modifyTrips.addAll(generatingTrips);
                    List<TripVo> originalRecords = tripVoPage.getRecords();
                    if (originalRecords != null && !originalRecords.isEmpty()) {
                        java.util.Set<String> generatingIds = new java.util.HashSet<>();
                        for (TripVo vo : modifyTrips) {
                            if (vo == null || vo.getTripId() == null) continue;
                            generatingIds.add(vo.getTripId());
                        }
                        originalRecords = originalRecords.stream()
                                .filter(vo -> vo == null || vo.getTripId() == null || !generatingIds.contains(vo.getTripId()))
                                .toList();
                        Collections.reverse(modifyTrips);
                        modifyTrips.addAll(originalRecords);
                    }else {
                        Collections.reverse(modifyTrips);
                    }
                    tripVoPage.setRecords(modifyTrips);
                }
            }
        }
        
        return tripVoPage;
    }

    @Override
    public TravelPlanResponse getTripById(String tripId,Long userId) {
        String key = redisKeyProperties.buildPlanKey(userId,tripId);
        Object o = redisTemplate.opsForValue().get(key);
        if (verifyUtil.verifyObject(o)){
            throw new BusException(CodeEnum.TRIP_NOT_FOUND);
        }
        if(o instanceof TravelPlanResponse cached) {
            if (cached.getTravelStyle() != null) {
                return cached;
            }
            redisTemplate.delete(key);
        }
        QueryWrapper<Trips> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id",tripId);
        Trips trips = tripsMapper.selectOne(queryWrapper);
        if(Objects.isNull(trips)){
            redisTemplate.opsForValue().set(key,"null",randomTime(),TimeUnit.MINUTES);
            throw new BusException(CodeEnum.TRIP_NOT_FOUND);
        }
        if (trips.getTravelStyle() == null) {
            Object raw = tripsMapper.selectObjs(new QueryWrapper<Trips>()
                    .select("travel_style")
                    .eq("trip_id", tripId))
                .stream()
                .findFirst()
                .orElse(null);
            String json = null;
            if (raw instanceof String s) {
                json = s;
            } else if (raw instanceof byte[] bytes) {
                json = new String(bytes, StandardCharsets.UTF_8);
            } else if (raw != null) {
                json = String.valueOf(raw);
            }
            if (StringUtils.hasText(json)) {
                try {
                    trips.setTravelStyle(JSON.parseArray(json, String.class));
                } catch (Exception ignored) {
                    trips.setTravelStyle(null);
                }
            }
        }
        TravelPlanResponse travelPlanResponse = new TravelPlanResponse();
        BeanUtils.copyProperties(trips,travelPlanResponse);
        travelPlanResponse.setBudget(trips.getBudgetTotal());
        QueryWrapper<TripSchedules> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("trip_id",tripId);
        List<TripSchedules> tripSchedules = tripSchedulesMapper.selectList(queryWrapper1);
        if(Objects.isNull(tripSchedules) || tripSchedules.isEmpty()){
            throw new BusException(CodeEnum.TRIP_SCHEDULES_NOT_FOUND);
        }

        List<TravelPlanResponse.DayPlan> dayPlans = tripSchedules.stream().map(tripSchedule -> {
            TravelPlanResponse.DayPlan dayPlan = new TravelPlanResponse.DayPlan();
            BeanUtils.copyProperties(tripSchedule,dayPlan);
            dayPlan.setDate(tripSchedule.getDayNumber());
            dayPlan.setDateTime(tripSchedule.getDate());
            dayPlan.setItems(new ArrayList<>());
            return dayPlan;
        }).toList();
        List<String> transKeys = new ArrayList<>();
        List<String> poiKeys = new ArrayList<>();
        QueryWrapper<TripDayItem> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("trip_id",tripId);
        List<TripDayItem> tripDayItems = tripDayItemMapper.selectList(queryWrapper2);
        if(tripDayItems != null && !tripDayItems.isEmpty()){
            tripDayItems.forEach(tripDayItem -> {
                for (TravelPlanResponse.DayPlan dayPlan : dayPlans) {
                    if(dayPlan.getId().equals(tripDayItem.getScheduleId())){
                        dayPlan.getItems().add(tripDayItem);
                    }
                }
                if(TripConstant.TRANS.equals(tripDayItem.getItemType())){
                    String ref = tripDayItem.getRefId();
                    if (StringUtils.hasText(ref)) {
                        transKeys.add(ref);
                    }
                } else if (TripConstant.POI.equals(tripDayItem.getItemType())) {
                    String ref = tripDayItem.getRefId();
                    if (StringUtils.hasText(ref)) {
                        poiKeys.add(ref);
                    }
                }
            });
        }
        travelPlanResponse.setDays(dayPlans);
        //List<TripTransportation> tripTransportations = new ArrayList<>();
        if (!transKeys.isEmpty()) {
            QueryWrapper<TripTransportation> wrapperTrans = new QueryWrapper<>();
            wrapperTrans.in("dedupKey", transKeys);
            travelPlanResponse.setTransportations(tripTransportationMapper.selectList(wrapperTrans));
        }

        if (!poiKeys.isEmpty()) {
            QueryWrapper<TripGaoDe> wrapperPoi = new QueryWrapper<>();
            wrapperPoi.in("dedupKey", poiKeys);
            travelPlanResponse.setGaoDes(tripGaoDeMapper.selectList(wrapperPoi));
        }
        travelPlanResponse.setIsSave(TripConstant.SAVE);
        redisTemplate.opsForValue().set(redisKeyProperties.buildPlanKey(userId, tripId),travelPlanResponse,randomTime(),TimeUnit.MINUTES);
        return travelPlanResponse;
    }

    @Override
    @Transactional
    public BackupInfo getBackup(String tripId, Long userId) {
        String key = redisKeyProperties.buildBackupPlanKey(userId, tripId);
        Object o = redisTemplate.opsForValue().get(key);
        if(verifyUtil.verifyObject(o)){
            throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
        }
        LocalDateTime expire = null;
        TravelPlanResponse response = null;
        BackupInfo backupInfo = new BackupInfo();
        if(o != null) {
            BackupDetails backupDetails = (BackupDetails) o;
            if(LocalDateTime.now().isAfter(backupDetails.getExpireTime())){
                this.deleteMysqlBack(tripId,userId);
                throw new BusException(CodeEnum.TRIP_BACKUP_EXPIRE_ERROR);
            }
            expire = backupDetails.getExpireTime();
            response = backupDetails.getTravelPlanResponse();
            backupInfo.setObjectKey(backupDetails.getObjectKey());
        }else {
            //return getResponseBackup(tripId, userId, true).backupInfo;
            QueryWrapper<TripBackup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("trip_id",tripId);
            queryWrapper.eq("user_id", userId);
            TripBackup tripBackup = tripBackupMapper.selectOne(queryWrapper);
            if(Objects.isNull(tripBackup)){
                redisTemplate.opsForValue().set(key, "null",randomTime(),TimeUnit.MINUTES);
                throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
            }
            if(LocalDateTime.now().isAfter(tripBackup.getExpireAt())){
                this.deleteMysqlBack(tripId,userId);
                throw new BusException(CodeEnum.TRIP_BACKUP_EXPIRE_ERROR);
            }
            expire = tripBackup.getExpireAt();
            String json = null;
            try {
                json = BackupUtil.getTravelPlanResponseJson(tripBackup.getContent(),tripBackup.getContentFormat());
            } catch (IOException e) {
                throw new BusException(CodeEnum.SYSTEM_ERROR);
            }
            backupInfo.setObjectKey(tripBackup.getObjectKey());
            response = JSON.parseObject(json, TravelPlanResponse.class);
            BackupDetails backupDetails = new BackupDetails();
            backupDetails.setExpireTime(expire);
            backupDetails.setTravelPlanResponse(response);
            backupDetails.setObjectKey(tripBackup.getObjectKey());
            redisTemplate.opsForValue().set(key,backupDetails,randomTime(),TimeUnit.MINUTES);
        }

        Duration duration = Duration.between(LocalDateTime.now(), expire);
        long daysPart = duration.toDaysPart();
        int hoursPart = duration.toHoursPart();
        int minutesPart = duration.toMinutesPart();

        String time = "%s天%s小时%s分钟".formatted(String.valueOf(daysPart),
                String.valueOf(hoursPart), String.valueOf(minutesPart));
        backupInfo.setTravelPlanResponse(response);
        backupInfo.setExpire(time);
        return backupInfo;
    }

    private void updateZsetBackup(String tripId,Long userId){
        String zKey = buildSyncQueueMember(userId, tripId, TripConstant.BACKUP);
        String s = buildSyncQueueMember(userId, tripId, TripConstant.NO_BACKUP);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(UPDATE_ZSET);
        redisTemplate.execute(script, List.of(redisKeyProperties.getSyncQueue()), zKey, s);
    }
    @Override
    @Transactional
    @TripLocks(user = LockMode.READ,backup = LockMode.WRITE, userIdIndex = 1, tripIdIndex = 0, waitMillis = 3000, leaseMillis = 30000)
    public void restoreBackup(String tripId, Long userId) {
        String syncQueueMember = this.buildSyncQueueMember(userId, tripId, TripConstant.BACKUP);
        Double score = redisTemplate.opsForZSet().score(redisKeyProperties.getSyncQueue(), syncQueueMember);
        updateZsetBackup(tripId,userId);
        handleBackup(tripId,userId,score,true);
    }
    @Transactional
    protected void delBackup(TravelPlanResponse response) {
        //if(tripBackupMapper.exists(queryWrapper)){
           // BackupDetails backupDetail = this.handleBackupDetails(tripId, userId);
            List<TripDayItem> list = new ArrayList<>();
            for (TravelPlanResponse.DayPlan day : response.getDays()) {
                list.addAll(day.getItems());
            }
            clearMyRedis(list);
        //}
    }
    @Transactional
    protected void handleBackup(String tripId,Long userId,Double score,boolean isRestore){
        String backupPlanKey = redisKeyProperties.buildBackupPlanKey(userId, tripId);
        Object object = redisTemplate.opsForValue().get(backupPlanKey);
        if(verifyUtil.verifyObject(object)){
            throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
        }
        BackupDetails backupDetails = (object == null ? null : (BackupDetails) object);
        TravelPlanResponse response = null;
        QueryWrapper<TripBackup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id",tripId);
        queryWrapper.eq("user_id",userId);
        if(score != null) {
            if (backupDetails != null) {
                response = backupDetails.getTravelPlanResponse();
                if(tripBackupMapper.exists(queryWrapper)) {
                    TripBackup tripBackup = tripBackupMapper.selectOne(queryWrapper);
                    String json = null;
                    try {
                        json = BackupUtil.getTravelPlanResponseJson(tripBackup.getContent(),tripBackup.getContentFormat());
                    } catch (IOException e) {
                        throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
                    }
                    TravelPlanResponse travelPlanResponse = JSON.parseObject(json, TravelPlanResponse.class);
                    if(TripConstant.SAVE.equals(travelPlanResponse.getIsSave())){
                        delBackup(travelPlanResponse);
                    }
                    if(isRestore){
                        if(tripBackup.getObjectKey() != null){
                            this.deleteMd(List.of(tripBackup.getObjectKey()));
                        }
                    }
                }
            }else {
                throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
            }
        }else {
            if(backupDetails == null) {
                backupDetails = handleBackupDetails(tripId,userId);
            }
            response = backupDetails.getTravelPlanResponse();
        }
        String tripId1 = tripId;
        if(isRestore){
            tripId1 = UUID.randomUUID().toString();
            response.setTripId(tripId1);
        }
        if(TripConstant.Complete_trip_ID.equals(response.getCompleteStatus())){
            String objectKey = backupDetails.getObjectKey();

            if(isRestore){
                String replace = objectKey.replace(tripId, tripId1);
                mdUtils.move(objectKey,replace);
                this.saveMdUrl(tripId1,userId,replace);
            }else {
                this.saveMdUrl(tripId1,userId,objectKey);
            }
            String mdKey = redisKeyProperties.buildBackupMdKey(userId, tripId);
            redisTemplate.delete(mdKey);
        }
        String key = redisKeyProperties.buildPlanKey(userId, tripId1);
        redisTemplate.opsForValue().set(key,response,randomTime(),TimeUnit.MINUTES);
        if(TripConstant.SAVE.equals(response.getIsSave()) && !isRestore && score != null){
            redisTemplate.delete(backupPlanKey);
            tripBackupMapper.delete(queryWrapper);
            return;
        }
        if(TripConstant.SAVE.equals(response.getIsSave()) && (!isRestore || score == null) ){
            handleBack(response,userId,isRestore);
            if(!isRestore) {
                if (backupDetails.getObjectKey() != null) {
                    mdUtils.delAllObjectsByFolder(userId + "/" + tripId, Set.of(backupDetails.getObjectKey()));
                } else {
                    mdUtils.delAllObjectsByFolder(userId + "/" + tripId, null);
                    QueryWrapper<TripMd> queryWrapper1 = new QueryWrapper<>();
                    queryWrapper1.eq("trip_id",tripId);
                    queryWrapper1.eq("user_id",userId);
                    tripMdMapper.delete(queryWrapper1);
                }
            }
        }else {
            if(isRestore){
                Trips trips = new Trips();
                trips.setTripId(tripId1);
                trips.setUserId(userId);
                trips.setTitle(response.getTitle());
                trips.setCreatedAt(LocalDateTime.now());
                trips.setTotalDays(response.getTotalDays());
                trips.setDestination(response.getDestination());
                trips.setCompleteStatus(TripConstant.DRAFT_TRIP_ID);
                tripsMapper.insert(trips);
            }
            response.setIsSave(TripConstant.NO_SAVE);
            updateTrip(tripId1,userId,TripConstant.NO_BACKUP);
        }
        if(!(isRestore && score == null)){
            String mdKey = redisKeyProperties.buildMdKey(userId, tripId);
            redisTemplate.delete(mdKey);
        }
        redisTemplate.delete(backupPlanKey);
        tripBackupMapper.delete(queryWrapper);
    }
    @Transactional
    protected void handleBack(TravelPlanResponse response,Long userId,boolean isRestore){
        if(!isRestore){
            deleteMysqlTrip(response.getTripId(),true);
        }
        List<TripTransportation> transportations = response.getTransportations();
        List<TripGaoDe> gaoDes = response.getGaoDes();
        response.setTransportations(null);
        response.setGaoDes(null);
        insertTrip(response,userId);
        response.setTransportations(transportations);
        response.setGaoDes(gaoDes);
        //redisTemplate.opsForValue().set(key,response,7, TimeUnit.DAYS);
    }
    private BackupDetails handleBackupDetails(String tripId,Long userId){
        QueryWrapper<TripBackup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id",tripId);
        queryWrapper.eq("userId",userId);
        TripBackup tripBackup = tripBackupMapper.selectOne(queryWrapper);
        if(tripBackup == null){
            redisTemplate.opsForValue().set(redisKeyProperties.buildBackupPlanKey(userId,tripId), "null",randomTime(),TimeUnit.MINUTES);
            throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
        }
        //tripBackupMapper.delete(queryWrapper);
        String json = null;
        try {
            json = BackupUtil.getTravelPlanResponseJson(tripBackup.getContent(),tripBackup.getContentFormat());
        } catch (IOException e) {
            throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
        }
        TravelPlanResponse travelPlanResponse = JSON.parseObject(json, TravelPlanResponse.class);
        BackupDetails backupDetails = new BackupDetails();
        backupDetails.setObjectKey(tripBackup.getObjectKey());
        backupDetails.setTravelPlanResponse(travelPlanResponse);
        return backupDetails;
    }
    @Override
    @Transactional
    @TripLocks(user = LockMode.READ, trip = LockMode.WRITE,backup = LockMode.WRITE,userIdIndex = 1, tripIdIndex = 0, waitMillis = 3000, leaseMillis = 30000)
    public void backupCover(String tripId, Long userId){
        String s = buildSyncQueueMember(userId, tripId, TripConstant.BACKUP);
        Double score = redisTemplate.opsForZSet().score(redisKeyProperties.getSyncQueue(), s);
        this.removeSyncQueueMembersForTrip(userId,tripId);
        handleBackup(tripId,userId,score,false);
    }
    @Transactional
    protected void deleteMysqlBack(String tripId, Long userId){
        QueryWrapper<TripBackup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id",tripId);
        queryWrapper.eq("user_id", userId);
        tripBackupMapper.delete(queryWrapper);
    }
    @Override
    @Transactional
    @TripLocks(user = LockMode.READ, backup = LockMode.WRITE, userIdIndex = 1, tripIdIndex = 0, waitMillis = 3000, leaseMillis = 30000)
    public void deleteBackup(String tripId, Long userId){
        String s = buildSyncQueueMember(userId, tripId, TripConstant.BACKUP);
        Double score = redisTemplate.opsForZSet().score(redisKeyProperties.getSyncQueue(), s);
        this.updateZsetBackup(tripId,userId);
        String key = redisKeyProperties.buildBackupPlanKey(userId, tripId);
        Object object = redisTemplate.opsForValue().get(key);
        BackupDetails backupDetails = (object != null ? (BackupDetails) object : null);

        redisTemplate.delete(key);
        QueryWrapper<TripBackup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id",tripId);
        queryWrapper.eq("user_id", userId);
        if(!tripBackupMapper.exists(queryWrapper)){
            return;
        }
        TripBackup tripBackup = tripBackupMapper.selectOne(queryWrapper);
        if(tripBackup.getObjectKey() != null){
            this.deleteMd(List.of(tripBackup.getObjectKey()));
        }
        TravelPlanResponse travelPlanResponse = null;
        if(score == null){
            if(backupDetails == null){
                String json = null;
                try {
                    json = BackupUtil.getTravelPlanResponseJson(tripBackup.getContent(),tripBackup.getContentFormat());
                } catch (IOException e) {
                    throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
                }
                travelPlanResponse = JSON.parseObject(json, TravelPlanResponse.class);
            }else {
                travelPlanResponse = backupDetails.getTravelPlanResponse();
            }
        }else {
            String json = null;
            try {
                json = BackupUtil.getTravelPlanResponseJson(tripBackup.getContent(),tripBackup.getContentFormat());
            } catch (IOException e) {
                throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
            }
            travelPlanResponse = JSON.parseObject(json, TravelPlanResponse.class);
        }
        if(TripConstant.SAVE.equals(travelPlanResponse.getIsSave())){
            this.delBackup(travelPlanResponse);
        }
        String mdKey = redisKeyProperties.buildBackupMdKey(userId, tripId);
        redisTemplate.delete(mdKey);
        tripBackupMapper.delete(queryWrapper);
    }
    @Override
    @TripLocks(user = LockMode.READ, trip = LockMode.READ,backup = LockMode.READ, userIdIndex = 1, tripIdIndex = 0, waitMillis = 0, leaseMillis = 15000)
    public void insertRedisTravel(TravelPlanResponse travelPlanResponse, Long userId,Integer isBackup) {
        /*Integer isPresentTrip = tripsMapper.getIsPresentTrip(travelPlanResponse.getTripId(), userId);
        if(isPresentTrip == null || isPresentTrip == 0){
            throw new BusException(CodeEnum.TRIP_NOT_FOUND);
        }*/
        checkTravelPlanResponse(travelPlanResponse);
        travelPlanResponse.setCompleteStatus(TripConstant.DRAFT_TRIP_ID);
        travelPlanResponse.setIsSave(TripConstant.NO_SAVE);
        String key = redisKeyProperties.buildPlanKey(userId, travelPlanResponse.getTripId());
        List<String> mdUrl = new ArrayList<>();
        if(TripConstant.BACKUP.equals(isBackup)){
            TravelPlanResponse response =  getTripById(travelPlanResponse.getTripId(),userId);

            if(response != null){
                if(response.getCompleteStatus().equals(TripConstant.MODIFY_TRIP_ID)){
                    response.setCompleteStatus(TripConstant.DRAFT_TRIP_ID);
                }
                BackupDetails backupInfo = new BackupDetails();
                if(response.getCompleteStatus().equals(TripConstant.Complete_trip_ID)){
                    backupInfo.setObjectKey(this.getObjectKey(response.getTripId(),userId));
                }
                backupInfo.setExpireTime(LocalDateTime.now().plusDays(7));
                backupInfo.setTravelPlanResponse(response);
                redisTemplate.opsForValue().set(redisKeyProperties.buildBackupPlanKey(userId, travelPlanResponse.getTripId()), backupInfo,randomTime(), TimeUnit.MINUTES);
                //redisTemplate.opsForValue().set(key,travelPlanResponse,7, TimeUnit.DAYS);
                updateTrip(response.getTripId(),userId,TripConstant.BACKUP);
                mdUrl.add(redisKeyProperties.buildBackupMdKey(userId,travelPlanResponse.getTripId()));
                /*redisTemplate.delete(mdUrl);
                return;*/
            }
        }else {
            updateTrip(travelPlanResponse.getTripId(),userId,TripConstant.NO_BACKUP);
        }
        mdUrl.add(redisKeyProperties.buildMdKey(userId, travelPlanResponse.getTripId()));
        redisTemplate.delete(mdUrl);

        redisTemplate.opsForValue().set(key,travelPlanResponse,randomTime(), TimeUnit.MINUTES);

    }
    @Override
    @Transactional
    @TripLocks(user = LockMode.READ, backup = LockMode.READ, userIdIndex = 1, tripIdIndex = 0, waitMillis = 0, leaseMillis = 3000)
    public void updateMysqlBackup(String tripId, Long userId){
        Object object = redisTemplate.opsForValue().get(redisKeyProperties.buildBackupPlanKey(userId, tripId));
        if(object == null){
            return;
        }
        if(verifyUtil.verifyObject(object)){
            throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
        }
        BackupDetails backupDetails = (BackupDetails) object;
        String jsonString = JSON.toJSONString(backupDetails.getTravelPlanResponse());
        byte[] bytes = null;
        try {
            bytes = BackupUtil.gzipJson(jsonString);
        } catch (IOException e) {
            throw new BusException(CodeEnum.TRIP_BACKUP_ERROR);
        }
        QueryWrapper<TripBackup> backupQueryWrapper = new QueryWrapper<>();
        backupQueryWrapper.eq("trip_id", tripId);
        backupQueryWrapper.eq("user_id", userId);
        TripBackup tripBackup = tripBackupMapper.selectOne(backupQueryWrapper);
        if (tripBackup == null) {
            TripBackup inserted = new TripBackup();
            inserted.setTripId(tripId);
            inserted.setUserId(userId);
            inserted.setContent(bytes);
            inserted.setContentFormat(2);
            inserted.setObjectKey(backupDetails.getObjectKey());
            inserted.setCreatedAt(LocalDateTime.now());
            inserted.setExpireAt(backupDetails.getExpireTime());
            tripBackupMapper.insert(inserted);
        }else {
            String json = null;
            try {
                json = BackupUtil.getTravelPlanResponseJson(tripBackup.getContent(),tripBackup.getContentFormat());
            } catch (IOException e) {
                throw new BusException(CodeEnum.TRIP_BACKUP_NOT_ERROR);
            }
            TravelPlanResponse travelPlanResponse = JSON.parseObject(json, TravelPlanResponse.class);
            tripBackup.setContent(bytes);
            tripBackup.setContentFormat(2);
            tripBackup.setCreatedAt(LocalDateTime.now());
            tripBackup.setObjectKey(backupDetails.getObjectKey());
            tripBackup.setExpireAt(backupDetails.getExpireTime());
            tripBackupMapper.updateById(tripBackup);

            if(TripConstant.SAVE.equals(travelPlanResponse.getIsSave())) {
                delBackup(travelPlanResponse);
            }
        }
    }

    @Override
    //@TripLocks(user = LockMode.READ, trip = LockMode.READ, userIdIndex = 1, tripIdIndex = 0, waitMillis = 0, leaseMillis = 15000)
    public void updateTrip(String tripId, Long userId,Integer isBackup) {
        // 2. 将同步任务加入延迟队列 (防抖核心)
        // Score 设置为 60 秒后的时间戳

        Long triggerTimes = System.currentTimeMillis() + 60 * 1000 * 5;
        String syncQueue = redisKeyProperties.getSyncQueue();
        String backupKey = buildSyncQueueMember(userId, tripId, TripConstant.BACKUP);
        String noBackupKey = buildSyncQueueMember(userId, tripId, TripConstant.NO_BACKUP);
        redisTemplate.execute(new DefaultRedisScript<>(ZSET_SCRIPT, Long.class),
                List.of(syncQueue),
                backupKey,
                noBackupKey,
                triggerTimes,
                isBackup);
    }
    @Override
    public void updateCompleteStatus(String tripId, Long userId,Integer status){
        String key = redisKeyProperties.buildPlanKey(userId, tripId);
        Object object = redisTemplate.opsForValue().get(key);
        if(verifyUtil.verifyObject(object)){
            throw new BusException(CodeEnum.TRIP_NOT_FOUND);
        }
        if(object != null){
            TravelPlanResponse response = (TravelPlanResponse) object;
            response.setCompleteStatus(status);
            redisTemplate.opsForValue().set(key,response,7, TimeUnit.DAYS);
        }
        String syncQueueMember = buildSyncQueueMember(userId, tripId, TripConstant.NO_BACKUP);
        Double score = redisTemplate.opsForZSet().score(redisKeyProperties.getSyncQueue(), syncQueueMember);
        if(score != null){
            return;
        } else if (redisTemplate.opsForZSet().score(redisKeyProperties.getSyncQueue()
                , buildSyncQueueMember(userId, tripId, TripConstant.BACKUP)) != null) {
            return;
        }
        QueryWrapper<Trips> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id",tripId);
        Trips trips = tripsMapper.selectOne(queryWrapper);
        if(Objects.isNull(trips)){
            updateTrip(tripId,userId,TripConstant.NO_BACKUP);
        }else {
            trips.setCreatedAt(LocalDateTime.now());
            trips.setCompleteStatus(status);
            tripsMapper.updateById(trips);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @TripLocks(user = LockMode.READ, trip = LockMode.WRITE, backup = LockMode.WRITE, userIdIndex = 1, tripIdIndex = 0, waitMillis = 3000, leaseMillis = 30000)
    public void deleteTrip(String tripId,Long userId) {

        String key = redisKeyProperties.buildPlanKey(userId, tripId);
        String backupPlanKey = redisKeyProperties.buildBackupPlanKey(userId, tripId);
        String mdKey = redisKeyProperties.buildMdKey(userId, tripId);
        //stringRedisTemplate.delete(List.of(key, backupPlanKey, mdKey));
        QueryWrapper<TripBackup> backupQueryWrapper = new QueryWrapper<>();
        backupQueryWrapper.eq("trip_id", tripId);
        backupQueryWrapper.eq("user_id", userId);
        if(tripBackupMapper.exists(backupQueryWrapper)){
            this.deleteBackup(tripId,userId);
            tripBackupMapper.delete(backupQueryWrapper);
        }
        deleteMysqlTrip(tripId, true);
        QueryWrapper<TripMd> wrapper4 = new QueryWrapper<>();
        wrapper4.eq("trip_id", tripId);
        if(tripMdMapper.exists(wrapper4)){
            tripMdMapper.delete(wrapper4);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String prefix = userId + "/" + tripId;
                mdUtils.delAllObjectsByFolder(prefix,null);
                stringRedisTemplate.delete(List.of(key, backupPlanKey, mdKey));
                removeOngoingStatus(userId, tripId);
                removeSyncQueueMembersForTrip(userId, tripId);
            }
        });
    }
    @Transactional
    protected void deleteMysqlTrip(String tripId,boolean isDelRef) {
        QueryWrapper<Trips> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id",tripId);
        int delete = tripsMapper.delete(queryWrapper);
        if(delete == 0){
            return;
        }
        QueryWrapper<TripSchedules> wrapper = new QueryWrapper<>();
        wrapper.eq("trip_id", tripId);
        int delete1 = tripSchedulesMapper.delete(wrapper);
        if(delete1 == 0){
            return;
        }
        QueryWrapper<TripDayItem> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("trip_id", tripId);
        List<TripDayItem> tripDayItems = tripDayItemMapper.selectList(wrapper1);
        tripDayItemMapper.delete(wrapper1);
        if(isDelRef){
            if(tripDayItems != null && !tripDayItems.isEmpty()){
                clearMyRedis(tripDayItems);
            }
        }
        /*QueryWrapper<TripMd> wrapper4 = new QueryWrapper<>();
        wrapper4.eq("trip_id", tripId);
        tripMdMapper.delete(wrapper4);*/
    }

    /**
     * 释放 TripDayItem 引用的全局 POI/交通资源：引用计数减一，归零则清理映射并删除DB记录。
     * @param tripDayItems 行程内的 day item 列表
     */
    @Transactional
    protected void clearMyRedis(List<TripDayItem> tripDayItems){
        Map<String, Integer> transDecrements = new HashMap<>();
        Map<String, Integer> poiDecrements = new HashMap<>();
        for (TripDayItem tripDayItem : tripDayItems == null ? List.<TripDayItem>of() : tripDayItems) {
            String refStr = tripDayItem.getRefId();
            if (!StringUtils.hasText(refStr)) {
                continue;
            }
            if (TripConstant.TRANS.equals(tripDayItem.getItemType())) {
                transDecrements.merge(refStr, 1, Integer::sum);
            } else if (TripConstant.POI.equals(tripDayItem.getItemType())) {
                poiDecrements.merge(refStr, 1, Integer::sum);
            }
        }

        if (!transDecrements.isEmpty()) {
            for (Map.Entry<String, Integer> entry : transDecrements.entrySet()) {
                String dedupKey = entry.getKey();
                Integer delta = entry.getValue();
                if (!StringUtils.hasText(dedupKey) || delta == null || delta <= 0) {
                    continue;
                }
                UpdateWrapper<TripTransportation> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("dedupKey", dedupKey)
                        .setSql("ref_count = GREATEST(IFNULL(ref_count, 0) - " + delta + ", 0)");
                tripTransportationMapper.update(null, updateWrapper);
            }

            QueryWrapper<TripTransportation> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.in("dedupKey", transDecrements.keySet()).le("ref_count", 0);
            tripTransportationMapper.delete(deleteWrapper);
        }
        if (!poiDecrements.isEmpty()) {
            for (Map.Entry<String, Integer> entry : poiDecrements.entrySet()) {
                String dedupKey = entry.getKey();
                Integer delta = entry.getValue();
                if (!StringUtils.hasText(dedupKey) || delta == null || delta <= 0) {
                    continue;
                }
                UpdateWrapper<TripGaoDe> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("dedupKey", dedupKey)
                        .setSql("ref_count = GREATEST(IFNULL(ref_count, 0) - " + delta + ", 0)");
                tripGaoDeMapper.update(null, updateWrapper);
            }

            QueryWrapper<TripGaoDe> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.in("dedupKey", poiDecrements.keySet()).le("ref_count", 0);
            tripGaoDeMapper.delete(deleteWrapper);
        }
    }
    private String getObjectKey(String tripId,Long userId){
        QueryWrapper<TripMd> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("trip_id", tripId);
        queryWrapper.eq("user_id", userId);
        TripMd tripMd = tripMdMapper.selectOne(queryWrapper);
        return tripMd.getObjectKey();
    }
    @Override
    @Transactional
    @TripLocks(user = LockMode.WRITE, userIdIndex = 0, waitMillis = 3000, leaseMillis = 60000)
    public void clear(Long userId) {
        QueryWrapper<Trips> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        tripsMapper.delete(wrapper);
        QueryWrapper<TripSchedules> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("user_id", userId);
        tripSchedulesMapper.delete(wrapper1);
        QueryWrapper<TripDayItem> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("user_id", userId);
        List<TripDayItem> tripDayItems = tripDayItemMapper.selectList(wrapper2);
        if(tripDayItems != null && !tripDayItems.isEmpty()){
            clearMyRedis(tripDayItems);
        }
        tripDayItemMapper.delete(wrapper2);
        QueryWrapper<TripMd> wrapper5 = new QueryWrapper<>();
        wrapper5.eq("user_id", userId);
        tripMdMapper.delete(wrapper5);
        QueryWrapper<TripBackup> wrapper6 = new QueryWrapper<>();
        wrapper6.eq("user_id", userId);
        tripBackupMapper.delete(wrapper6);
        // 2. 注册事务同步回调，确保事务提交后再删 Redis
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mdUtils.delAllObjectsByFolder(String.valueOf(userId),null);
                // 只有当数据库事务成功提交后，才会执行这里的代码
                removeSyncQueueMembersForUser(userId);
                removeAllOngoingStatus(userId);
                String pattern = redisKeyProperties.getPlan() + userId + ":*";
                delRedisCollection(pattern);

                String mdPattern = redisKeyProperties.getMd() + userId + ":*";
                delRedisCollection(mdPattern);
                //deleteMembersByPatternWithScan(String.valueOf(userId));
            }
        });
    }

    private void delRedisCollection(String mdPattern) {
        ScanOptions mdOptions = ScanOptions.scanOptions().match(mdPattern).count(100).build();
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(mdOptions)) {
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    connection.del(keyBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }
    /*private void deleteMembersByPatternWithScan(String pattern){
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build();
        List<String> toDelete = new ArrayList<>();
        try(Cursor<ZSetOperations.TypedTuple<Object>> cursor =
                    redisTemplate.opsForZSet().scan(redisKeyProperties.getSyncQueue(),scanOptions)){
            while (cursor.hasNext()) {
                ZSetOperations.TypedTuple<Object> next = cursor.next();
                Object value = next.getValue();
                toDelete.add(String.valueOf(value));

                // 可选：当收集到一定数量时提前删除，避免列表过大
                if (toDelete.size() >= 100) {
                    redisTemplate.opsForZSet().remove(redisKeyProperties.getSyncQueue(),
                            (Object) toDelete.toArray(new String[0]));
                    toDelete.clear();
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        // 删除剩余的成员
        if (!toDelete.isEmpty()) {
            redisTemplate.opsForZSet().remove(redisKeyProperties.getSyncQueue(), (Object) toDelete.toArray(new String[0]));
        }
    }*/

    /**
     * 删除某个行程在 ongoing hash 中的状态，避免删除后仍显示“生成中/修改中”。
     *
     * @param userId 用户ID
     * @param tripId 行程ID
     */
    private void removeOngoingStatus(Long userId, String tripId) {
        if (userId == null || !StringUtils.hasText(tripId)) return;
        String ongoingKey = redisKeyProperties.buildOngoingPlanKey(userId);
        redisTemplate.opsForHash().delete(ongoingKey,
                tripId + ":" + TripConstant.GENERATE_TRIP_ID,
                tripId + ":" + TripConstant.MODIFY_TRIP_ID);
    }

    /**
     * 删除某个用户的 ongoing hash，避免清空后仍显示“生成中/修改中”。
     *
     * @param userId 用户ID
     */
    private void removeAllOngoingStatus(Long userId) {
        if (userId == null) return;
        String ongoingKey = redisKeyProperties.buildOngoingPlanKey(userId);
        redisTemplate.delete(ongoingKey);
    }

    /**
     * 构造延迟队列 member（用于 ZSet 去重/删除）。
     *
     * @param userId   用户ID
     * @param tripId   行程ID
     * @param isBackup 是否备份(0/1)
     * @return member 字符串
     */
    private String buildSyncQueueMember(Long userId, String tripId, Integer isBackup) {
        return userId + "," + tripId + "," + isBackup;
    }


    /**
     * 删除某个行程在延迟队列中的所有同步任务，避免“删除后被异步同步复活”。
     *
     * @param userId 用户ID
     * @param tripId 行程ID
     */
    private void removeSyncQueueMembersForTrip(Long userId, String tripId) {
        if (userId == null || !StringUtils.hasText(tripId)) return;
        redisTemplate.opsForZSet().remove(redisKeyProperties.getSyncQueue(),
                buildSyncQueueMember(userId, tripId, TripConstant.NO_BACKUP),
                buildSyncQueueMember(userId, tripId, TripConstant.BACKUP));
    }


    /**
     * 删除某个用户在延迟队列中的所有同步任务，避免“清空后被异步同步复活”。
     *
     * @param userId 用户ID
     */
    private void removeSyncQueueMembersForUser(Long userId) {
        if (userId == null) return;
        Set<Object> members = redisTemplate.opsForZSet().range(redisKeyProperties.getSyncQueue(), 0, -1);
        if (members == null || members.isEmpty()) return;
        String prefix = userId + ",";
        List<Object> toRemove = new ArrayList<>();
        for (Object m : members) {
            if (!(m instanceof String s)) continue;
            if (s.startsWith(prefix)) {
                toRemove.add(s);
            }
        }
        if (!toRemove.isEmpty()) {
            redisTemplate.opsForZSet().remove(redisKeyProperties.getSyncQueue(), toRemove.toArray());
        }
    }
    @Async
    protected void deleteMd(List<String> objectKeys){
        if(objectKeys != null && !objectKeys.isEmpty()){
            for (String key : objectKeys) {
                mdUtils.deleteMd(key);
            }
        }
    }

    @Override
    public List<String> getHotStyle() {
        String s = stringRedisTemplate.opsForValue().get(redisKeyProperties.getHot());
        if(!StringUtils.hasText(s)){
            return null;
        }
        return Arrays.stream(s.split(",")).toList();
    }

    @Override
    public void saveMdUrl(String tripId, Long userId, String objectKey) {
        QueryWrapper<TripMd> wrapper = new QueryWrapper<>();
        wrapper.eq("trip_id",tripId);
        TripMd tripMd1 = tripMdMapper.selectOne(wrapper);
        if(tripMd1 != null){
            //this.deleteMd(List.of(tripMd1.getObjectKey()));
            tripMd1.setObjectKey(objectKey);
            tripMd1.setUpdateAt(LocalDateTime.now());
            tripMdMapper.updateById(tripMd1);
            return;
        }
        TripMd tripMd = new TripMd();
        tripMd.setTripId(tripId);
        tripMd.setUserId(userId);
        tripMd.setObjectKey(objectKey);
        tripMd.setCreateAt(LocalDateTime.now());
        tripMd.setUpdateAt(LocalDateTime.now());
        tripMdMapper.insert(tripMd);
    }

    @Override
    public String getMdUrl(String tripId, Long userId) {
        if(!this.getTripById(tripId,userId).getCompleteStatus().equals(TripConstant.Complete_trip_ID)){
            throw new BusException(CodeEnum.TRIP_MD_ERROR);
        }
        String key = redisKeyProperties.buildMdKey(userId, tripId);
        String mdUrl = normalizeUrl(stringRedisTemplate.opsForValue().get(key));
        if(StringUtils.hasText(mdUrl)) {
            return mdUrl;
        }
        QueryWrapper<TripMd> wrapper = new QueryWrapper<>();
        wrapper.eq("trip_id", tripId);
        TripMd tripMd = tripMdMapper.selectOne(wrapper);
        if(tripMd != null){
            String urlMd = mdUtils.getTemporaryUrlMd(tripMd.getObjectKey());
            stringRedisTemplate.opsForValue().set(key, urlMd, 1, TimeUnit.DAYS);
            return urlMd;
        }
        return null;
    }
    @Override
    public void completeConvertDraft(String tripId, Long userId){
        TravelPlanResponse response = this.getTripById(tripId, userId);
        if(response != null){
            if(TripConstant.Complete_trip_ID.equals(response.getCompleteStatus())){
                this.updateCompleteStatus(tripId,userId,TripConstant.DRAFT_TRIP_ID);
                QueryWrapper<TripMd> wrapper = new QueryWrapper<>();
                wrapper.eq("trip_id", tripId);
                TripMd tripMd = tripMdMapper.selectOne(wrapper);
                if(tripMd != null){
                    this.deleteMd(List.of(tripMd.getObjectKey()));
                    tripMdMapper.delete(wrapper);
                    String mdKey = redisKeyProperties.buildMdKey(userId, tripId);
                    redisTemplate.delete(mdKey);
                    /*String urlMd = mdUtils.getTemporaryUrlMd(tripMd.getObjectKey());
                    redisTemplate.delete(urlMd);*/
                }
            }
        }
    }
    @Override
    @Transactional
    public String getMdBackUrl(String tripId, Long userId) {
        BackupInfo backupInfo = this.getBackup(tripId, userId);
        if(backupInfo == null || backupInfo.getObjectKey() == null){
            return null;
        }
        String key = redisKeyProperties.buildBackupMdKey(userId, tripId);
        String mdUrl = normalizeUrl(stringRedisTemplate.opsForValue().get(key));
        if(StringUtils.hasText(mdUrl)) {
            return mdUrl;
        }
        if(backupInfo.getObjectKey() != null){
            String url = mdUtils.getTemporaryUrlMd(backupInfo.getObjectKey());
            stringRedisTemplate.opsForValue().set(key, url, 1, TimeUnit.DAYS);
            return url;
        }
        return null;
       /* QueryWrapper<TripBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("trip_id", tripId);
        BackupInfo backup = this.getBackup(tripId, userId);
        if(backup != null && backup.getObjectKey() != null){
            String urlMd = mdUtils.getTemporaryUrlMd(backup.getObjectKey());
            redisTemplate.opsForValue().set(key,urlMd,1, TimeUnit.DAYS);
            return urlMd;
        }*/
    }
    /*private void insertBackupMd(String tripId, Long userId, String objectKey) {
        String key = redisKeyProperties.buildMdKey(userId, tripId) + ":" + TripConstant.BACKUP;
        String urlMd = mdUtils.getTemporaryUrlMd(objectKey);
        stringRedisTemplate.opsForValue().set(key, urlMd, 1, TimeUnit.DAYS);
    }
    private void delBackupMd(String tripId, Long userId, String objectKey) {
        String key = redisKeyProperties.buildMdKey(userId, tripId) + ":" + TripConstant.BACKUP;
        redisTemplate.delete(key);
    }*/

    private String normalizeUrl(String value) {
        if (!StringUtils.hasText(value)) return value;
        String s = value.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }


    /**
     * 将 Redis 中的行程计划落库，并基于去重键合并全局 POI/交通数据。
     * @param travelPlanResponse 行程计划
     * @param userId 用户ID
     */
    @Override
    @Transactional
    public void insertTrip(TravelPlanResponse travelPlanResponse,Long userId) {
        Trips trip = new Trips();
        trip.setTripId(travelPlanResponse.getTripId());
        trip.setUserId(userId);
        trip.setStatus(1);
        trip.setTitle(travelPlanResponse.getTitle());
        trip.setDescription(travelPlanResponse.getDescription());
        trip.setDestination(travelPlanResponse.getDestination());
        trip.setStartDate(travelPlanResponse.getStartDate());
        trip.setEndDate(travelPlanResponse.getEndDate());
        trip.setTotalDays(travelPlanResponse.getTotalDays());
        trip.setTravelerCount(travelPlanResponse.getTravelerCount());
        trip.setTotalPrice(travelPlanResponse.getTotalPrice());
        trip.setBudgetTotal(travelPlanResponse.getBudget());
        trip.setBudgetUsed(BigDecimal.ZERO);
        trip.setTravelStyle(travelPlanResponse.getTravelStyle());
        trip.setNotes(travelPlanResponse.getNotes());
        trip.setCompleteStatus(travelPlanResponse.getCompleteStatus());
        trip.setCreatedAt(LocalDateTime.now());
        Map<String,Integer> map = new HashMap<>();
        List<TravelPlanResponse.DayPlan> days = travelPlanResponse.getDays();
        for (TravelPlanResponse.DayPlan day : days) {
            TripSchedules tripSchedules = new TripSchedules();
            tripSchedules.setTripId(travelPlanResponse.getTripId());
            tripSchedules.setDayNumber(day.getDate());
            tripSchedules.setDate(day.getDateTime());
            tripSchedules.setTitle(day.getTitle());
            tripSchedules.setDescription(day.getDescription());
            tripSchedules.setBudgetPlanned(day.getBudgetPlanned());
            tripSchedules.setBudgetActual(BigDecimal.ZERO);
            tripSchedules.setWeather(day.getWeather());
            tripSchedules.setCreatedAt(LocalDateTime.now());
            tripSchedules.setUpdatedAt(LocalDateTime.now());
            tripSchedulesMapper.insert(tripSchedules);
            day.getItems().forEach(item -> {
                TripDayItem dbItem = new TripDayItem();
                BeanUtils.copyProperties(item, dbItem);
                dbItem.setId(null);
                dbItem.setTripId(travelPlanResponse.getTripId());
                dbItem.setScheduleId(tripSchedules.getId());
                dbItem.setUserId(userId);
                if(map.containsKey(item.getRefId())){
                    map.replace(item.getRefId(), map.get(item.getRefId())+1);
                }else {
                    map.put(item.getRefId(), 1);
                }
                tripDayItemMapper.insert(dbItem);
            });
        }
        if (travelPlanResponse.getTransportations() != null) {
            for (TripTransportation transportation : travelPlanResponse.getTransportations()) {
                String dedupKey = transportation == null ? null : transportation.getDedupKey();
                if (!StringUtils.hasText(dedupKey) && transportation != null) {
                    dedupKey = DedupKeyUtils.buildTransportationDedupKey(transportation.getTrainNumber(), transportation.getDepartureTime());
                    transportation.setDedupKey(dedupKey);
                }
                if (!StringUtils.hasText(dedupKey)) {
                    throw new BusException(CodeEnum.TRIP_SAVE_ERROR);
                }
                if(map.containsKey(dedupKey)){
                    Integer i = map.get(dedupKey);
                    map.remove(dedupKey);
                    try {
                        transportation.setId(null);
                        transportation.setCreateAt(LocalDateTime.now());
                        transportation.setRefCount(i);
                        tripTransportationMapper.insert(transportation);
                    } catch (DuplicateKeyException e) {
                        UpdateWrapper<TripTransportation> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("dedupKey", dedupKey)
                                .setSql("ref_count = IFNULL(ref_count, 0) + " + i);
                        int rows = tripTransportationMapper.update(null, updateWrapper);
                        if (rows <= 0) {
                            tripTransportationMapper.insert(transportation);
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    }
                }
            }
        }
        if (travelPlanResponse.getGaoDes() != null) {
            for (TripGaoDe tripGaoDe : travelPlanResponse.getGaoDes()) {
                String dedupKey = tripGaoDe == null ? null : tripGaoDe.getDedupKey();
                if (!StringUtils.hasText(dedupKey) && tripGaoDe != null) {
                    dedupKey = DedupKeyUtils.buildPoiDedupKey(tripGaoDe);
                    tripGaoDe.setDedupKey(dedupKey);
                }
                if (!StringUtils.hasText(dedupKey)) {
                    throw new BusException(CodeEnum.TRIP_SAVE_ERROR);
                }
                if(map.containsKey(dedupKey)){
                    Integer i = map.get(dedupKey);
                    map.remove(dedupKey);
                    try {
                        tripGaoDe.setId(null);
                        tripGaoDe.setCreateAt(LocalDateTime.now());
                        tripGaoDe.setRefCount(i);
                        tripGaoDeMapper.insert(tripGaoDe);
                    } catch (DuplicateKeyException e) {
                        UpdateWrapper<TripGaoDe> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("dedupKey", dedupKey)
                                .setSql("ref_count = IFNULL(ref_count, 0) + " + i);
                        int rows = tripGaoDeMapper.update(null, updateWrapper);
                        if (rows <= 0) {
                            tripGaoDe.setCreateAt(LocalDateTime.now());
                            tripGaoDe.setRefCount(1);
                            tripGaoDeMapper.insert(tripGaoDe);
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    }
                }

            }
        }
        tripsMapper.insert(trip);
    }

    /**
     * 异步保存trip(先删除后添加)
     * @param tripId
     * @param userId
     * @param isBackup
     */
    @Override
    @Transactional
    @TripLocks(user = LockMode.READ, trip = LockMode.READ, userIdIndex = 1, tripIdIndex = 0, waitMillis = 0, leaseMillis = 30000)
    public void syncToMysql(String tripId, Long userId,boolean isBackup) {
        String key = redisKeyProperties.buildPlanKey(userId, tripId);
        TravelPlanResponse travelPlanResponse = (TravelPlanResponse) redisTemplate.opsForValue().get(key);
        if(travelPlanResponse != null){
            travelPlanResponse.setIsSave(TripConstant.SAVE);
            if(!isBackup){
                TravelPlanResponse backupResponse = this.getBackupResponse(tripId, userId);
                if(backupResponse != null){
                    this.deleteMysqlTrip(tripId,!TripConstant.SAVE.equals(backupResponse.getIsSave()));
                }else {
                    this.deleteMysqlTrip(tripId,true);
                }
            }else {
                this.deleteMysqlTrip(tripId,true);
            }
            //this.deleteMysqlTrip(tripId,isBackup);
            this.insertTrip(travelPlanResponse,userId);
            /*String mdKey = redisKeyProperties.buildMdKey(userId, tripId);
            stringRedisTemplate.delete(mdKey);*/
            QueryWrapper<TripMd> wrapper4 = new QueryWrapper<>();
            wrapper4.eq("trip_id", tripId);
            Set<String> md = new HashSet<>();
            md.add(this.getBackObjectKey(tripId,userId));
            if(travelPlanResponse.getCompleteStatus().equals(TripConstant.DRAFT_TRIP_ID)){
                tripMdMapper.delete(wrapper4);
            } else if(travelPlanResponse.getCompleteStatus().equals(TripConstant.Complete_trip_ID)){
                TripMd tripMd = tripMdMapper.selectOne(wrapper4);
                md.add(tripMd.getObjectKey());
            }
            md.remove(null);
            if(!md.isEmpty()){
                mdUtils.delAllObjectsByFolder(userId + "/" + tripId,md);
            }else {
                mdUtils.delAllObjectsByFolder(userId + "/" + tripId,null);
            }
            redisTemplate.opsForValue().set(key,travelPlanResponse,1,TimeUnit.DAYS);
        }
    }

    private TravelPlanResponse getBackupResponse(String tripId, Long userId) {
        String key = redisKeyProperties.buildBackupPlanKey(userId, tripId);
        Object o = redisTemplate.opsForValue().get(key);
        if(verifyUtil.verifyObject(o)){
            return null;
        }
        TravelPlanResponse response = null;
        if(o != null) {
            BackupDetails backupDetails = (BackupDetails) o;
            response = backupDetails.getTravelPlanResponse();
        }else {
            QueryWrapper<TripBackup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("trip_id",tripId);
            queryWrapper.eq("user_id", userId);
            TripBackup tripBackup = tripBackupMapper.selectOne(queryWrapper);
            if(tripBackup != null){
                String json = null;
                try {
                    json = BackupUtil.getTravelPlanResponseJson(tripBackup.getContent(),tripBackup.getContentFormat());
                } catch (IOException e) {
                    throw new BusException(CodeEnum.SYSTEM_ERROR);
                }
                response = JSON.parseObject(json, TravelPlanResponse.class);
                BackupDetails backupDetails = new BackupDetails();
                backupDetails.setTravelPlanResponse(response);
                backupDetails.setExpireTime(tripBackup.getExpireAt());
                backupDetails.setObjectKey(tripBackup.getObjectKey());
                redisTemplate.opsForValue().set(key,backupDetails,1,TimeUnit.DAYS);
            }
        }
        return response;
    }
    private String getBackObjectKey(String tripId,Long userId) {
        String key = redisKeyProperties.buildBackupPlanKey(userId, tripId);
        Object object = redisTemplate.opsForValue().get(key);
        if(verifyUtil.verifyObject(object)){
            return null;
        }
        if(object != null){
            return ((BackupDetails) object).getObjectKey();
        }
        QueryWrapper<TripBackup> wrapper = new QueryWrapper<>();
        wrapper.eq("trip_id", tripId);
        TripBackup tripBackup = tripBackupMapper.selectOne(wrapper);
        if(tripBackup != null){
            return tripBackup.getObjectKey();
        }
        return null;
    }
    /**
     * 获取下载md url
     * @param tripId id
     * @return url
     */
    @Override
    public String download(String tripId) {
        QueryWrapper<TripMd> wrapper = new QueryWrapper<>();
        wrapper.eq("trip_id", tripId);
        TripMd tripMd = tripMdMapper.selectOne(wrapper);
        if(tripMd != null){
            return mdUtils.getDownloadUrlByPath(tripMd.getObjectKey(),tripId + ".md");
        }
        throw new BusException(CodeEnum.TRIP_MD_ERROR);
    }

    /**
     * 返回自定义poi搜索框
     * @param type 类型
     * @return poi集合
     */
    @Override
    public List<String> getPoiName(Integer type) {
        String key;
        if(TripConstant.ACCOMMODATION_TYPE.equals(type)){
            key = redisKeyProperties.buildPoiKey(TripConstant.ACCOMMODATION);
        } else if (TripConstant.SCENIC_SPOT_TYPE.equals(type)) {
            key = redisKeyProperties.buildPoiKey(TripConstant.SCENIC);
        } else if (TripConstant.CATERING_TYPE.equals(type)) {
            key = redisKeyProperties.buildPoiKey(TripConstant.CATERING);
        }else {
            throw new BusException(CodeEnum.TRIP_SEARCH_TYPE_ERROR);
        }
        String text = (String) redisTemplate.opsForValue().get(key);
        if(text != null){
            return Arrays.asList(text.split(","));
        }
        List<String> poiName = tripGaoDeMapper.getGaoDePoiName(type);
        if(!poiName.isEmpty()){
            String string = String.join(",", poiName);
            redisTemplate.opsForValue().set(key, string);
        }
        return poiName;
    }

    /**
     * 检验
     * @param travelPlanResponse 旅行计划
     */
    @Override
    public void checkTravelPlanResponse(TravelPlanResponse travelPlanResponse) {
        isQualified(travelPlanResponse);
        checkItem(travelPlanResponse);
    }

    /**
     *  检查travelPlanResponse各种元素个数是否达到上限
     * @param travelPlanResponse 旅行计划
     */
    public void isQualified(TravelPlanResponse travelPlanResponse){
        if(travelPlanResponse.getTravelStyle().size() > 10){
            throw new BusException(CodeEnum.TRIP_STYLE_SIZE_ERROR);
        } else if (travelPlanResponse.getGaoDes().size() > 100) {
            throw new BusException(CodeEnum.TRIP_GAODE_SIZE_ERROR);
        } else if (travelPlanResponse.getTransportations().size() > 20) {
            throw new BusException(CodeEnum.TRIP_TRANSPORTATION_SIZE_ERROR);
        } else if (travelPlanResponse.getDays().size() > 10) {
            throw new BusException(CodeEnum.TRIP_DAY_SIZE_ERROR);
        }
        for (TravelPlanResponse.DayPlan day : travelPlanResponse.getDays()) {
            if(day.getItems().size() > 10){
                throw new BusException(CodeEnum.TRIP_ITEM_SIZE_ERROR);
            }
        }
    }

    /**
     * 对transportations和tripGaoDes 去多余和去重
     * @param travelPlanResponse 旅行计划
     */
    public void checkItem(TravelPlanResponse travelPlanResponse){
        Set<String> set = new HashSet<>();
        for (TravelPlanResponse.DayPlan day : travelPlanResponse.getDays()) {
            day.getItems().forEach(item -> {
                set.add(item.getRefId());
            });
        }
        List<TripTransportation> transportations = travelPlanResponse.getTransportations().stream().filter(trans -> {
            return set.contains(trans.getDedupKey());
        }).toList();
        List<TripGaoDe> tripGaoDes = travelPlanResponse.getGaoDes().stream().filter(tripGaoDe -> {
            return set.contains(tripGaoDe.getDedupKey());
        }).toList();
        travelPlanResponse.setTransportations(transportations);
        travelPlanResponse.setGaoDes(tripGaoDes);

    }

    public Long randomTime(){
        return (long) (60 * 24 + new Random().nextInt(60));
    }
}
