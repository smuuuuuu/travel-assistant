package com.itbaizhan.traveluserapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 手机号验证码登录请求对象
 * 用于接收前端发送的验证码登录参数
 * 
 * @author 智游助手开发团队
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsLoginRequest {
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 验证码
     */
    private String code;
    
    /**
     * 是否记住我
     */
    private Boolean rememberMe;
}