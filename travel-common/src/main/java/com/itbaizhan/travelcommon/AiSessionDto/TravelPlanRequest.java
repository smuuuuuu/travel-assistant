package com.itbaizhan.travelcommon.AiSessionDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TravelPlanRequest {
    private String destination; //终点地
    private String departure; //出发地
    private Integer days;
    private Integer people;
    private String budget;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;
    private String transportation;
    private List<String> travelStyle;
    private String rawRequirement;

    private String FlightDepAirport;
    private String FlightArrAirport;
}