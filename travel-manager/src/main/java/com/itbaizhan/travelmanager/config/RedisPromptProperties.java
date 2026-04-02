package com.itbaizhan.travelmanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * 与 travel_AI_trip 中 {@code RedisPromptProperties} 配置前缀一致，保证缓存 key 相同。
 */
@Data
@ConfigurationProperties(prefix = "redis.prompt")
public class RedisPromptProperties {
    private String agentNextStep;
    private String normal;
    private String allModify;
    private String allGenerate;
    private String allTool;

    public String getRedisKey(String key) {
        return normal + key;
    }

}
