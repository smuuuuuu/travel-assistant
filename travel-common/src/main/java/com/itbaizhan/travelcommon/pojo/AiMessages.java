package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler;
import lombok.Data;

/**
 * AI对话消息表
 * @TableName ai_messages
 */
@TableName(value ="ai_messages")
@Data
public class AiMessages implements Serializable {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "message_id")
    private String messageId;
    /**
     * 会话ID
     */
    @TableField(value = "session_id")
    private String sessionId;

    @TableField(value = "think")
    private String think;
    /**
     * 问题类型  '1 user', '2 assistant', '3 system'
     * '0 text', ' 1 image', '2 audio', 3 'file'
     */
    @TableField(value = "question_type")
    private Integer questionType;

    /**
     * 问题
     */
    @TableField(value = "question")
    private String question;

    /**
     * 回答类型 '0 text', ' 1 image', '2 audio', 3 'file'
     */
    @TableField(value = "answer_type")
    private Integer answerType;

    /**
     * 回答
     */
    @TableField(value = "answer")
    private String answer;

    /**
     * agent工具使用
     */
    @TableField(exist = false)
    //@TableField(value = "tool_usage",typeHandler = Fastjson2TypeHandler.class)
    private List<String> toolUsage;

    /**
     * 使用的token数量
     */
    //@TableField(value = "tokens_used")
    @TableField(exist = false)
    private Integer tokensUsed;

    /**
     * 处理时间(毫秒)
     */
    //@TableField(value = "processing_time")
    @TableField(exist = false)
    private Long processingTime;

    /**
     * 置信度分数
     */
    @TableField(value = "confidence_score")
    private BigDecimal confidenceScore;

    /**
     * 用户反馈
     */
    @TableField(value = "feedback")
    private Object feedback;

    //@TableField(value = "is_agent")
    @TableField(exist = false)
    private Integer isAgent;

    @TableField(exist = false)
    private Long userId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}