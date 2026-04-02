package com.itbaizhan.travelmanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * 与 travel_AI_trip 中 {@code RedisKeyProperties} 配置前缀一致。
 */
@Data
@ConfigurationProperties(prefix = "redis.key")
public class RedisKeyProperties {
    private String hot;
    private String poi;
    private String stationCode;
    private String iataCode;
    private String cityCode;
    private String aiModuleEnable;

    public String buildPoiKey(String type){
        return poi + type;
    }
}
