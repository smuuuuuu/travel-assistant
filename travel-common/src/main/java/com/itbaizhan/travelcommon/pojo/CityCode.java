package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("city_code")
public class CityCode implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String city;
    private String code;
}
