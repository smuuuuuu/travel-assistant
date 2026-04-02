package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 住宿类型偏好表
 * @TableName accommodation_types
 */
@TableName(value ="accommodation_types")
@Data
public class AccommodationTypes implements Serializable {
    /**
     * 住宿类型ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 住宿类型名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 类型描述
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