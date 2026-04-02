package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 行政区域地州市信息表
 * @TableName bz_city
 */
@TableName(value ="bz_city")
@Data
public class BzCity implements Serializable {
    /**
     * 城市ID
     */
    @TableField(value = "id")
    private String id;

    /**
     * 城市名称
     */
    @TableField(value = "city")
    private String city;

    /**
     * 省份ID
     */
    @TableField(value = "provinceid")
    private String provinceId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}