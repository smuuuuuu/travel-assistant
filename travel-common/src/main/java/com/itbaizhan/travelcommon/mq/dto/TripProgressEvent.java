package com.itbaizhan.travelcommon.mq.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 行程生成进度事件（用于 RocketMQ 传递、Redis 快照落库、SSE 推送）。
 */
@Data
@NoArgsConstructor
public class TripProgressEvent implements Serializable {
    private String tripId;
    /**
     * 流类型：1=生成，2=修改（用于区分同一 trip 的不同进行中任务）
     */
    private Integer streamType;
    private Long userId;
    private String type;
    private String status;
    private Long seq;
    private Long ts;
    private String destination;
    private Integer totalDays;

}
