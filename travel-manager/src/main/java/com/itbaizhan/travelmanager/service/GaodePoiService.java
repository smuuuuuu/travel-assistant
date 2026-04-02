package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.GaodePoi;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelmanager.config.RedisKeyProperties;
import com.itbaizhan.travelmanager.config.RedisPromptProperties;
import com.itbaizhan.travelmanager.constant.TripConstant;
import com.itbaizhan.travelmanager.mapper.GaodePoiMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GaodePoiService {
    @Autowired
    private GaodePoiMapper gaodePoiMapper;
    @Autowired
    private RedisKeyProperties redisKeyProperties;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Page<GaodePoi> getGaodePoiPage(int pageNo, int pageSize) {
        Page<GaodePoi> page = new Page<>(pageNo, pageSize);
        return gaodePoiMapper.selectPage(page, null);
    }
    public Page<GaodePoi> getGaodePoiPageByType(int type,int pageNo,int pageSize) {
        Page<GaodePoi> page = new Page<>(pageNo, pageSize);
        QueryWrapper<GaodePoi> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type",type);
        return gaodePoiMapper.selectPage(page, queryWrapper);
    }
    @Transactional
    public void insertGaodePoi(GaodePoi gaodePoi) {
        gaodePoiMapper.insert(gaodePoi);
        handle(gaodePoi,true);
    }
    @Transactional
    public void updateGaodePoi(GaodePoi gaodePoi) {
        gaodePoiMapper.updateById(gaodePoi);
        handle(gaodePoi,false);
    }
    @Transactional
    public void deleteGaodePoi(List<Long> ids) {
        List<GaodePoi> gaodePois = gaodePoiMapper.selectByIds(ids);
        gaodePoiMapper.deleteByIds(ids);
        Set<Integer> type = new HashSet<>();
        for(GaodePoi gaodePoi : gaodePois){
            if(!type.contains(gaodePoi.getType())){
                handle(gaodePoi,false);
            }
            type.add(gaodePoi.getType());
        }
    }

    private void handle(GaodePoi gaodePoi,boolean isCreate){
        String key = "";
        if(TripConstant.ACCOMMODATION_TYPE.equals(gaodePoi.getType())){
            key = redisKeyProperties.buildPoiKey(TripConstant.ACCOMMODATION);
        } else if (TripConstant.SCENIC_SPOT_TYPE.equals(gaodePoi.getType())) {
            key = redisKeyProperties.buildPoiKey(TripConstant.SCENIC);
        } else if (TripConstant.CATERING_TYPE.equals(gaodePoi.getType())) {
            key = redisKeyProperties.buildPoiKey(TripConstant.CATERING);
        }else {
            throw new BusException(CodeEnum.TRIP_SEARCH_TYPE_ERROR);
        }
        if(isCreate){
            String text = (String) redisTemplate.opsForValue().get(key);
            if(text != null){
                text = text + "," + gaodePoi.getPoiName();
                redisTemplate.opsForValue().set(key, text);
            }
        }else {
            redisTemplate.delete(key);
        }
    }
}