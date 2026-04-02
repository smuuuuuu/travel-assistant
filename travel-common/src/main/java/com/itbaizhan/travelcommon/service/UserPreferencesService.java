package com.itbaizhan.travelcommon.service;

import com.itbaizhan.travelcommon.info.Preferences;
import com.itbaizhan.travelcommon.info.UserInfo;
import com.itbaizhan.travelcommon.pojo.*;

import java.util.List;

/**
* @author smuuuu
* @description 针对表【user_preferences(用户偏好设置表)】的数据库操作Service
* @createDate 2025-10-11 19:35:49
*/
public interface UserPreferencesService {
    UserInfo getUser(String token);

    UserInfo getUserInfo(String token);

    String getUserName(String token);

    void updateUserPreferences(String token, Preferences preferences);

    List<AccommodationTypes> getAccommodationTypes();
    List<ActivityPreferences> getActivityPreferences();
    List<BudgetRanges> getBudgetRanges();
    List<FoodPreferences> getFoodPreferences();
    List<TransportationTypes> getTransportationTypes();
    List<TravelStyles> getTravelStyles();
}
