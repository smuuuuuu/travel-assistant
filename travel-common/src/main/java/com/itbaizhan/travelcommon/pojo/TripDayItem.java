package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 
 * @TableName trip_day_item
 */
@TableName(value ="trip_day_item")
@Data
public class TripDayItem implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_Id")
    private Long userId;
    /**
     * 
     */
    @TableField(value = "trip_id")
    private String tripId;

    /**
     * 
     */
    @TableField(value = "schedule_id")
    private Long scheduleId;

    /**
     * 
     */
    @TableField(value = "item_type")
    private String itemType;

    /**
     * 
     */
    @TableField(value = "ref_id")
    private String refId;

    /**
     * 
     */
    @TableField(value = "poi_type")
    private Integer poiType;

    /**
     * 
     */
    @JsonFormat(pattern = "HH:mm")
    @TableField(value = "start_time")
    private LocalTime startTime;

    /**
     * 
     */
    @JsonFormat(pattern = "HH:mm")
    @TableField(value = "end_time")
    private LocalTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "start_date")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField(value = "end_date")
    private LocalDate endDate;
    /**
     * 
     */
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 
     */
    @TableField(value = "note")
    private String note;

    private BigDecimal price;
    @TableField(value = "total_price")
    private BigDecimal totalPrice;

    /**
     * 
     */
    @TableField(value = "votes")
    private Integer votes;

    /**
     * 
     */
    @TableField(value = "seat_class")
    private String seatClass;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}