package com.itbaizhan.traveluserapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户登录请求对象
 * 用于接收前端发送的登录参数
 * 
 * @author 智游助手开发团队
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest implements Serializable {
    
    /**
     * 用户名或邮箱
     */
    private String username;
    
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 是否记住我
     */
    private Boolean rememberMe;
}