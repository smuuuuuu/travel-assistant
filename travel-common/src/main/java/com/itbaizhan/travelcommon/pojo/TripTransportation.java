package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
@TableName("trip_transportation") // 对应数据库表名
public class TripTransportation implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String type; // FLIGHT, TRAIN, BUS, CAR
    // 避免使用 SQL 关键字 'from' 和 'to'
    @TableField(value = "departure_location")
    private String departureLocation;
    @TableField(value = "arrival_location")
    private String arrivalLocation;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "departure_time")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "arrival_time")
    private LocalDateTime arrivalTime;
    @TableField(value = "train_number")
    private String trainNumber; // 航班号或车次号

    @TableField(value = "booking_status")
    private String bookingStatus; // PENDING, CONFIRMED, CANCELLED
    private String description;   // 备注
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_at")
    private LocalDateTime createAt;

    @TableField(value = "ref_count")
    private Integer refCount;

    @TableField(value = "dedupKey")
    private String dedupKey;
}
