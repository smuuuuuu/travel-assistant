package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.itbaizhan.travelcommon.info.Activities;
import lombok.Data;

/**
 * 行程日程表
 * @TableName trip_schedules
 */
@TableName(value ="trip_schedules")
@Data
public class TripSchedules implements Serializable {
    /**
     * 日程ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 行程ID
     */
    @TableField(value = "trip_id")
    private String tripId;

    /**
     * 第几天
     */
    @TableField(value = "day_number")
    private String dayNumber;

    /**
     * 日期
     */
    @TableField(value = "date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 当日标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 当日描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 活动安排
     */
    @TableField(value = "activities",typeHandler = JacksonTypeHandler.class)
    private List<Activities> activities;


     /*// 交通安排
    @TableField(value = "transportation",typeHandler = JacksonTypeHandler.class)
    private Transportation transportation;*/

    @TableField(value = "weather",typeHandler = JacksonTypeHandler.class)
    private WeatherInfo weather;

    public record WeatherInfo(
        String temperature,
        String condition
    ){}
    /*
     * 住宿安排
    @TableField(value = "accommodation")
    private String accommodation;
    /**
     * 餐饮安排
    @TableField(value = "meals")
    private String meals;*/

    /**
     * 计划预算
     */
    @TableField(value = "budget_planned")
    private BigDecimal budgetPlanned;

    /**
     * 实际花费
     */
    @TableField(value = "budget_actual")
    private BigDecimal budgetActual;

    /**
     * 备注
     */
    @TableField(value = "notes")
    private String notes;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}