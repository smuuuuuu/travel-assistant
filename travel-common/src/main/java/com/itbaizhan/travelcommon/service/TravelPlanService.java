package com.itbaizhan.travelcommon.service;

import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface TravelPlanService {
    void generatePlanStream(TravelPlanRequest request, SseEmitter emitter,Long userId);

    void chat(String question, String current, SseEmitter emitter,Long userId, String tripId,Integer isBackup);

    void generateMd(String tripId, Long userId,SseEmitter emitter);
}
