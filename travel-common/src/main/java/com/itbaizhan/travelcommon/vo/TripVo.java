package com.itbaizhan.travelcommon.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class TripVo  {
    private String tripId;
    private String title;
    private String destination;
    private Integer totalDays;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private Integer status;  //行程状态
}
