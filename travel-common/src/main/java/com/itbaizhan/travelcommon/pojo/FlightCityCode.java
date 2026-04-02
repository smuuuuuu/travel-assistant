package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("flight_city_code")
public class FlightCityCode  implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField(value = "city_name")
    private String cityName;
    @TableField(value = "iata_code")
    private String iataCode;
    @TableField(value = "airport_name")
    private String airportName;
}
