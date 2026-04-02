package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 交通方式偏好表
 * @TableName transportation_types
 */
@TableName(value ="transportation_types")
@Data
public class TransportationTypes implements Serializable {
    /**
     * 交通方式ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 交通方式名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 方式描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 是否启用
     */
    @TableField(value = "is_active")
    private Integer isActive;

    /**
     * 排序顺序
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private Date updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}