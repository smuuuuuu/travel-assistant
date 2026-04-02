package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * AI对话会话表
 * @TableName ai_conversations
 */
@TableName(value ="ai_conversations")
@Data
public class AiConversations implements Serializable {
    /**
     * 会话ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 会话标识
     */
    @TableField(value = "session_id")
    private String sessionId;

    /**
     * 会话标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 对话上下文类型 ('1 general', ' 2 trip_planning', '3 travel_guide', '4 translation', '5 emergency')
     */
    @TableField(value = "context_type")
    private Integer contextType;

    /**
     * 对话语言
     */
    @TableField(value = "language")
    private String language;

    /**
     * 会话状态 0 active  1 inactive
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableField(exist = false)
    private List<AiMessages> aiMessages;
}