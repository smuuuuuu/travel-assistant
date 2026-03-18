package com.itbaizhan.travel_trip_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "redis.key")
public class RedisKeyProperties {
    /*public String HASH_TRANS_KEY_2_ID = "dedup:trans:key2id";
    public String HASH_TRANS_ID_2_KEY = "dedup:trans:id2key";
    public String HASH_POI_KEY_2_ID = "dedup:poi:key2id";
    public String HASH_POI_ID_2_KEY = "dedup:poi:id2key";

    public String LOCK_TRANS_PREFIX = "lock:trans:";
    public String LOCK_POI_PREFIX = "lock:poi:";*/

    private String hot;
    private String plan;
    private String planCount;
    private String md;
    private String poi;
    private String syncQueue;
    private String transportation;
    private String gaoDe;

    private String stationCode;

    private String iataCode;
    private String cityCode;
    private String train;
    private String highSpeed;
    private String flight;
    private String flightItineraries;
    private String accommodation;
    private String scenic;
    private String catering;
    private String tripVo;
    //private String prompt;
    private String aiModuleEnable;

    public String buildBackupPlanKey(Long userId,String tripId) {
        return plan + userId + ":" + tripId + ":backup";
    }
    public String buildPlanKey(Long userId,String tripId) {
        return plan + userId + ":" + tripId;
    }
    public String buildMdKey(Long userId,String tripId) {
        return md + userId + ":" + tripId;
    }
    public String buildBackupMdKey(Long userId,String tripId) {
        return md + userId + ":" + tripId + ":backup";
    }
    public String buildTrainKey(String fromCity,String toCity,String date){
        return train + fromCity + ":" + toCity + ":" + date;
    }
    public String buildHighKey(String fromCity,String toCity,String date){
        return highSpeed + fromCity + ":" + toCity + ":" + date;
    }
    public String buildFlightKey(String fromCity,String toCity,String date){
        return flight + fromCity + ":" + toCity + ":" + date;
    }
    public String buildFlightItinerariesKey(String fromCity,String toCity,String date){
        return flightItineraries + fromCity + ":" + toCity + ":" + date;
    }
    public String buildAccommodationKey(String city,String keywords){
        return accommodation + city + ":" + keywords;
    }
    public String buildScenicKey(String city,String keywords){
        return scenic + city + ":" + keywords;
    }
    public String buildCateringKey(String city,String keywords){
        return catering + city + ":" + keywords;
    }
    public String buildCountKey(Long userId){
        return planCount + userId;
    }
    public String buildPoiKey(String type){
        return poi + type;
    }
    public String buildOngoingPlanKey(Long userId){
        return plan+ "ongoing:" + userId;
    }
}
