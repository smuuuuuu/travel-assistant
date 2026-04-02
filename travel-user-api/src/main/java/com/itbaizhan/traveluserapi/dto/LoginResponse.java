package com.itbaizhan.traveluserapi.dto;

import com.itbaizhan.travelcommon.info.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户登录响应对象
 * 用于返回登录成功后的JWT token和用户信息
 * 
 * @author 智游助手开发团队
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse implements Serializable {
    
    /**
     * JWT认证令牌
     */
    private String token;
    
    /**
     * 用户基本信息
     */
    private UserInfo userInfo;
    
    /**
     * 令牌类型（通常为Bearer）
     */
    private String tokenType = "Bearer";
    
    /**
     * 令牌过期时间（秒）
     */
    private Long expiresIn;

    public LoginResponse(String token,UserInfo userInfo) {
        this.userInfo = userInfo;
        this.token = token;
    }
}