package com.itbaizhan.travel_ai_service.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import com.itbaizhan.travel_ai_service.tools.WeatherService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    private final int MAX_MESSAGES = 100;
    @Autowired
    private PromptProperties promptProperties;

    @Bean
    public MessageWindowChatMemory messageWindowChatMemory(RedissonRedisChatMemoryRepository redisChatMemoryRepository) {
        return  MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }
    @Bean
    public ChatClient chatClient(ChatModel model
            , MessageWindowChatMemory messageWindowChatMemory
                                 , ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(model)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem(promptProperties.getSystemPrompt())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withTopP(0.7)
                                .build()
                )
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }
    @Bean
    public ToolCallbackProvider toolCallbackProvider(WeatherService weatherService) {
        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    }
}
