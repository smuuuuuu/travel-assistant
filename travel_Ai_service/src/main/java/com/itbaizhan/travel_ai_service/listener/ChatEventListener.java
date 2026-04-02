package com.itbaizhan.travel_ai_service.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itbaizhan.travel_ai_service.mapper.AiConversationsMapper;
import com.itbaizhan.travel_ai_service.mapper.AiMessagesMapper;
import com.itbaizhan.travelcommon.info.AiMessageEvent;
import com.itbaizhan.travelcommon.pojo.AiConversations;
import com.itbaizhan.travelcommon.pojo.AiMessages;
import com.itbaizhan.travelcommon.service.AiAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class ChatEventListener {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Async // 关键：让这个方法在单独线程池跑，不阻塞主线程
    @EventListener
    public void handleChatCompleted(AiMessageEvent aiMessageEvent) {
        log.info("开始异步归档聊天记录: sessionId={}", aiMessageEvent.getSessionId());

        try {
            AiMessages aiMessages = new AiMessages();
            aiMessages.setMessageId(aiMessageEvent.getMsgUid());
            aiMessages.setSessionId(aiMessageEvent.getSessionId());
            aiMessages.setQuestion(aiMessageEvent.getQuestion());
            aiMessages.setAnswer(aiMessageEvent.getAnswer());
            aiMessages.setAnswerType(0);
            aiMessages.setIsAgent(0);
            aiMessages.setQuestionType(aiMessageEvent.getQuestionType());
            if(aiMessageEvent.getUserId() != null) {
                aiMessages.setUserId(aiMessageEvent.getUserId());
            }
            if (aiMessageEvent.getTokensUsed() != null) {
                aiMessages.setTokensUsed(aiMessageEvent.getTokensUsed());
            }
            if (aiMessageEvent.getProcessingTime() != null) {
                aiMessages.setProcessingTime(aiMessageEvent.getProcessingTime());
            }
            aiMessages.setUserId(aiMessageEvent.getUserId());
            aiMessages.setCreatedAt(LocalDateTime.now());
            aiAssistantService.insertMessage(aiMessages);
            log.info("聊天记录归档成功");
        } catch (Exception e) {
            log.error("聊天记录归档失败", e);
        }
    }
}
