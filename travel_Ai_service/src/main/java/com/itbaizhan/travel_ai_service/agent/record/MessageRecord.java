package com.itbaizhan.travel_ai_service.agent.record;

import com.itbaizhan.travel_ai_service.agent.model.State;
import lombok.Data;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Data
public class MessageRecord {
    List<Message> messages;
    State state;
    Long userId;
    ChatResponse chatResponse;
    List<String> toolUsage;
    Long processingTime;
    Integer token;
    String think;
    Consumer<String> streamConsumer;
    Map<Integer,Message> recordMessage;
    Integer index;
    String city;
}
