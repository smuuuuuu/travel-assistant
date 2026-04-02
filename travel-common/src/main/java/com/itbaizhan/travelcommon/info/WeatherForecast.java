package com.itbaizhan.travelcommon.info;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WeatherForecast implements Serializable {
    private String city;
    private String adcode;
    private String province;
    private String reportTime;
    private List<DailyForecast> forecasts;
}