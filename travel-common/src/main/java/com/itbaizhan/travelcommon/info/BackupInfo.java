package com.itbaizhan.travelcommon.info;

import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanResponse;
import lombok.Data;

@Data
public class BackupInfo {
    private TravelPlanResponse travelPlanResponse;
    private String expire;
    private String objectKey;
}
