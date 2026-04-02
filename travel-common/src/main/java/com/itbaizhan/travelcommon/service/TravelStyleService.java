package com.itbaizhan.travelcommon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itbaizhan.travelcommon.pojo.TravelStyles;

public interface TravelStyleService {

    void createTravelStyle(Long userId, String style);

    void deleteTravelStyle(Long userId,Long styleId);

    void updateTravelStyle(Long userId,Long styleId,String style);

    IPage<TravelStyles> getTravelStyles(Long userId,String styleName,Integer status
            ,Integer pageNo,Integer pageSize);

    void chargeStatus(Long styleId,Integer status);
}
