package com.itbaizhan.travelcommon.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 航班详细信息实体类
 * 用于映射航班动态查询接口返回的数据
 */
@Data
public class FlightDetail {
    private String dedupKey;
    /** 航班号 (例如: CA1523) */
    @JsonProperty("FlightNo")
    private String FlightNo;

    @JsonProperty("fservice")
    private String fservice;

    /** 航空公司 (例如: 中国国际航空股份有限公司) */
    @JsonProperty("FlightCompany")
    private String FlightCompany;

    /** 航班状态 (例如: 计划) */
    @JsonProperty("FlightState")
    private String FlightState;

    /** 计划起飞时间 (格式: yyyy-MM-dd HH:mm:ss) */
    @JsonProperty("FlightDeptimePlanDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime FlightDeptimePlanDate;

    /** 计划到达时间 (格式: yyyy-MM-dd HH:mm:ss) */
    @JsonProperty("FlightArrtimePlanDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime FlightArrtimePlanDate;

    /** 出发机场名称 (例如: 北京首都) */
    @JsonProperty("FlightDepAirport")
    private String FlightDepAirport;

    /** 到达机场名称 (例如: 上海虹桥) */
    @JsonProperty("FlightArrAirport")
    private String FlightArrAirport;

    /** 出发航站楼 (例如: T3) */
    @JsonProperty("FlightHTerminal")
    private String FlightHTerminal;

    /** 到达航站楼 (例如: T2) */
    @JsonProperty("FlightTerminal")
    private String FlightTerminal;

    /** 值机柜台 (例如: J,K) */
    @JsonProperty("CheckinTable")
    private String CheckinTable;

    /** 登机口 (例如: C62) */
    @JsonProperty("BoardGate")
    private String BoardGate;

    /** 行李转盘 (例如: 22) */
    @JsonProperty("BaggageID")
    private String BaggageID;

    /** 飞机编号 (例如: B327V) */
    @JsonProperty("AircraftNumber")
    private String AircraftNumber;

    /** 飞行时长，单位分钟 (例如: 99) */
    @JsonProperty("FlightDuration")
    private String FlightDuration;

}