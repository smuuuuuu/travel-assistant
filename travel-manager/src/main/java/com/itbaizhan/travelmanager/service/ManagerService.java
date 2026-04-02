package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itbaizhan.travelcommon.AiSessionDto.ManagerDto;
import com.itbaizhan.travelcommon.pojo.ManagerOperationLog;
import com.itbaizhan.travelcommon.pojo.ManagerTokenUsage;
import com.itbaizhan.travelcommon.pojo.ManagerUser;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelmanager.mapper.ManagerOperationLogMapper;
import com.itbaizhan.travelmanager.mapper.ManagerTokenUsageMapper;
import com.itbaizhan.travelmanager.mapper.ManagerUserMapper;
import com.itbaizhan.travelmanager.security.ManagerSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManagerService {

    @Autowired
    private ManagerOperationLogMapper managerOperationLogMapper;
    @Autowired
    private ManagerTokenUsageMapper managerTokenUsageMapper;
    @Autowired
    private ManagerUserMapper managerUserMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public void updatePassword(String password){
        try {
            String encode = passwordEncoder.encode(password);
            Long managerId = ManagerSecurityContext.currentManagerId();
            ManagerUser managerUser = managerUserMapper.selectById(managerId);
            managerUser.setPassword(encode);
            managerUserMapper.updateById(managerUser);
        } catch (Exception e) {
            throw new BusException(CodeEnum.MANAGER_USER_PASSWORD_ERROR);
        }
    }

    public ManagerDto managerDto(){
        Long managerId = ManagerSecurityContext.currentManagerId();
        ManagerUser managerUser = managerUserMapper.selectById(managerId);
        ManagerDto managerDto = new ManagerDto();
        managerDto.setName(managerUser.getUsername());
        managerDto.setRole(managerUser.getRole());
        managerDto.setCreateTime(managerUser.getUpdatedAt());
        managerDto.setUpdateTime(managerUser.getUpdatedAt());
        return managerDto;
    }

    public List<ManagerOperationLog> managerOperationLog(){
        Long managerId = ManagerSecurityContext.currentManagerId();
        QueryWrapper<ManagerOperationLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("manager_id", managerId);
        List<ManagerOperationLog> managerOperationLogs = managerOperationLogMapper.selectList(queryWrapper);
        return managerOperationLogs.stream().peek(managerOperationLog -> {
            managerOperationLog.setId(null);
            managerOperationLog.setManagerId(null);
        }).toList();
    }

    public List<ManagerTokenUsage> managerTokenUsage(){
        Long managerId = ManagerSecurityContext.currentManagerId();
        QueryWrapper<ManagerTokenUsage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("manager_id", managerId);
        List<ManagerTokenUsage> managerTokenUsages = managerTokenUsageMapper.selectList(queryWrapper);
        return managerTokenUsages.stream().peek(managerTokenUsage -> {
            managerTokenUsage.setId(null);
            managerTokenUsage.setManagerId(null);
        }).toList();
    }
}
