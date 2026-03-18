package com.itbaizhan.travel_trip_service.task;

import com.itbaizhan.travel_trip_service.config.RedisKeyProperties;
import com.itbaizhan.travel_trip_service.constant.TripConstant;
import com.itbaizhan.travel_trip_service.mapper.TripGaoDeMapper;
import com.itbaizhan.travel_trip_service.mapper.TripTransportationMapper;
import com.itbaizhan.travel_trip_service.mapper.TripsMapper;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.service.TripsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class TripSyncTask {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TripsService tripsService;
    @Autowired
    private TripsMapper tripsMapper;
    @Autowired
    private TripTransportationMapper tripTransportationMapper;
    @Autowired
    private TripGaoDeMapper tripGaoDeMapper;

    @Autowired
    private RedisKeyProperties redisKeyProperties;
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void syncTrips() {
        tripsMapper.deleteExpireBackup();
    }

    /**
     * 每日清理 ref_count 归零且已无 TripDayItem 引用的全局 POI/交通资源（分批删除，避免锁表）。
     */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOrphanGlobalResources() {
        int batchLimit = 2000;
        int maxBatches = 20;
        for (int i = 0; i < maxBatches; i++) {
            int deletedTrans = tripTransportationMapper.deleteOrphanByRefCount(batchLimit);
            int deletedPoi = tripGaoDeMapper.deleteOrphanByRefCount(batchLimit);
            if (deletedTrans <= 0 && deletedPoi <= 0) {
                return;
            }
        }
    }
    // 每2秒轮询一次
    @Scheduled(fixedDelay = 2000)
    public void processSyncQueue() {
        long now = System.currentTimeMillis();

        // 1. 取出所有已到期的任务 (Score <= 当前时间)
        Set<Object> values = redisTemplate.opsForZSet().rangeByScore(redisKeyProperties.getSyncQueue(), 0, now);

        if (values == null || values.isEmpty()) {
            return;
        }

        for (Object obj : values) {
            String value = (String) obj;

            // 2. 从 ZSet 中移除 (防止重复执行)
            // 注意：这里先移除再执行，如果执行失败可能丢失。
            // 更严谨的做法是使用 Lua 脚本原子化 "获取并移除"。
            Long removeCount = redisTemplate.opsForZSet().remove(redisKeyProperties.getSyncQueue(), value);

            if (removeCount != null && removeCount > 0) {
                try {
                    // 3. 执行真正的落库逻辑
                    // 注意：这里需要根据 tripId 找到 userId，或者 ZSet member 存 "userId:tripId"
                    // 假设能解析出 userId
                    List<String> strings = Arrays.stream(value.split(",")).toList();
                    Long userId = Long.parseLong(strings.get(0));
                    String tripId = strings.get(1);
                    String isBackup = strings.get(2);
                    if (!shouldProcessTask(userId, tripId, Integer.parseInt(isBackup))) {
                        continue;
                    }
                    if(TripConstant.BACKUP.equals(Integer.parseInt(isBackup))){
                        tripsService.updateMysqlBackup(tripId, userId);
                    }
                    tripsService.syncToMysql(tripId, userId,TripConstant.NO_BACKUP.equals(Integer.parseInt(isBackup)));
                    log.info("异步同步行程成功: {}", tripId);
                } catch (BusException e) {
                    log.warn("异步同步行程跳过（锁冲突/清空中）: {}", e.getMsg());
                    redisTemplate.opsForZSet().add(redisKeyProperties.getSyncQueue(), value, now + 10_000);
                } catch (Exception e) {
                    log.error("同步失败，重试或记录日志: " + values, e);
                    // 可选：失败重试，重新加回 ZSet，Score 设为 1 分钟后
                }
            }
        }
    }

    /**
     * 判断延迟队列任务是否仍应执行，避免删除/清空后被“异步同步复活”。
     *
     * @param userId   用户ID
     * @param tripId   行程ID
     * @param isBackup 是否备份(0/1)
     * @return true 表示可执行
     */
    private boolean shouldProcessTask(Long userId, String tripId, int isBackup) {
        if (userId == null || tripId == null || tripId.isBlank()) return false;
        String key = TripConstant.NO_BACKUP.equals(isBackup)
                ? redisKeyProperties.buildPlanKey(userId, tripId)
                : redisKeyProperties.buildBackupPlanKey(userId, tripId);
        return redisTemplate.hasKey(key);
    }
}
