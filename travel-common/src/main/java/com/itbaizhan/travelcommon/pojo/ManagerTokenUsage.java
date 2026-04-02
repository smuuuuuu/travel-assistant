package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("manager_token_usage")
public class ManagerTokenUsage implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("manager_id")
    private Long managerId;

    @TableField("manager_name")
    private String managerName;

    @TableField("biz_scene")
    private String bizScene;

    @TableField("model")
    private String model;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("llm_call_count")
    private Integer llmCallCount;

    @TableField("detail")
    private String detail;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
