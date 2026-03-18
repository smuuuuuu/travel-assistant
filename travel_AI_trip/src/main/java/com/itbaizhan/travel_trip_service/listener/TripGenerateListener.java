package com.itbaizhan.travel_trip_service.listener;


import com.alibaba.fastjson2.JSON;
import com.itbaizhan.travel_trip_service.config.RedisKeyProperties;
import com.itbaizhan.travelcommon.mq.dto.TripProgressEvent;
import com.itbaizhan.travel_trip_service.sse.TripSseEmitterRegistry;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(topic = "${demo.rocketmq.topic}",consumerGroup = "${demo.rocketmq.consumerGroup}")
public class TripGenerateListener implements RocketMQListener<String> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedisKeyProperties redisKeyProperties;
    @Autowired
    private TripSseEmitterRegistry emitterRegistry;

    /**
     * 消费行程生成进度消息：写入 Redis 快照，并向在线 SSE 连接推送。
     *
     * @param message RocketMQ 消息体（JSON 字符串）
     */
    @Override
    public void onMessage(String message) {
        try {
            TripProgressEvent event;
            try {
                event = JSON.parseObject(message, TripProgressEvent.class);
            } catch (Exception ignored) {
                return;
            }
            if (event == null || event.getUserId() == null || event.getTripId() == null) {
                return;
            }
            String ongoingKey = redisKeyProperties.buildOngoingPlanKey(event.getUserId());
            Integer streamType = event.getStreamType() == null ? 1 : event.getStreamType();
            String tripIdKey = event.getTripId() + ":" + streamType;

            long incomingSeq = event.getSeq() == null ? 0L : event.getSeq();
            Long lastSeq = readSeqFromSnapshot(ongoingKey, tripIdKey);
            if (lastSeq != null && incomingSeq <= lastSeq) {
                return;
            }

            String type = event.getType() == null ? "progress" : event.getType();
            if ("done".equalsIgnoreCase(type)) {
                emitterRegistry.send(event.getUserId(), tripIdKey, "done",
                        Map.of("status", event.getStatus(), "tripId", event.getTripId(), "streamType", streamType));
                redisTemplate.opsForHash().delete(ongoingKey, tripIdKey);
                emitterRegistry.completeAll(event.getUserId(), tripIdKey);
                return;
            }

            if ("error".equalsIgnoreCase(type)) {
                emitterRegistry.send(event.getUserId(), tripIdKey, "error",
                        Map.of("message", event.getStatus() == null ? "生成行程失败，请稍后重试" : event.getStatus(),
                                "tripId", event.getTripId(), "streamType", streamType));
                redisTemplate.opsForHash().delete(ongoingKey, tripIdKey);
                emitterRegistry.completeAll(event.getUserId(), tripIdKey);
                return;
            }

            Map<String, Object> snapshot = buildSnapshot(event);
            redisTemplate.opsForHash().put(ongoingKey, tripIdKey, JSON.toJSONString(snapshot));
            redisTemplate.expire(ongoingKey, 30, TimeUnit.MINUTES);
            emitterRegistry.send(event.getUserId(), tripIdKey, "progress",
                    Map.of("status", event.getStatus(), "tripId", event.getTripId(), "streamType", streamType));
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError) {
                throw (VirtualMachineError) t;
            }
        }
    }

    /**
     * 从 Redis ongoing 快照中读取 seq（用于幂等过滤）。
     *
     * @param ongoingKey ongoing hash key
     * @param tripId     hash field
     * @return 已处理的最大 seq
     */
    private Long readSeqFromSnapshot(String ongoingKey, String tripId) {
        Object raw = redisTemplate.opsForHash().get(ongoingKey, tripId);
        if (raw == null) return null;
        try {
            Map<String, Object> map = JSON.parseObject(String.valueOf(raw));
            Object seq = map.get("seq");
            if (seq == null) return null;
            return Long.parseLong(String.valueOf(seq));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 构造 Redis 快照对象（用于刷新回显）。
     *
     * @param event MQ 进度事件
     * @return 快照 Map
     */
    private Map<String, Object> buildSnapshot(TripProgressEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("tripId", event.getTripId());
        map.put("streamType", event.getStreamType());
        map.put("status", event.getStatus());
        map.put("type",event.getType());
        map.put("destination", event.getDestination());
        map.put("totalDays", event.getTotalDays());
        map.put("seq", event.getSeq());
        map.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return map;
    }
}
