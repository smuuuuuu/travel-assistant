package com.itbaizhan.travel_ai_service.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.itbaizhan.travel_ai_service.agent.model.State;
import com.itbaizhan.travel_ai_service.agent.record.MessageRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent{

    private ToolCallback[] toolCallbacks;
    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    private final String nextPrompt;

    public ToolCallAgent() {
        super();
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                //.withMultiModel(true)
                .withInternalToolExecutionEnabled(false)
                .build();
        nextPrompt = "The tool has been executed successfully,please proceed to the next plan.";
    }

    @Override
    public boolean think(String conversationId, MessageRecord messageRecord) {
        /*if(StrUtil.isNotBlank(this.getNextStepPrompt())){
            messageRecord.getMessages().add(new UserMessage(this.getNextStepPrompt()));
        }*/
        Prompt prompt = new Prompt(messageRecord.getMessages(), chatOptions);
        try{
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(this.getSystemPrompt())
                    .toolCallbacks(toolCallbacks)
                    .call().chatResponse();
            if (chatResponse != null) {
                Integer totalTokens = chatResponse.getMetadata().getUsage().getTotalTokens();
                messageRecord.setToken(messageRecord.getToken() + totalTokens);
            }
            messageRecord.setChatResponse(chatResponse);
            AssistantMessage assistantMessage = Objects.requireNonNull(chatResponse).getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(conversationId + "的思考：" + result);
            log.info(conversationId + "选择了 " + toolCallList.size() + " 个工具来使用");

            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> {
                        String name = toolCall.name();
                        String arguments = toolCall.arguments();
                        if(StrUtil.isNotBlank(messageRecord.getCity())){
                            if("mapsTextSearch".equals(name)){
                                JSONObject jsonObject = JSON.parseObject(arguments);
                                messageRecord.setCity(jsonObject.getString("region"));
                            }
                        }
                        return String.format("工具名称：%s，参数：%s", name, arguments);
                    })
                    .collect(Collectors.joining("\n"));

            messageRecord.getToolUsage().add(toolCallInfo.replace("{", "").replace("}", "").replace("\"", ""));
            log.info(toolCallInfo);
            //boolean doTerminate = toolCallList.stream().allMatch(toolCall -> StrUtil.equals("doTerminate", toolCall.name()));
            // 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                messageRecord.getMessages().add(assistantMessage);
                //streamText(messageRecord, result);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        }catch (Exception e){
            log.error(conversationId + "的思考过程遇到了问题：" + e.getMessage());
            messageRecord.getMessages().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act(MessageRecord messageRecord) {
        if(messageRecord.getChatResponse() == null || !messageRecord.getChatResponse().hasToolCalls()){
            return "没有工具需要调用";
        }
        Prompt prompt = new Prompt(messageRecord.getMessages(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, messageRecord.getChatResponse());
        messageRecord.setMessages(toolExecutionResult.conversationHistory());
        int index = messageRecord.getMessages().size() - 1;
        /*ToolResponseMessage message = (ToolResponseMessage)messageRecord.getMessages().get(index);
        ToolResponseMessage.ToolResponse toolResponse2 = message.getResponses().get(0);
        String string = toolResponse2.responseData();
        System.out.println(string);
        if (verifyString(toolResponse2.responseData()) && !toolResponse2.responseData().startsWith("Error")) {
            System.out.println("test");
        }*/
        ToolResponseMessage responseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        boolean getAllData = responseMessage.getResponses().stream()
                .allMatch(response -> response.name().equals("getAllData") || response.name().equals("mapsTextSearchWithGetAllData"));
        if(getAllData){
            Map<Integer, Message> recordMessage = messageRecord.getRecordMessage();
            recordMessage.forEach((key, value) -> {
                messageRecord.getMessages().set(key, value);
            });
        }else {
            boolean doTerminate = responseMessage.getResponses().stream()
                    .allMatch(response -> response.name().equals("doTerminate"));
            if(doTerminate){
                messageRecord.setState(State.FINISHED);
            }else {
                if(messageRecord.getIndex() == 0){
                    messageRecord.setIndex(index);
                }else {
                    ToolResponseMessage last = (ToolResponseMessage) messageRecord.getMessages().get(messageRecord.getIndex());
                    ToolResponseMessage.ToolResponse lastResponse = last.getResponses().get(0);
                    if (verifyString(lastResponse.responseData()) && !lastResponse.responseData().startsWith("Error")) {
                        messageRecord.getRecordMessage().put(messageRecord.getIndex(), last);
                        ToolResponseMessage.ToolResponse toolResponse1 = new
                                ToolResponseMessage.ToolResponse(lastResponse.id(), lastResponse.name(), this.nextPrompt);
                        messageRecord.getMessages().set(messageRecord.getIndex(), new ToolResponseMessage(List.of(toolResponse1)));
                        messageRecord.setIndex(index);
                    }
                }
            }
        }
        String results = responseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        //streamText(messageRecord, results);
        return results;
    }
    private boolean verifyString(String str){
        return !str.equals("null") && !str.equals("\"\"") && StringUtils.hasText(str);
    }
}
