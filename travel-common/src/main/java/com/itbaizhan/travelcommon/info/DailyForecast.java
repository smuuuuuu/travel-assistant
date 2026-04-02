package com.itbaizhan.travelcommon.info;

import lombok.Data;

import java.io.Serializable;

@Data
public class DailyForecast implements Serializable {
    private String date;         // 日期
    private String week;         // 星期
    private String dayWeather;   // 白天天气
    private String nightWeather; // 夜间天气
    private String dayTemp;      // 白天温度
    private String nightTemp;    // 夜间温度
    private String dayWind;      // 白天风向
    private String nightWind;    // 夜间风向
    private String dayPower;     // 白天风力
    private String nightPower;   // 夜间风力
    private String dayIcon;      // 白天图标
    private String nightIcon;    // 夜间图标
}