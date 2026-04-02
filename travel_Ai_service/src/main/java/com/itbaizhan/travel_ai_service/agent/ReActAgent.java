package com.itbaizhan.travel_ai_service.agent;

import com.itbaizhan.travel_ai_service.agent.model.State;
import com.itbaizhan.travel_ai_service.agent.record.MessageRecord;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
public abstract class ReActAgent extends BaseAgent {

    public abstract boolean think(String conversationId,MessageRecord messageRecord);

    public abstract String act(MessageRecord messageRecord);

    @Override
    public String step(String conversationId, MessageRecord messageRecord) {
        try{
            boolean think = think(conversationId,messageRecord);
            if(think){
                return act(messageRecord);
            }
            messageRecord.setState(State.FINISHED);
            return "思考完成 - 无需行动";
        }catch (Exception e){}
        return "";
    }
}
