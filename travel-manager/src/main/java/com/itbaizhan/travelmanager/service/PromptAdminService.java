package com.itbaizhan.travelmanager.service;

import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelmanager.mongo.AiPromptDocument;
import com.itbaizhan.travelmanager.mongo.AiPromptMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromptAdminService {

    private final AiPromptMongoRepository aiPromptMongoRepository;
    private final PromptCacheInvalidator promptCacheInvalidator;

    public AiPromptDocument getPrompt(String promptId) {
        Optional<AiPromptDocument> aiPromptDocument = aiPromptMongoRepository.findById(promptId);
        if(aiPromptDocument.isEmpty()) {
            aiPromptDocument = aiPromptMongoRepository.findByScene(promptId);
        }
        if(aiPromptDocument.isPresent()) {
            return aiPromptDocument.get();
        }
        throw new BusException(CodeEnum.PARAMETER_ERROR.getCode(), "prompt not found");
    }
    @Transactional
    public String savePrompt(AiPromptDocument promptDocument) {
        promptDocument.setId(null);
        promptDocument.setVersion("v1.0.0");
        promptDocument.setCreatedAt(LocalDateTime.now());
        promptDocument.setUpdatedAt(LocalDateTime.now());
        AiPromptDocument saved = aiPromptMongoRepository.save(promptDocument);
        return saved.getId();
    }
    @Transactional
    public void update(String scene, String content, String description) {
        if (!StringUtils.hasText(scene)) {
            throw new BusException(CodeEnum.PARAMETER_ERROR.getCode(), "scene required");
        }
        Optional<AiPromptDocument> aiPromptDocument = aiPromptMongoRepository.findById(scene);
        if(aiPromptDocument.isPresent()) {
            AiPromptDocument doc = aiPromptDocument.get();
            doc.setContent(content);
            doc.setDescription(description);
            doc.setUpdatedAt(LocalDateTime.now());
            aiPromptMongoRepository.save(doc);
            promptCacheInvalidator.updatePrompt(scene);
        }
    }
    @Transactional
    public void deleteByScene(String scene) {
        aiPromptMongoRepository.findById(scene).ifPresent(aiPromptMongoRepository::delete);
    }
}
