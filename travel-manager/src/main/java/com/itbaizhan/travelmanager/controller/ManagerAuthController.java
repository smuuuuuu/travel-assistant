package com.itbaizhan.travelmanager.controller;

import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelmanager.config.ManagerJwtProperties;
import com.itbaizhan.travelmanager.dto.ManagerLoginRequest;
import com.itbaizhan.travelmanager.dto.ManagerLoginResponse;
import com.itbaizhan.travelmanager.security.ManagerJwtService;
import com.itbaizhan.travelmanager.security.ManagerPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/auth")
@RequiredArgsConstructor
public class ManagerAuthController {

    private final AuthenticationManager authenticationManager;
    private final ManagerJwtService managerJwtService;
    private final ManagerJwtProperties managerJwtProperties;

    @PostMapping("/login")
    public BaseResult<ManagerLoginResponse> login(@RequestBody ManagerLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusException(CodeEnum.PARAMETER_ERROR.getCode(), "用户名或密码不能为空");
        }
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            ManagerPrincipal principal = (ManagerPrincipal) auth.getPrincipal();
            String token = managerJwtService.createToken(principal.getManagerUser());
            long seconds = managerJwtProperties.getExpirationHours() * 3600L;
            return BaseResult.success(new ManagerLoginResponse(token, seconds));
        } catch (AuthenticationException e) {
            throw new BusException(CodeEnum.LOGIN_NAME_PASSWORD_ERROR);
        }
    }
}
