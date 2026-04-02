package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("city_direct_code")
public class CityDirectCode  implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String city;
    private String code;

    public CityDirectCode(String city, String code) {
        this.city = city;
        this.code = code;
    }
}
