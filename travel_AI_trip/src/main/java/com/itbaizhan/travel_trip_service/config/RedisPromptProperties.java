package com.itbaizhan.travel_trip_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "redis.prompt")
public class RedisPromptProperties {
    private String normal;
    private String modify;
    private String generate;
    private String toolRestriction;
    private String allModify;
    private String allGenerate;
    private String precheckModify;
    private String precheckGeneration;
    private String generateMd;
    private String poiSearch;
    private String argHint;
    private String quotaHint;
    private String allTool;

    public String getRedisKey(String key){
        return normal + key;
    }
}
