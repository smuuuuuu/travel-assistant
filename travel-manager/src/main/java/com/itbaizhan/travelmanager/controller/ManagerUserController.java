package com.itbaizhan.travelmanager.controller;

import com.itbaizhan.travelcommon.AiSessionDto.ManagerDto;
import com.itbaizhan.travelcommon.pojo.ManagerOperationLog;
import com.itbaizhan.travelcommon.pojo.ManagerTokenUsage;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manager/user")
public class ManagerUserController {
    @Autowired
    private ManagerService managerService;

    @PutMapping("/encode")
    public BaseResult<?> password(@RequestParam String password) {
        managerService.updatePassword(password);
        return BaseResult.success();
    }
    @GetMapping
    public BaseResult<ManagerDto> getUser() {
        return BaseResult.success(managerService.managerDto());
    }
    @GetMapping("/log")
    public BaseResult<List<ManagerOperationLog>> getUserLog() {
        return BaseResult.success(managerService.managerOperationLog());
    }
    @GetMapping("/useToken")
    public BaseResult<List<ManagerTokenUsage>> getUserToken() {
        return BaseResult.success(managerService.managerTokenUsage());
    }
}
