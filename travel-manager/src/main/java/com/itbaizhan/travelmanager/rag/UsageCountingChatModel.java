package com.itbaizhan.travelmanager.rag;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.Usage;

/**
 * 包装 {@link ChatModel}，累加 {@link ChatResponse} 中的 token 用量（供 FAQ 导入等场景统计）。
 */
public final class UsageCountingChatModel implements ChatModel {

    private final ChatModel delegate;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private int llmCallCount;
    private String lastModel;

    public UsageCountingChatModel(ChatModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        ChatResponse r = delegate.call(prompt);
        llmCallCount++;
        if (r != null && r.getMetadata() != null) {
            var meta = r.getMetadata();
            String m = meta.getModel();
            if (m != null) {
                lastModel = m;
            }
            Usage u = meta.getUsage();
            if (u != null) {
                if (u.getPromptTokens() != null) {
                    promptTokens += u.getPromptTokens();
                }
                if (u.getCompletionTokens() != null) {
                    completionTokens += u.getCompletionTokens();
                }
                if (u.getTotalTokens() != null) {
                    totalTokens += u.getTotalTokens();
                }
            }
        }
        return r;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public int getLlmCallCount() {
        return llmCallCount;
    }

    public String getLastModel() {
        return lastModel;
    }
}
