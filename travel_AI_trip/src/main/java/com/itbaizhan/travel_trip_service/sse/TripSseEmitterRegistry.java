package com.itbaizhan.travel_trip_service.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class TripSseEmitterRegistry {

    private final ConcurrentHashMap<String, CopyOnWriteArraySet<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    /**
     * SSE 心跳：避免长时间无数据导致连接被代理/中间件关闭。
     */
    @Scheduled(fixedDelayString = "${sse.heartbeat-interval-ms:15000}")
    public void heartbeat() {
        if (emitterMap.isEmpty()) return;
        for (Map.Entry<String, CopyOnWriteArraySet<SseEmitter>> entry : emitterMap.entrySet()) {
            CopyOnWriteArraySet<SseEmitter> set = entry.getValue();
            if (set == null || set.isEmpty()) continue;
            for (SseEmitter emitter : set) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Throwable t) {
                    if (t instanceof VirtualMachineError) {
                        throw (VirtualMachineError) t;
                    }
                    set.remove(emitter);
                    if (set.isEmpty()) {
                        emitterMap.remove(entry.getKey());
                    }
                    try {
                        emitter.complete();
                    } catch (Throwable ignored) {
                        if (ignored instanceof VirtualMachineError) {
                            throw (VirtualMachineError) ignored;
                        }
                    }
                }
            }
        }
    }

    /**
     * 注册某个行程的 SSE 连接。
     *
     * @param userId  用户ID
     * @param tripId  行程ID
     * @param emitter SSE连接
     */
    public void register(Long userId, String tripId, SseEmitter emitter) {
        String key = buildKey(userId, tripId);
        emitterMap.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(emitter);
    }

    /**
     * 移除某个行程的 SSE 连接。
     *
     * @param userId  用户ID
     * @param tripId  行程ID
     * @param emitter SSE连接
     */
    public void remove(Long userId, String tripId, SseEmitter emitter) {
        String key = buildKey(userId, tripId);
        Set<SseEmitter> set = emitterMap.get(key);
        if (set == null) return;
        set.remove(emitter);
        if (set.isEmpty()) {
            emitterMap.remove(key);
        }
    }

    /**
     * 向某个行程的所有在线 SSE 连接发送事件。
     *
     * @param userId 用户ID
     * @param tripId 行程ID
     * @param name   SSE事件名
     * @param data   事件数据
     */
    public void send(Long userId, String tripId, String name, Object data) {
        String key = buildKey(userId, tripId);
        Set<SseEmitter> set = emitterMap.get(key);
        if (set == null || set.isEmpty()) return;

        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event().name(name).data(data));
            } catch (Throwable t) {
                if (t instanceof VirtualMachineError) {
                    throw (VirtualMachineError) t;
                }
                remove(userId, tripId, emitter);
                try {
                    emitter.complete();
                } catch (Throwable ignored) {
                    if (ignored instanceof VirtualMachineError) {
                        throw (VirtualMachineError) ignored;
                    }
                }
            }
        }
    }

    /**
     * 关闭并清理某个行程的所有在线 SSE 连接。
     *
     * @param userId 用户ID
     * @param tripId 行程ID
     */
    public void completeAll(Long userId, String tripId) {
        String key = buildKey(userId, tripId);
        Set<SseEmitter> set = emitterMap.remove(key);
        if (set == null || set.isEmpty()) return;
        for (SseEmitter emitter : set) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private String buildKey(Long userId, String tripId) {
        return String.valueOf(userId) + ":" + String.valueOf(tripId);
    }
}
