package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 
 * @TableName trip_backup
 */
@TableName(value ="trip_backup")
@Data
public class TripBackup implements Serializable {
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
    @TableField(value = "trip_id")
    private String tripId;

    /**
     * 1=json_utf8, 2=gzip_json_utf8
     */
    @TableField(value = "content_format")
    private Integer contentFormat;

    @TableField(value = "object_key")
    private String objectKey;
    /**
     * 可选：内容校验，用于排查/去重
     */
    @TableField(value = "content_sha256")
    private String contentSha256;

    /**
     * 
     */
    @TableField(value = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 到期时间，用于清理
     */
    @TableField(value = "expire_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireAt;

    /**
     * JSON UTF-8 bytes or GZIP(JSON UTF-8 bytes)
     */
    @TableField(value = "content")
    private byte[] content;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}