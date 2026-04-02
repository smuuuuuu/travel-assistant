package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 预算范围偏好表
 * @TableName budget_ranges
 */
@TableName(value ="budget_ranges")
@Data
public class BudgetRanges implements Serializable {
    /**
     * 预算范围ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 预算范围名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 最小金额
     */
    @TableField(value = "min_amount")
    private BigDecimal minAmount;

    /**
     * 最大金额
     */
    @TableField(value = "max_amount")
    private BigDecimal maxAmount;

    /**
     * 货币类型
     */
    @TableField(value = "currency")
    private String currency;

    /**
     * 描述
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