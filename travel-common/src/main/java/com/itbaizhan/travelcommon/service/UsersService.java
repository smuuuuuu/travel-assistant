package com.itbaizhan.travelcommon.service;

import com.itbaizhan.travelcommon.info.Preferences;
import com.itbaizhan.travelcommon.info.UserInfo;
import com.itbaizhan.travelcommon.pojo.RegisterUser;

/**
* @author smuuuu
* @description 针对表【users(用户基本信息表)】的数据库操作Service
* @createDate 2025-10-10 19:42:45
*/
public interface UsersService{

    String login(String username, String password,boolean remember);

    String loginWithCode(String phone, String code);

    void register(RegisterUser users);

    void checkPhone(String phone);

    void saveLoginCode(String phone, String code);

    void checkLoginCode(String phone, String code);

    UserInfo getUserInfo(String token);

}
