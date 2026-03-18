package com.itbaizhan.travel_trip_service.repository.mongo;

import com.itbaizhan.travel_trip_service.entity.mongo.AiPrompt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPromptRepository extends MongoRepository<AiPrompt, String> {

    /**
     * 根据场景标识查找生效的 Prompt
     */
    /*Optional<AiPrompt> findBySceneAndIsActiveTrue(String scene);

    Optional<AiPrompt> findAiPromptById(String id);*/

    /**
     * 根据场景标识查找 (无论是否生效)
     */
    Optional<AiPrompt> findByScene(String scene);
}
