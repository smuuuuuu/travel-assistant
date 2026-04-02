package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("manager_operation_log")
public class ManagerOperationLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("manager_id")
    private Long managerId;

    @TableField("manager_name")
    private String managerName;

    @TableField("module")
    private String module;

    @TableField("action")
    private String action;

    @TableField("http_method")
    private String httpMethod;

    @TableField("request_uri")
    private String requestUri;

    @TableField("query_string")
    private String queryString;

    @TableField("success")
    private Boolean success;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("error_message")
    private String errorMessage;

    @TableField("client_ip")
    private String clientIp;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
