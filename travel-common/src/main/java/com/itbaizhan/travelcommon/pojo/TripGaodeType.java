package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName trip_gaoDe_type
 */
@TableName(value ="trip_gaoDe_type")
@Data
public class TripGaodeType implements Serializable {
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
    @TableField(value = "name")
    private String name;
    @TableField(value = "ai_module_id")
    private Long aiModuleId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}