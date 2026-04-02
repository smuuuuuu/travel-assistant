package com.itbaizhan.travelcommon.info;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AiMessageEvent extends ApplicationEvent {
    private final String sessionId;
    private final String question;
    private final String answer;
    private final String msgUid;
    private final Integer questionType;
    private final Integer tokensUsed;
    private final Long processingTime;
    private final Long userId;


    public AiMessageEvent(Object source,String sessionId, String question, String answer,String msgUid,Integer questionType) {
        super(source);
        this.sessionId = sessionId;
        this.question = question;
        this.answer = answer;
        this.msgUid = msgUid;
        this.questionType = questionType;
        this.tokensUsed = null;
        this.processingTime = null;
        this.userId = null;
    }

    /**
     * 创建包含 token 与耗时信息的消息事件
     */
    public AiMessageEvent(Object source,String sessionId, String question, String answer,String msgUid,Integer questionType,Integer tokensUsed,Long processingTime,Long userId) {
        super(source);
        this.sessionId = sessionId;
        this.question = question;
        this.answer = answer;
        this.msgUid = msgUid;
        this.questionType = questionType;
        this.tokensUsed = tokensUsed;
        this.processingTime = processingTime;
        this.userId = userId;
    }
}
