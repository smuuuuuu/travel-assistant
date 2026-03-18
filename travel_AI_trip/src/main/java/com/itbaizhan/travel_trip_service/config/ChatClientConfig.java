package com.itbaizhan.travel_trip_service.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.itbaizhan.travel_trip_service.tool.CodeTools;


@Configuration
public class ChatClientConfig {
    @Value("${ai.prompt.system}")
    private String system;
    @Autowired
    private McpMethodProperties mcpMethodProperties;

    @Bean
    //public ChatClient chatClient(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient
                .builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem(system)
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withTopP(0.7)
                                .build()
                )
                //.defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    @Bean
    public ToolCallbackProvider codeToolCallbackProvider(CodeTools codeTools) {
        return MethodToolCallbackProvider.builder().toolObjects(codeTools).build();
    }

    @Bean(name = "tripTools")
    public ToolCallbackProvider toolCallbackProvider(
            @Qualifier("mcpAsyncToolCallbacks") ToolCallbackProvider mcpToolProvider,
            @Qualifier("codeToolCallbackProvider") ToolCallbackProvider codeToolCallbackProvider
    ) {
        return () -> {
            List<String> poiTools = mcpMethodProperties.getPoiTools();
            List<ToolCallback> callbacks = new ArrayList<>();
            ToolCallback[] mcpCallbacks = mcpToolProvider.getToolCallbacks();
            if (mcpCallbacks != null && mcpCallbacks.length > 0) {
                for (ToolCallback mcpCallback : mcpCallbacks) {
                    if(poiTools.contains(mcpCallback.getToolDefinition().name())){
                        callbacks.add(mcpCallback);
                    }
                }
                //callbacks.addAll(Arrays.asList(mcpCallbacks));
            }
            ToolCallback[] codeCallbacks = codeToolCallbackProvider.getToolCallbacks();
            if (codeCallbacks != null && codeCallbacks.length > 0) {
                callbacks.addAll(Arrays.asList(codeCallbacks));
            }
            return callbacks.toArray(new ToolCallback[0]);
        };
    }


    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setReadTimeout(300000); // 5 minutes
                    setConnectTimeout(300000); // 5 minutes
                }});
    }
}
