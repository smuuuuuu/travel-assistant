package com.itbaizhan.travelmanager.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FAQ 导入完成后通过 Redis 发布通知，便于网关/前端 SSE/WebSocket 订阅同一 channel 推送给操作者。
 * Channel: {@value FaqImportCompletedListener#REDIS_TOPIC}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaqImportCompletedListener {

    public static final String REDIS_TOPIC = "manager:faq:import:notify";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onFaqImportCompleted(FaqImportCompletedEvent event) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "FAQ_IMPORT_DONE");
            payload.put("username", event.getOperatorUsername());
            payload.put("insertedCount", event.getInsertedCount());
            payload.put("success", event.isSuccess());
            payload.put("message", event.getMessage());
            payload.put("ts", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.convertAndSend(REDIS_TOPIC, json);
            log.info("FAQ import notify published: success={}, count={}", event.isSuccess(), event.getInsertedCount());
        } catch (Exception e) {
            log.warn("Failed to publish FAQ import notification", e);
        }
    }
}
