package com.itbaizhan.travel_ai_service.agent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.itbaizhan.travel_ai_service.agent.record.MessageRecord;
import com.itbaizhan.travel_ai_service.service.FaqServiceImpl;
import com.itbaizhan.travel_ai_service.util.PromptUtil;
import com.itbaizhan.travelcommon.pojo.AiMessages;
import com.itbaizhan.travelcommon.pojo.TravelFaq;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.AiAssistantService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class TripAgent extends ToolCallAgent{
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    @Qualifier("agentTools")
    private ToolCallback[] agentTools;
    @Value("${prompt.agent-system}")
    private String agentSystem;
    @Value("${redis.agent-next-step}")
    private String agentNextStep;
    @Resource
    private ChatModel chatModel;
    @Autowired
    private AiAssistantService aiAssistantService;
    @Autowired
    private FaqServiceImpl faqService;
    @Autowired
    private ChatMemory chatMemory;


    @PostConstruct
    public void postConstruct() {
        this.setToolCallbacks(agentTools);
        this.setSystemPrompt(agentSystem);
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();
        this.setChatClient(chatClient);
    }

    public SseEmitter stream(String conversationId, String userMessage,Long userId){
        return this.runStream(conversationId,userMessage,userId);
    }

    @Override
    protected String getNextStepPrompt() {
        String nextStepPrompt = (String)redisTemplate.opsForValue().get(agentNextStep);
        return PromptUtil.renderPromptTemplate(nextStepPrompt,"total",this.getMaxSteps());
    }

    @Override
    protected void persistConversation(String conversationId, MessageRecord messageRecord) {
        if (messageRecord.getMessages() == null || messageRecord.getMessages().isEmpty()) {
            return;
        }
        ChatResponse chatResponse = messageRecord.getChatResponse();
        if(!this.doTerminate(chatResponse)){
            messageRecord.getStreamConsumer().accept("生成出错请重试");
            return;
        }
        Message userMessage = messageRecord.getMessages().get(0);
        String question = userMessage.getText();
        Message answer = messageRecord.getMessages().get(messageRecord.getMessages().size() - 2);
        if(!faqService.existFaq(question)){
            TravelFaq travelFaq = new TravelFaq();
            travelFaq.setQuestion(question);
            travelFaq.setAnswer(answer.getText());
            travelFaq.setCity(messageRecord.getCity());
            faqService.createFaq(travelFaq);
        }
        AiMessages aiMessage = new AiMessages();
        aiMessage.setMessageId(UUID.randomUUID().toString());
        aiMessage.setSessionId(conversationId);
        aiMessage.setCreatedAt(LocalDateTime.now());
        aiMessage.setQuestionType(2);
        aiMessage.setQuestion(question);
        aiMessage.setAnswer(answer.getText());
        aiMessage.setIsAgent(1);
        aiMessage.setAnswerType(0);
        aiMessage.setThink(messageRecord.getThink());
        aiMessage.setTokensUsed(messageRecord.getToken());
        aiMessage.setProcessingTime(messageRecord.getProcessingTime());
        aiMessage.setToolUsage(messageRecord.getToolUsage());
        aiMessage.setUserId(messageRecord.getUserId());
        aiAssistantService.insertMessage(aiMessage);

        chatMemory.add(conversationId,List.of(userMessage,answer));
    }
    /**
     * 将文本分块输出到流式通道
     */
    private void streamText(MessageRecord messageRecord, String text) {
        if (messageRecord.getStreamConsumer() == null || StrUtil.isBlank(text)) {
            return;
        }
        int chunkSize = 50;
        int length = text.length();
        for (int i = 0; i < length; i += chunkSize) {
            int end = Math.min(i + chunkSize, length);
            messageRecord.getStreamConsumer().accept(text.substring(i, end));
        }
    }
}
