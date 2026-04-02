package com.itbaizhan.travelcommon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itbaizhan.travelcommon.AiSessionDto.SessionRequest;
import com.itbaizhan.travelcommon.pojo.AiConversations;
import com.itbaizhan.travelcommon.pojo.AiMessages;

import java.util.List;

public interface AiAssistantService {

    AiConversations createSession(SessionRequest sessionRequest, Long userId);

    boolean existConversation(String conversationId);

    IPage<AiConversations> getSessions(Integer page, Integer size, Long userId, Integer contextType);

    IPage<AiMessages> getHistoryMessages(String sessionId, Integer page, Integer size);

    void updateSession(String sessionId, String title);

    void deleteSession(String sessionId);

    void deleteMessage(String messageId);

    void insertMessage(AiMessages aiMessages);
}
