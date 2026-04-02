package com.itbaizhan.travelcommon.info;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetStyles implements Serializable {
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
}
