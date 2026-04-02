package com.itbaizhan.travelcommon.info;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanResponse;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BackupDetails {
    private TravelPlanResponse travelPlanResponse;
    private String objectKey;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
