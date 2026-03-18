package com.itbaizhan.travel_trip_service.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI Prompt 实体类 (MongoDB)
 */
@Data
@Document(collection = "ai_prompts")
public class AiPrompt {

    @Id
    private String id;

    /**
     * 场景标识 (如: trip_modify, generate_plan)
     * 唯一索引，方便根据场景快速查询
     */
    @Indexed(unique = true)
    private String scene;

    /**
     * Prompt 正文内容
     */
    private String content;

    /**
     * 版本号 (如: v1.0.0)
     */
    private String version;

    /**
     * 描述说明
     */
    private String description;

    /**
     * 动态参数 (可选，预留扩展)
     * 例如: temperature, max_tokens, stop_sequences 等
     */
    private Map<String, Object> parameters;

    /**
     * 是否启用
     */
    @Field("is_active")
    private Boolean isActive;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
