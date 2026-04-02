package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.constant.General;
import com.itbaizhan.travelcommon.managerDto.GaoDeAiModuleDto;
import com.itbaizhan.travelcommon.pojo.AiModuleConfig;
import com.itbaizhan.travelcommon.pojo.TripGaodeType;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelmanager.config.RedisKeyProperties;
import com.itbaizhan.travelmanager.mapper.AiModuleConfigMapper;
import com.itbaizhan.travelmanager.mapper.TripGaodeTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiModuleConfigAdminService {

    private final AiModuleConfigMapper aiModuleConfigMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisKeyProperties redisKeyProperties;
    private final TripGaodeTypeMapper tripGaodeTypeMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PromptAdminService promptAdminService;
    private final PromptCacheInvalidator promptCacheInvalidator;

    public IPage<AiModuleConfig> getListByPage(Integer pageNo, Integer pageSize){
        IPage<AiModuleConfig> page = new Page<>(pageNo, pageSize);
        return aiModuleConfigMapper.selectPage(page, null);
    }
    public List<GaoDeAiModuleDto> getGaoDeAiModuleList(){
        QueryWrapper<AiModuleConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_tool",1);
        List<AiModuleConfig> aiModuleConfigs = aiModuleConfigMapper.selectList(queryWrapper);
        List<TripGaodeType> tripGaodeTypes = tripGaodeTypeMapper.selectList(null);
        List<GaoDeAiModuleDto> gaoDeAiModuleDtos = new ArrayList<>();
        if(tripGaodeTypes != null && !tripGaodeTypes.isEmpty()){
            tripGaodeTypes.forEach(tripGaodeType -> {
                aiModuleConfigs.forEach(aiModuleConfig -> {
                    if(aiModuleConfig.getId().equals(tripGaodeType.getAiModuleId())){
                        GaoDeAiModuleDto dto = new GaoDeAiModuleDto();
                        BeanUtils.copyProperties(aiModuleConfig,dto);
                        dto.setType(tripGaodeType.getType());
                        dto.setName(tripGaodeType.getName());
                        gaoDeAiModuleDtos.add(dto);
                    }
                });
            });
        }
        return gaoDeAiModuleDtos;
    }
    @Transactional
    public void enable(Long id) {
        AiModuleConfig aiModuleConfig = aiModuleConfigMapper.selectById(id);
        if (aiModuleConfig == null) {
            throw new BusException(CodeEnum.PARAMETER_ERROR.getCode(), "id invalid");
        }
        aiModuleConfig.setIsEnabled(General.ENABLE);
        aiModuleConfigMapper.updateById(aiModuleConfig);
        promptCacheInvalidator.invalidateSceneAndAggregates(aiModuleConfig.getPromptId(),aiModuleConfig.getIsTool(), aiModuleConfig.getModuleKey());
        evictModuleCache();
    }
    @Transactional
    public AiModuleConfig update(AiModuleConfig body) {
        if (body.getId() == null) {
            throw new BusException(CodeEnum.PARAMETER_ERROR.getCode(), "id required");
        }
        int rows = aiModuleConfigMapper.updateById(body);
        if (rows == 0) {
            throw new BusException(CodeEnum.PARAMETER_ERROR.getCode(), "update failed");
        }
        evictModuleCache();
        return aiModuleConfigMapper.selectById(body.getId());
    }
    @Transactional
    public AiModuleConfig create(AiModuleConfig body) {
        body.setId(null);
        aiModuleConfigMapper.insert(body);
        evictModuleCache();
        promptCacheInvalidator.savePrompt(body.getPromptId());
        return body;
    }
    @Transactional
    public void delete(Long id) {
        AiModuleConfig aiModuleConfig = aiModuleConfigMapper.selectById(id);
        if (aiModuleConfig.getPromptId() != null) {
            promptAdminService.deleteByScene(aiModuleConfig.getPromptId());
        }
        aiModuleConfigMapper.deleteById(id);
        if(General.ENABLE.equals(aiModuleConfig.getIsEnabled())){
            promptCacheInvalidator.invalidateSceneAndAggregates(aiModuleConfig.getPromptId(),aiModuleConfig.getIsTool(), aiModuleConfig.getModuleKey());
        }
        evictModuleCache();
    }

    private void evictModuleCache() {
        String key = redisKeyProperties.getAiModuleEnable();
        if (StringUtils.hasText(key)) {
            stringRedisTemplate.delete(key);
        }
    }
}
