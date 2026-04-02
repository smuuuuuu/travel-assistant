package com.itbaizhan.travel_ai_service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travel_ai_service.mapper.AiConversationsMapper;
import com.itbaizhan.travel_ai_service.mapper.AiMessageDetailMapper;
import com.itbaizhan.travel_ai_service.mapper.AiMessagesMapper;
import com.itbaizhan.travelcommon.AiSessionDto.SessionRequest;
import com.itbaizhan.travelcommon.pojo.AiConversations;
import com.itbaizhan.travelcommon.pojo.AiMessageDetail;
import com.itbaizhan.travelcommon.pojo.AiMessages;
import com.itbaizhan.travelcommon.service.AiAssistantService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class AiAssistantServiceImpl implements AiAssistantService {
    @Autowired
    private AiConversationsMapper aiConversationsMapper;
    @Autowired
    private AiMessagesMapper aiMessagesMapper;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private AiMessageDetailMapper aiMessageDetailMapper;

   // private static final String DEFAULT_PROMPT = "你好，介绍下你自己！";
    @Override
    public AiConversations createSession(SessionRequest sessionRequest,Long userId) {
        AiConversations aiConversations = new AiConversations();
        aiConversations.setTitle(sessionRequest.getTitle());
        aiConversations.setUserId(userId);
        aiConversations.setSessionId(IdWorker.getIdStr());
        aiConversations.setContextType(sessionRequest.getContentType());
        aiConversations.setSessionId(UUID.randomUUID().toString());
        aiConversations.setStatus(1);
        aiConversations.setCreatedAt(LocalDateTime.now());
        aiConversations.setUpdatedAt(LocalDateTime.now());
        aiConversationsMapper.insert(aiConversations);

        return aiConversations;
    }

    @Override
    public boolean existConversation(String conversationId) {
        QueryWrapper<AiConversations> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", conversationId);
        return aiConversationsMapper.exists(queryWrapper);
    }

    @Override
    public IPage<AiConversations> getSessions(Integer page, Integer size, Long userId, Integer contextType) {
        IPage<AiConversations> aiConversationsIPage = new Page<>(page, size);
        QueryWrapper<AiConversations> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("context_type", contextType);
        queryWrapper.orderByDesc("updated_at");
        return aiConversationsMapper.selectPage(aiConversationsIPage, queryWrapper);
    }

    @Override
    public IPage<AiMessages> getHistoryMessages(String sessionId, Integer page, Integer size) {
        IPage<AiMessages> pageParam = new Page<>(page, size);
        QueryWrapper<AiMessages> msgQuery = new QueryWrapper<>();
        msgQuery.eq("session_id", sessionId);
        // 按时间倒序查询，获取最新的消息
        msgQuery.orderByDesc("created_at");
        return aiMessagesMapper.selectPage(pageParam, msgQuery);
    }

    @Override
    public void updateSession(String sessionId, String title) {
        AiConversations conversation = new AiConversations();
        conversation.setTitle(title);
        conversation.setUpdatedAt(LocalDateTime.now());
        QueryWrapper<AiConversations> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId);
        aiConversationsMapper.update(conversation, queryWrapper);
    }

    @Override
    public void deleteSession(String sessionId) {
        QueryWrapper<AiConversations> convQuery = new QueryWrapper<>();
        convQuery.eq("session_id", sessionId);
        aiConversationsMapper.delete(convQuery);

        QueryWrapper<AiMessages> msgQuery = new QueryWrapper<>();
        msgQuery.eq("session_id", sessionId);
        aiMessagesMapper.delete(msgQuery);
        
        // 清除聊天记忆
        chatMemory.clear(sessionId);
    }
    public void insertMessage(AiMessages aiMessages) {
        AiMessageDetail aiMessageDetail = getAiMessageDetail(aiMessages);
        aiMessagesMapper.insert(aiMessages);
        aiMessageDetailMapper.insert(aiMessageDetail);
        AiConversations conversation = new AiConversations();
        conversation.setUpdatedAt(LocalDateTime.now());
        QueryWrapper<AiConversations> conversationsQueryWrapper = new QueryWrapper<>();
        conversationsQueryWrapper.eq("session_id", aiMessages.getSessionId());
        aiConversationsMapper.update(conversation, conversationsQueryWrapper);
    }

    private AiMessageDetail getAiMessageDetail(AiMessages aiMessages) {
        AiMessageDetail aiMessageDetail = new AiMessageDetail();
        aiMessageDetail.setMessageId(aiMessages.getMessageId());
        aiMessageDetail.setSessionId(aiMessages.getSessionId());
        if(aiMessages.getToolUsage() != null){
            aiMessageDetail.setToolUsage(aiMessages.getToolUsage());
        }
        aiMessageDetail.setUserId(aiMessages.getUserId());
        aiMessageDetail.setIsAgent(aiMessages.getIsAgent());
        aiMessageDetail.setUseToken(aiMessages.getTokensUsed());
        aiMessageDetail.setProcessingTime(aiMessages.getProcessingTime());
        return aiMessageDetail;
    }

    @Override
    public void deleteMessage(String messageId) {
        // 先查询消息所属的会话ID
        QueryWrapper<AiMessages> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("message_id", messageId);
        AiMessages aiMessages = aiMessagesMapper.selectOne(queryWrapper);
        
        if (aiMessages == null) {
            return;
        }
        
        String sessionId = aiMessages.getSessionId();
        // 删除数据库中的消息
        aiMessagesMapper.delete(queryWrapper);
        aiMessagesMapper.deleteByAiMessagesDetail(messageId);

        AiConversations conversation = new AiConversations();
        conversation.setUpdatedAt(LocalDateTime.now());
        QueryWrapper<AiConversations> conversationsQueryWrapper = new QueryWrapper<>();
        conversationsQueryWrapper.eq("session_id", sessionId);
        aiConversationsMapper.update(conversation, conversationsQueryWrapper);
        
        // 更新聊天记忆：先清除，再从数据库加载最近的 N 条记录
        chatMemory.clear(sessionId);
        
        // 获取该会话最近的 10 条消息作为上下文
        IPage<AiMessages> pageParam = new Page<>(1, 10);
        QueryWrapper<AiMessages> historyQuery = new QueryWrapper<>();
        historyQuery.eq("session_id", sessionId);
        historyQuery.orderByDesc("created_at"); // 最新在前
        
        IPage<AiMessages> historyPage = aiMessagesMapper.selectPage(pageParam, historyQuery);
        List<AiMessages> records = historyPage.getRecords();
        
        // 倒序排列，使其变为时间正序（旧 -> 新）
        Collections.reverse(records);
        
        List<Message> messages = new ArrayList<>();
        for (AiMessages record : records) {
            if (record.getQuestion() != null) {
                messages.add(new UserMessage(record.getQuestion()));
            }
            if (record.getAnswer() != null) {
                messages.add(new AssistantMessage(record.getAnswer()));
            }
        }
        
        if (!messages.isEmpty()) {
            chatMemory.add(sessionId, messages);
        }
    }
}
