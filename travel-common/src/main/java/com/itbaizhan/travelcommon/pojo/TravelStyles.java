package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 旅行风格偏好表
 * @TableName travel_styles
 */
@TableName(value ="travel_styles")
@Data
public class TravelStyles implements Serializable {
    /**
     * 风格ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 风格名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 风格描述
     */
    @TableField(value = "description")
    private String description;

    @TableField(value = "use_count")
    private Integer useCount;
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