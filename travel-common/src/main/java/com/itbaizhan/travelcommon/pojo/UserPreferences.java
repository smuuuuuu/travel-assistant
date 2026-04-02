package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.itbaizhan.travelcommon.info.BudgetStyles;
import lombok.Data;

/**
 * 用户偏好设置表
 * @TableName user_preferences
 */
@TableName(value ="user_preferences")
@Data
public class UserPreferences implements Serializable {
    /**
     * 偏好ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 旅行风格偏好
     */
    @TableField(value = "travel_style", typeHandler = FastjsonTypeHandler.class)
    private List<String> travelStyle;

    /**
     * 预算范围偏好
     */
    @TableField(value = "budget_range", typeHandler = FastjsonTypeHandler.class)
    private BudgetStyles budgetRange;

    /**
     * 住宿类型偏好
     */
    @TableField(value = "accommodation_type", typeHandler = FastjsonTypeHandler.class)
    private List<String> accommodationType;

    /**
     * 交通方式偏好
     */
    @TableField(value = "transportation_type", typeHandler = FastjsonTypeHandler.class)
    private List<String> transportationType;

    /**
     * 饮食偏好
     */
    @TableField(value = "food_preferences", typeHandler = FastjsonTypeHandler.class)
    private List<String> foodPreferences;

    /**
     * 活动偏好
     */
    @TableField(value = "activity_preferences", typeHandler = FastjsonTypeHandler.class)
    private List<String> activityPreferences;

    /**
     * 语言偏好
     */
    @TableField(value = "language_preference")
    private String languagePreference;

    /**
     * 货币偏好
     */
    @TableField(value = "currency_preference")
    private String currencyPreference;

    /**
     * 通知设置
     */
    @TableField(value = "notification_settings")
    private Object notificationSettings;

    /**
     * 隐私设置
     */
    @TableField(value = "privacy_settings")
    private Object privacySettings;

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