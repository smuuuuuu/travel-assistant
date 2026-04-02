package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 饮食偏好表
 * @TableName food_preferences
 */
@TableName(value ="food_preferences")
@Data
public class FoodPreferences implements Serializable {
    /**
     * 饮食偏好ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 饮食偏好名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 偏好描述
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