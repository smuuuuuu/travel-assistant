package com.itbaizhan.travel_ai_service.agent;


import cn.hutool.core.util.StrUtil;
import com.itbaizhan.travel_ai_service.agent.model.State;
import com.itbaizhan.travel_ai_service.agent.record.MessageRecord;
import com.itbaizhan.travel_ai_service.util.PromptUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Data
@Slf4j
public abstract class BaseAgent {
    private String systemPrompt;
    private ChatClient chatClient;
    private int currentStep = 0;
    private int maxSteps = 5;
    private String currentStepPrompt = "The current step is: {{current}} .";


    protected SseEmitter runStream(String conversationId,String userMessage,Long userId){
        SseEmitter sseEmitter = new SseEmitter(600000L * 2);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
            try {
                sseEmitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException e) {
                log.warn("SSE heartbeat failed: {}", e.getMessage());
            }
        }, 0, 15, TimeUnit.SECONDS);
        CompletableFuture.runAsync(() -> {
            List<Message> messageList = new ArrayList<>();
            messageList.add(new UserMessage(userMessage));
            int currentStep = 0;
            MessageRecord messageRecord = new MessageRecord();
            messageRecord.setMessages(messageList);
            messageRecord.setRecordMessage(new HashMap<>());
            messageRecord.setToolUsage(new ArrayList<>());
            messageRecord.setToken(0);
            messageRecord.setUserId(userId);
            messageRecord.setProcessingTime(0L);
            messageRecord.setState(State.IDLE);
            messageRecord.setStreamConsumer(part -> send(sseEmitter, "content", part));
            messageRecord.setIndex(0);
            long currentTimeMillis = System.currentTimeMillis();
            StringBuilder think = new StringBuilder();
            String nextStep = this.getNextStepPrompt();
            try {
                for(int i=0; i < maxSteps && messageRecord.getState() != State.FINISHED; i++){
                    currentStep = i + 1;
                    messageRecord.getMessages().add(
                            new UserMessage(PromptUtil.renderPromptTemplate(
                                    nextStep,"current",currentStep))
                    );
                    String stepResult = step(conversationId,messageRecord);
                    AssistantMessage assistantMessage = Objects.requireNonNull(messageRecord.getChatResponse()).getResult().getOutput();
                    String result = assistantMessage.getText();
                    //results.add(result);
                    if(messageRecord.getState() == State.FINISHED){
                        messageRecord.setThink(think.toString());
                        send(sseEmitter,"done",result);
                    }else {
                        think.append(result);
                        send(sseEmitter,"progress",result);
                    }
                    nextStep = this.getCurrentStepPrompt();
                }
                long completion = System.currentTimeMillis();
                messageRecord.setProcessingTime(completion - currentTimeMillis);
                if(currentStep >= maxSteps && messageRecord.getState() != State.FINISHED){
                    if(this.doTerminate(messageRecord.getChatResponse())) {
                        messageRecord.setState(State.FINISHED);
                        //results.add("Terminated: doTerminate tool called");
                        send(sseEmitter, "done", "执行结束：调用了 doTerminate 工具");
                    }else {
                        send(sseEmitter,"done","执行结束：达到最大步骤，但未调用 doTerminate 工具");
                    }
                }
                sseEmitter.complete();
                try {
                    persistConversation(conversationId, messageRecord);
                } catch (Exception e) {
                    log.error("error persisting conversation", e);
                }
            } catch (Exception e) {
                log.error("error executing agent", e);
                sseEmitter.completeWithError(e);
            } finally {
                stopHeartbeat(heartbeat, scheduler);
            }
        });
        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            log.warn("SSE connection timeout");
            stopHeartbeat(heartbeat, scheduler);
        });
        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            log.info("SSE connection completed");
            stopHeartbeat(heartbeat, scheduler);
        });
        sseEmitter.onError(throwable -> {
            if (log.isErrorEnabled()) {
                log.error("SSE error", throwable.getMessage());
            }
            stopHeartbeat(heartbeat, scheduler);
        });
        return sseEmitter;
    }

    public abstract String step(String conversationId,MessageRecord record);

    protected String getNextStepPrompt() {
        return null;
    }
    protected void persistConversation(String conversationId, MessageRecord messageRecord) {
    }
    public boolean doTerminate(ChatResponse chatResponse){
        if(chatResponse == null || !chatResponse.hasToolCalls()) {
            return false;
        }
        return chatResponse
                .getResult().getOutput().getToolCalls().stream()
                .anyMatch(toolCall -> StrUtil.equals("doTerminate", toolCall.name()));
    }

    public void send(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            try {
                emitter.send("执行错误：" + e.getMessage());
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    private void stopHeartbeat(ScheduledFuture<?> heartbeat, ScheduledExecutorService scheduler) {
        if (heartbeat != null) {
            heartbeat.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
