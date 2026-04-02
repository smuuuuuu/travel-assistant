package com.itbaizhan.travelmanager.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AiPromptMongoRepository extends MongoRepository<AiPromptDocument, String> {

    Optional<AiPromptDocument> findByScene(String scene);
}
