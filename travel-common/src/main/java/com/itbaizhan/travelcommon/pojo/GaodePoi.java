package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName gaoDe_poi
 */
@TableName(value ="gaoDe_poi")
@Data
public class GaodePoi implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 
     */
    @TableField(value = "poi_name")
    private String poiName;

    /**
     * 
     */
    @TableField(value = "poi_code")
    private String poiCode;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}