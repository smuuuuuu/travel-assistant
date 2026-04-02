package com.itbaizhan.traveluserapi.controller;

import com.itbaizhan.travelcommon.info.Preferences;
import com.itbaizhan.travelcommon.info.UserInfo;
import com.itbaizhan.travelcommon.pojo.RegisterUser;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.service.UsersService;
import com.itbaizhan.travelcommon.util.RandomUtil;
import com.itbaizhan.traveluserapi.dto.LoginRequest;
import com.itbaizhan.traveluserapi.dto.LoginResponse;
import com.itbaizhan.traveluserapi.dto.SmsLoginDto;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @DubboReference(timeout = 3000)
    private UsersService usersService;
    @GetMapping("/test")
    public BaseResult<String> test() {
        return BaseResult.success("test");
    }
    /**
     * 用户登录
     * @param loginRequest 登录请求对象，包含用户名和密码
     * @return 登录响应，包含JWT token和用户信息
     */
    @PostMapping("/login")
    public BaseResult<LoginResponse> login(@RequestBody LoginRequest loginRequest) {

        // 调用用户服务进行登录验证，获取JWT token
        String token = usersService.login(loginRequest.getUsername(), loginRequest.getPassword(),loginRequest.getRememberMe());

        // 获取用户信息
        UserInfo userInfo = usersService.getUserInfo(token);

        // 构建登录响应对象
        LoginResponse loginResponse = new LoginResponse(token, userInfo);
        loginResponse.setExpiresIn(7200L); // 设置token过期时间为2小时

        return BaseResult.success(loginResponse);
    }
    @PostMapping("/smsLogin")
    public BaseResult<LoginResponse> loginWithCode(@RequestBody SmsLoginDto smsLoginDto) {
        String token = usersService.loginWithCode(smsLoginDto.getPhone(), smsLoginDto.getCode());
        UserInfo userInfo = usersService.getUserInfo(token);
        LoginResponse loginResponse = new LoginResponse(token, userInfo);
        loginResponse.setExpiresIn(7200L); // 设置token过期时间为2
        return BaseResult.success(loginResponse);
    }

    /**
     * 发送验证码
     * @param phone 手机号
     * @param status 状态（1.登录 2.注册）
     * @return
     */
    @GetMapping("/sendLoginCheckCode")
    public BaseResult sendLoginCheckCode(@RequestParam String phone,@RequestParam Integer status) {
        if(status == 2){
            usersService.checkPhone(phone);
        }
        String s = RandomUtil.buildCheckCode(6);
        System.out.println("----------------" + s + "---------------------");
        usersService.saveLoginCode(phone, s);
        return BaseResult.success();
    }
    /**
     * 用户注册
     * @param registerUser 注册用户信息
     * @param request 请求对象
     * @return
     */
    @PostMapping("/register")
    public BaseResult register(@RequestBody RegisterUser registerUser, HttpServletRequest request) {
        registerUser.setIp(request.getRemoteAddr());
        usersService.register(registerUser);
        return BaseResult.success();
    }

}
