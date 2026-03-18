package com.itbaizhan.travel_trip_service.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.fastjson2.JSON;
import com.itbaizhan.travel_trip_service.config.RedisKeyProperties;
import com.itbaizhan.travel_trip_service.constant.TripConstant;
import com.itbaizhan.travelcommon.AiSessionDto.ChatDto;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanRequest;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanResponse;
import com.itbaizhan.travelcommon.info.BackupInfo;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.service.TravelPlanService;
import com.itbaizhan.travelcommon.service.TripsService;
import com.itbaizhan.travelcommon.util.JWTUtil;
import com.itbaizhan.travelcommon.vo.TripVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trip")
@Slf4j
public class TripsController {
    @Autowired
    private TripsService tripsService;

    @Autowired
    private TravelPlanService travelPlanService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedisKeyProperties redisKeyProperties;

    @PostMapping(value = "/plan", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generatePlanStream(@RequestBody TravelPlanRequest request,
                                         @RequestHeader("Authorization") String authorization
    ) {
        Map<String, Object> verify = verifyToken(authorization);
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        travelPlanService.generatePlanStream(request, emitter, (Long) verify.get("userId"));
        return emitter;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatDto chatDto,
                           @RequestHeader("Authorization") String authorization){
        Map<String, Object> stringObjectMap = verifyToken(authorization);
        if(!StringUtils.hasText(chatDto.getCurrent())){

        }
        if(chatDto.getIsBackup() == null){
            chatDto.setIsBackup(TripConstant.NO_BACKUP);
        }

        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        travelPlanService.chat(chatDto.getQuestion(), chatDto.getCurrent(), emitter,(Long) stringObjectMap.get("userId"),chatDto.getTripId(),chatDto.getIsBackup());
        return emitter;
    }
    @GetMapping("/list")
    public BaseResult<Page<TripVo>> getTripsList(@RequestParam(defaultValue = "1")int page,
                                                 @RequestParam(defaultValue = "10")int size,
                                                 @RequestHeader("Authorization") String authorization) {
        Map<String, Object> verify = verifyToken(authorization);
        Page<TripVo> userId = tripsService.getTripsVoById(page, size, (Long) verify.get("userId"));
        return BaseResult.success(userId);
    }
    @GetMapping("/tripById")
    public BaseResult<TravelPlanResponse> getTripById(@RequestParam("tripId") String tripId
            ,@RequestHeader("Authorization") String authorization) {
        Map<String, Object> map = verifyToken(authorization);
        TravelPlanResponse response = tripsService.getTripById(tripId, (Long) map.get("userId"));
        return BaseResult.success(response);
    }

    @GetMapping(value = "/ongoing/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOngoingStatus(@RequestParam("tripId") String tripId,
                                          @RequestHeader("Authorization") String authorization) {
        Map<String, Object> map = verifyToken(authorization);
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        tripsService.generateAndModifyStream(tripId, (Long) map.get("userId"), emitter,false);
        return emitter;
    }
    @GetMapping(value = "/ongoing/streamModify", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOngoingModStatus(@RequestParam("tripId") String tripId,
                                          @RequestHeader("Authorization") String authorization) {
        Map<String, Object> map = verifyToken(authorization);
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        tripsService.generateAndModifyStream(tripId, (Long) map.get("userId"), emitter,true);
        return emitter;
    }
    @GetMapping(value = "/ongoing/streamGenerateMd", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGenerateMd(@RequestParam("tripId") String tripId,
                                             @RequestHeader("Authorization") String authorization) {
        Map<String, Object> map = verifyToken(authorization);
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        tripsService.generateMdStream(tripId, (Long) map.get("userId"),emitter);
        return emitter;
    }

    /**
     * 获取某个 tripId 的最新“生成中”状态快照（用于刷新后回显）。
     *
     * @param tripId         行程ID
     * @param authorization  Bearer token
     * @return 最新快照（不存在则返回 null）
     */
    @GetMapping("/ongoing/latest")
    public BaseResult<Map<String, Object>> getOngoingLatest(@RequestParam("tripId") String tripId,
                                                            @RequestParam(value = "streamType", required = false) Integer streamType,
                                                            @RequestHeader("Authorization") String authorization) {
        Map<String, Object> map = verifyToken(authorization);
        Long userId = (Long) map.get("userId");
        String ongoingKey = redisKeyProperties.buildOngoingPlanKey(userId);
        String id = tripId == null ? "" : tripId.trim();
        if (id.isEmpty()) {
            return BaseResult.success(null);
        }

        Object raw = null;
        if (streamType != null) {
            raw = redisTemplate.opsForHash().get(ongoingKey, id + ":" + streamType);
        } else if (id.contains(":")) {
            raw = redisTemplate.opsForHash().get(ongoingKey, id);
        } else {
            raw = redisTemplate.opsForHash().get(ongoingKey, id + ":" + TripConstant.MODIFY_TRIP_ID);
            if (raw == null) {
                raw = redisTemplate.opsForHash().get(ongoingKey, id + ":" + TripConstant.GENERATE_TRIP_ID);
            }
            if(raw == null){
                raw = redisTemplate.opsForHash().get(ongoingKey, id + ":" + TripConstant.PROCESSING_MD_ID);
            }
        }
        if (raw == null) {
            return BaseResult.success(null);
        }
        try {
            return BaseResult.success(JSON.parseObject(String.valueOf(raw)));
        } catch (Exception e) {
            return BaseResult.success(null);
        }
    }
    @DeleteMapping("/delete")
    public BaseResult<?> deleteTripById(@RequestParam("tripId") String tripId
            ,@RequestHeader("Authorization") String authorization) {
        Map<String, Object> ma = verifyToken(authorization);
        tripsService.deleteTrip(tripId, (Long) ma.get("userId"));
        return BaseResult.success();
    }
    @DeleteMapping("/clear")
    public BaseResult<?> clearTripsByUserId(@RequestParam("userId") Long userId
            ,@RequestHeader("Authorization") String authorization) {
        verifyToken(authorization);
        tripsService.clear(userId);
        return BaseResult.success();
    }
    @GetMapping("/getBackup")
    public BaseResult<BackupInfo> getBackup(@RequestParam String tripId,
                                            @RequestHeader("Authorization") String authorization){
        Map<String, Object> map = verifyToken(authorization);
        BackupInfo backup = tripsService.getBackup(tripId,(Long) map.get("userId"));
        return BaseResult.success(backup);
    }
    @GetMapping("/restoreBackup")
    public BaseResult<?> restoreBackup(@RequestParam String tripId,
            @RequestHeader("Authorization") String authorization){
        Map<String, Object> map = verifyToken(authorization);
        tripsService.restoreBackup(tripId,(Long) map.get("userId"));
        return BaseResult.success();
    }
    @GetMapping("/backupCover")
    public BaseResult<?> backupCover(@RequestParam String tripId,@RequestHeader("Authorization") String authorization){
        Map<String, Object> map = verifyToken(authorization);
        tripsService.backupCover(tripId,(Long) map.get("userId"));
        return BaseResult.success();
    }
    @DeleteMapping("/deleteBackup")
    public BaseResult<?> deleteBackup(@RequestParam String tripId,@RequestHeader("Authorization") String authorization){
        Map<String, Object> map = verifyToken(authorization);
        tripsService.deleteBackup(tripId,(Long) map.get("userId"));
        return BaseResult.success();
    }
    @PostMapping("/save")
    public BaseResult<?> save(@RequestBody TravelPlanResponse response,@RequestHeader("Authorization") String authorization) {
        Map<String, Object> verify = verifyToken(authorization);
        tripsService.insertRedisTravel(response,(Long) verify.get("userId"), TripConstant.NO_BACKUP);
        return BaseResult.success();
    }
    @PostMapping("/backup")
    public BaseResult<?> backup(@RequestBody TravelPlanResponse response,@RequestHeader("Authorization") String authorization) {
        Map<String, Object> verify = verifyToken(authorization);
        tripsService.insertRedisTravel(response,(Long) verify.get("userId"), TripConstant.BACKUP);
        return BaseResult.success();
    }
    @GetMapping("/generateMd")
    public SseEmitter generateMd(@RequestParam String tripId,
                                       @RequestHeader("Authorization") String authorization) {
        Map<String, Object> map = verifyToken(authorization);
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        travelPlanService.generateMd(tripId, (Long) map.get("userId"),emitter);
        return emitter;
    }

    public Map<String, Object> verifyToken(String authorization) {
        String token = authorization.replace("Bearer ","");
        return JWTUtil.verify(token);
    }
    @GetMapping("/getHotStyle")
    public BaseResult<List<String>> getHotStyle() {
        //verifyToken(authorization);
        List<String> hotStyle = tripsService.getHotStyle();
        return BaseResult.success(hotStyle);
    }
    @GetMapping("/mdUrl")
    public BaseResult<String> getMdUrl(@RequestParam String tripId,
                                       @RequestHeader("Authorization") String authorization) {
        Map<String, Object> verify = verifyToken(authorization);
        String mdUrl = tripsService.getMdUrl(tripId, (Long) verify.get("userId"));
        return BaseResult.success(mdUrl);
    }
    @GetMapping("/downloadMd")
    public BaseResult<String> downloadMd(@RequestParam String tripId,
                                       @RequestHeader("Authorization") String authorization) {
        verifyToken(authorization);
        String downloadUrl = tripsService.download(tripId);
        return BaseResult.success(downloadUrl);
    }
    @GetMapping("/getPoiName")
    public BaseResult<List<String>> getPoiName(@RequestParam Integer type,@RequestHeader("Authorization") String authorization){
        verifyToken(authorization);
        return BaseResult.success(tripsService.getPoiName(type));
    }
    @GetMapping("/completeConvertDraft")
    public BaseResult<?> completeConvertDraft(@RequestParam String tripId,@RequestHeader("Authorization") String authorization){
        Map<String, Object> map = verifyToken(authorization);
        tripsService.completeConvertDraft(tripId,(Long) map.get("userId"));
        return BaseResult.success();
    }

    @GetMapping("/backupMdUrl")
    public BaseResult<String> getBackupMdUrl(@RequestParam String tripId,
                                             @RequestHeader("Authorization") String authorization) {
        Map<String, Object> verify = verifyToken(authorization);
        String mdUrl = tripsService.getMdBackUrl(tripId, (Long) verify.get("userId"));
        return BaseResult.success(mdUrl);
    }
}
