package com.itbaizhan.travelmanager.rag;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class KeyWordsEnricher {

    private static final String CITY_KEYWORD_PROMPT =
            "{context_str}. \n Provide 3 unique keywords and the city name that appears most frequently for this document. Present them in a comma-separated format, with the city name at the end. Keywords: ";

    @Autowired
    private ChatModel chatModel;

    public record KeywordEnrichOutcome(
            List<Document> documents,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            int llmCallCount,
            String model) {
    }

    public List<Document> enrichDocuments(List<Document> documents) {
        return enrichDocumentsWithUsage(documents).documents();
    }

    /**
     * 逐条调用 LLM 提取关键词与出现最多的城市（与 {@link #enrichCityDocuments} 同一套提示词），并汇总 token 用量。
     */
    public KeywordEnrichOutcome enrichDocumentsWithUsage(List<Document> documents) {
        UsageCountingChatModel counting = new UsageCountingChatModel(chatModel);
        for (Document document : documents) {
            enrichDocumentCityKeywords(document, counting);
        }
        return new KeywordEnrichOutcome(
                documents,
                counting.getPromptTokens(),
                counting.getCompletionTokens(),
                counting.getTotalTokens(),
                counting.getLlmCallCount(),
                counting.getLastModel());
    }

    public List<Document> enrichCityDocuments(List<Document> documents) {
        for (Document document : documents) {
            enrichDocumentCityKeywords(document, chatModel);
        }
        return documents;
    }

    /**
     * 将模型输出解析为 metadata：keywords（逗号拼接）、city（最后一项）。
     */
    private void enrichDocumentCityKeywords(Document document, ChatModel model) {
        PromptTemplate template = new PromptTemplate(CITY_KEYWORD_PROMPT);
        Prompt prompt = template.create(Map.of("context_str", document.getText()));
        String raw = model.call(prompt).getResult().getOutput().getText();
        List<String> parts = new ArrayList<>(Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList());
        if (parts.isEmpty()) {
            return;
        }
        if (parts.size() == 1) {
            document.getMetadata().put("city", parts.get(0));
            document.getMetadata().put("keywords", "");
            return;
        }
        String city = parts.remove(parts.size() - 1);
        document.getMetadata().put("city", city);
        document.getMetadata().put("keywords", String.join(",", parts));
    }
}
