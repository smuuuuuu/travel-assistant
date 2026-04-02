package com.itbaizhan.travelmanager.service;

import com.itbaizhan.travelcommon.pojo.ManagerTokenUsage;
import com.itbaizhan.travelmanager.mapper.ManagerTokenUsageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在独立事务中落库 Token 用量，提交后不随调用方事务回滚（如 FAQ 批量插入失败）。
 */
@Service
@RequiredArgsConstructor
public class ManagerTokenUsageRecordService {

    private final ManagerTokenUsageMapper managerTokenUsageMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertCommitted(ManagerTokenUsage row) {
        managerTokenUsageMapper.insert(row);
    }
}
