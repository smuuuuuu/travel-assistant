package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler;
import lombok.Data;

/**
 * 
 * @TableName ai_message_detail
 */
@TableName(value ="ai_message_detail")
@Data
public class AiMessageDetail implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     *
     */
    @TableField(value = "user_id")
    private Long userId;
    /**
     * 
     */
    @TableField(value = "message_id")
    private String messageId;

    /**
     * 
     */
    @TableField(value = "session_id")
    private String sessionId;

    /**
     * 
     */
    @TableField(value = "use_token")
    private Integer useToken;

    /**
     * 
     */
    @TableField(value = "processing_time")
    private Long processingTime;

    /**
     * 
     */
    @TableField(value = "is_agent")
    private Integer isAgent;

    /**
     * 
     */
    @TableField(value = "tool_usage",typeHandler = Fastjson2TypeHandler.class)
    private List<String> toolUsage;

    @TableField(value = "is_del")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}