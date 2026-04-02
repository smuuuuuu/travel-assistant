package com.itbaizhan.travel_ai_service.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.function.Consumer;

public class TravelRagCustomAdvisorFactory {

    public static Advisor createTravelRagCustomAdvisor(VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(buildRetriever(vectorStore, null))
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }

    /**
     * 创建带检索回调的 RAG Advisor
     */
    public static Advisor createTravelRagCustomAdvisor(VectorStore vectorStore, Consumer<List<Document>> onRetrieved) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(buildRetriever(vectorStore, onRetrieved))
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }

    /**
     * 构建可回调的文档检索器
     */
    private static DocumentRetriever buildRetriever(VectorStore vectorStore, Consumer<List<Document>> onRetrieved) {
        VectorStoreDocumentRetriever delegate = VectorStoreDocumentRetriever.builder()
                .similarityThreshold(0.50)
                .vectorStore(vectorStore)
                .topK(3)
                .build();
        return query -> {
            List<Document> documents = delegate.retrieve(query);
            if (onRetrieved != null && documents != null && !documents.isEmpty()) {
                onRetrieved.accept(documents);
            }
            return documents;
        };
    }
}
