package com.itbaizhan.travelmanager.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * FAQ 批量导入（文件解析入库）结束事件，供监听器向用户推送完成/失败消息。
 */
@Getter
public class FaqImportCompletedEvent extends ApplicationEvent {

    private final String operatorUsername;
    private final int insertedCount;
    private final boolean success;
    private final String message;

    public FaqImportCompletedEvent(Object source, String operatorUsername, int insertedCount,
                                   boolean success, String message) {
        super(source);
        this.operatorUsername = operatorUsername;
        this.insertedCount = insertedCount;
        this.success = success;
        this.message = message;
    }
}
