package com.itbaizhan.travelcommon.AiSessionDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;


import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TransDto implements Serializable {
    private String fromCity;
    private String toCity;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private Integer type;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
}
