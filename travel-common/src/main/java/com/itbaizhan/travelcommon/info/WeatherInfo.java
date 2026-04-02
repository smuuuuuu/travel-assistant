package com.itbaizhan.travelcommon.info;

import lombok.Data;

import java.io.Serializable;

@Data
public class WeatherInfo implements Serializable {
    private String province;
    private String city;
    private String adcode;
    private String weather;      // 天气现象
    private String temperature;  // 温度
    private String windDirection; // 风向
    private String windPower;    // 风力
    private String humidity;     // 湿度
    private String reportTime;   // 发布时间
    private String weatherIcon;  // 天气图标

    private WeatherForecast weatherForecast;
}