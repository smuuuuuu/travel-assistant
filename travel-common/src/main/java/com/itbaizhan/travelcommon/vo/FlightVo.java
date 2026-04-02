package com.itbaizhan.travelcommon.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class FlightVo {
    private String dedupkey;
    private String flightNo;
    private LocalTime planDepTime;
    private LocalTime planArrTime;
    private LocalDate date;
    private String duration;
    private Double price;
    private String seatClass;
    private String status;
}
