package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.AiModuleConfig;
import com.itbaizhan.travelcommon.pojo.TripGaodeType;
import com.itbaizhan.travelmanager.mapper.AiModuleConfigMapper;
import com.itbaizhan.travelmanager.mapper.TripGaodeTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GaoDeTypeService {
    @Autowired
    private TripGaodeTypeMapper tripGaodeTypeMapper;
    @Autowired
    private AiModuleConfigMapper aiModuleConfigMapper;

    public Page<TripGaodeType> getGaoDeTypeByPage(int pageNo, int pageSize){
        Page<TripGaodeType> page = new Page<>(pageNo, pageSize);
        return tripGaodeTypeMapper.selectPage(page, null);
    }
    @Transactional
    public void insertGaoDeType(TripGaodeType tripGaodeType){
        tripGaodeTypeMapper.insert(tripGaodeType);
    }
    @Transactional
    public void updateGaoDeType(TripGaodeType tripGaodeType){
        tripGaodeTypeMapper.updateById(tripGaodeType);
    }
    @Transactional
    public void deleteGaoDeType(Long id){
        tripGaodeTypeMapper.deleteById(id);
    }
    @Transactional
    public void updateGaoDeTypeAiModuleId(Long id, Long aiModuleId){
        TripGaodeType tripGaodeType = tripGaodeTypeMapper.selectById(id);
        AiModuleConfig aiModuleConfig = aiModuleConfigMapper.selectById(aiModuleId);
        if(tripGaodeType != null && aiModuleConfig != null){
            tripGaodeType.setAiModuleId(aiModuleId);
            tripGaodeTypeMapper.updateById(tripGaodeType);
        }
    }
}
