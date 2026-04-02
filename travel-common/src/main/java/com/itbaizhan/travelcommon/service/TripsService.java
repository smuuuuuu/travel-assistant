package com.itbaizhan.travelcommon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanResponse;
import com.itbaizhan.travelcommon.info.BackupInfo;
import com.itbaizhan.travelcommon.vo.TripVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


/**
* @author smuuuu
* @description 针对表【trips(行程计划表)】的数据库操作Service
* @createDate 2025-10-19 22:34:48
*/
public interface TripsService{
    Page<TripVo> getTripsVoById(Integer page, Integer size, Long userId);

    void generateAndModifyStream(String tripId, Long userId, SseEmitter emitter,boolean isModify);

    void generateMdStream(String tripId, Long userId, SseEmitter emitter);

    TravelPlanResponse getTripById(String tripId,Long userId);

    BackupInfo getBackup(String tripId, Long userId);

    void restoreBackup(String tripId,Long userId);

    void backupCover(String tripId, Long userId);

    void deleteBackup(String tripId, Long userId);

    void updateMysqlBackup(String tripId, Long userId);

    void insertRedisTravel(TravelPlanResponse travelPlanResponse, Long userId,Integer isBackup);

    //void planModify(TravelPlanResponse travelPlanResponse,Long userId,Integer isBackup);

    void updateTrip(String tripId,Long userId,Integer isBackup);

    void updateCompleteStatus(String tripId, Long userId,Integer status);

    void deleteTrip(String tripId,Long userId);

    void clear(Long userId);

    List<String> getHotStyle();

    void saveMdUrl(String tripId,Long userId,String objectKey);

    String getMdUrl(String tripId,Long userId);

    void insertTrip(TravelPlanResponse travelPlanResponse,Long userId);

    void syncToMysql(String tripId, Long userId,boolean isBackup);

    String download(String tripId);

    List<String> getPoiName(Integer type);

    void checkTravelPlanResponse(TravelPlanResponse travelPlanResponse);

    String getMdBackUrl(String tripId, Long userId);

    void completeConvertDraft(String tripId, Long userId);
}
