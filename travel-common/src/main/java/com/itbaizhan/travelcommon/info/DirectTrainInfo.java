package com.itbaizhan.travelcommon.info;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class DirectTrainInfo{
    private String from_station;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime arrive_time;
    private String start_train_code;//D5401
    @JsonFormat(pattern = "HH:mm")
    private LocalTime start_time;
    private String from_station_telecode; //UUH

    private List<String> dw_flag; // ["智能动车组","复兴号"]
    private String lishi;
    private String train_no; //5l000D540160

    private List<Price> prices;
    // prices":[{"seat_name":"二等座","price":185,"num":"无","short":"ze","discount":86,"seat_type_code":"O"}
    // ,{"seat_name":"一等座","price":294,"num":"17","short":"zy","discount":86,"seat_type_code":"M"}
    // ,{"seat_name":"无座","price":185,"num":"有","short":"wz","discount":86,"seat_type_code":"W"}]
    private String to_station_telecode; //NJH
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate start_date;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate arrive_date;
    private String to_station;//南京"}

    private String dedupKey;
    @Data
    public static class Price {
        private String seat_name;
        private Double price;
        private String num;
        @JSONField(name = "short")
        private String short_name;
        private Integer discount;
        private String seat_type_code;
    }
}
