package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 行程计划表
 * @TableName trips
 */
@TableName(value ="trips", autoResultMap = true)
@Data
public class Trips implements Serializable {
    /**
     * 行程ID
     */
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    @TableField(value = "trip_id")
    private String tripId;
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 行程标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 行程描述
     */
    @TableField(value = "description")
    private String description;

    private String departure;
    /**
     * 主要目的地
     */
    @TableField(value = "destination")
    private String destination;

    /**
     * 开始日期
     */
    @TableField(value = "start_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startDate;

    /**
     * 结束日期
     */
    @TableField(value = "end_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endDate;

    /**
     * 总天数
     */
    @TableField(value = "total_days")
    private Integer totalDays;

    /**
     * 旅行人数
     */
    @TableField(value = "traveler_count")
    private Integer travelerCount;
    /**
     * 预估总价
     */
    @TableField(value = "total_price")
    private BigDecimal totalPrice;
    /**
     * 总预算
     */
    @TableField(value = "budget_total")
    private BigDecimal budgetTotal;

    /**
     * 已使用预算
     */
    @TableField(value = "budget_used")
    private BigDecimal budgetUsed;

    /**
     * 旅行风格
     */
    @TableField(value = "travel_style",typeHandler = Fastjson2TypeHandler.class)
    private List<String> travelStyle;

    /*@TableField(value = "transportation",typeHandler = Fastjson2TypeHandler.class)
    private List<Transportation> transportation;
    @TableField(value = "hotels",typeHandler = Fastjson2TypeHandler.class)
    private List<Hotel> hotels;
    @TableField(value = "tickets",typeHandler = Fastjson2TypeHandler.class)
    private List<Ticket> tickets;*/
    /**
     * 行程状态
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 完成状态  1草稿 2修改 3已完成
     */
    @TableField(value = "complete_status")
    private Integer completeStatus;
    /**
     * 是否公开
     */
    @TableField(value = "is_public")
    private Integer isPublic;

    /**
     * 封面图片URL
     */
    @TableField(value = "cover_image_url")
    private String coverImageUrl;

    /**
     * 备注信息
     */
    @TableField(value = "notes")
    private String notes;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;


    @TableField(exist = false)
    private List<TripSchedules> tripSchedules;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}