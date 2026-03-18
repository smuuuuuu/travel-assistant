package com.itbaizhan.travel_ai_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "prompt")
public class PromptProperties {

    private String systemPrompt;

    private String imagePrompt;

    private String chat;
}
