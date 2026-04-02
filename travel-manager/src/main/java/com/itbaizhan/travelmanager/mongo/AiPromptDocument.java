package com.itbaizhan.travelmanager.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 与 travel_AI_trip 集合 {@code ai_prompts} 字段保持一致。
 */
@Data
@Document(collection = "ai_prompts")
public class AiPromptDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String scene;

    private String content;

    private String version;

    private String description;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
