package com.itbaizhan.travel_trip_service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itbaizhan.travel_trip_service.config.RedisKeyProperties;
import com.itbaizhan.travel_trip_service.mapper.AiModuleConfigMapper;
import com.itbaizhan.travel_trip_service.mapper.TripGaoDeMapper;
import com.itbaizhan.travel_trip_service.mapper.TripGaodeTypeMapper;
import com.itbaizhan.travelcommon.constant.General;
import com.itbaizhan.travelcommon.pojo.AiModuleConfig;
import com.itbaizhan.travelcommon.pojo.TripGaodeType;
import com.itbaizhan.travelcommon.service.AiModuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiModuleConfigServiceImpl implements AiModuleConfigService {
    @Autowired
    private AiModuleConfigMapper aiModuleConfigMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedisKeyProperties redisKeyProperties;
    @Autowired
    private TripGaoDeMapper tripGaoDeMapper;
    @Autowired
    private TripGaodeTypeMapper tripGaodeTypeMapper;
    @Override
    public List<AiModuleConfig> getAiModuleConfig() {
        return aiModuleConfigMapper.selectList(null);
    }

    @Override
    public List<AiModuleConfig> getAiModuleToolEnable() {
        Object object = redisTemplate.opsForValue().get(redisKeyProperties.getAiModuleEnable());
        if(object != null){
            return (List<AiModuleConfig>) object;
        }
        QueryWrapper<AiModuleConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_enabled", General.ENABLE);
        queryWrapper.eq("is_tool",General.IS_TOOL);
        List<AiModuleConfig> aiModuleConfigs = aiModuleConfigMapper.selectList(queryWrapper);
        if(aiModuleConfigs == null){
            return List.of();
        }
        redisTemplate.opsForValue().set(redisKeyProperties.getAiModuleEnable(), aiModuleConfigs);
        return aiModuleConfigs;
    }
    @Override
    public String getPoiTypeWithAiModuleId(Map<String,Boolean> targets){
        List<AiModuleConfig> aiModuleToolEnable = this.getAiModuleToolEnable();
        List<Long> aiModuleIds = new ArrayList<>();
        if(aiModuleToolEnable != null){
            if(targets != null){
                for (AiModuleConfig aiModuleConfig : aiModuleToolEnable) {
                    if(targets.getOrDefault(aiModuleConfig.getModuleKey(),false)){
                        aiModuleIds.add(aiModuleConfig.getId());
                    }
                }

               /* List<TripGaodeType> tripGaodeTypes = tripGaodeTypeMapper.selectByIds(longs);
                if(tripGaodeTypes != null){
                    return tripGaodeTypes.stream().map(TripGaodeType::getName).collect(Collectors.joining("/"));
                }
                return "";*/
            }else {
                aiModuleToolEnable.stream().map(AiModuleConfig::getId).forEach(aiModuleIds::add);
            }

            List<String> nameByAiModuleId = tripGaoDeMapper.getNameByAiModuleId(aiModuleIds);
            if(nameByAiModuleId != null){
                return nameByAiModuleId.stream().map(String::valueOf).collect(Collectors.joining("/"));
            }
        }
        List<TripGaodeType> tripGaodeTypes = tripGaodeTypeMapper.selectList(null);
        if(tripGaodeTypes != null){
            return tripGaodeTypes.stream().map(TripGaodeType::getName).collect(Collectors.joining("/"));
        }
        return "";
    }
    @Override
    public List<AiModuleConfig> getAllPoiTypeAiModule(){
        QueryWrapper<TripGaodeType> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("ai_module_id",0);
        List<TripGaodeType> tripGaodeTypes = tripGaodeTypeMapper.selectList(queryWrapper);
        if(tripGaodeTypes != null){
            List<Long> collect = tripGaodeTypes.stream().map(TripGaodeType::getAiModuleId).toList();
            List<AiModuleConfig> aiModuleConfigs = aiModuleConfigMapper.selectByIds(collect);
            if(aiModuleConfigs != null){
                return aiModuleConfigs.stream().filter(
                        aiModuleConfig -> aiModuleConfig.getIsEnabled().equals(General.ENABLE)
                                && aiModuleConfig.getIsTool().equals(General.IS_TOOL)).toList();
            }
        }
        return new ArrayList<>();
    }
}
