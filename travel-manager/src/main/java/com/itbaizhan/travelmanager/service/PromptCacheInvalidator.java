package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itbaizhan.travelcommon.constant.General;
import com.itbaizhan.travelcommon.pojo.AiModuleConfig;
import com.itbaizhan.travelmanager.config.RedisPromptProperties;
import com.itbaizhan.travelmanager.mapper.AiModuleConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 与 travel_AI_trip {@code PromptSelectServiceImpl} 使用的 Redis key 规则对齐。
 */
@Component
@RequiredArgsConstructor
public class PromptCacheInvalidator {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisPromptProperties redisPromptProperties;
    private final AiModuleConfigMapper aiModuleConfigMapper;

    public void updatePrompt(String scene){
        QueryWrapper<AiModuleConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("prompt_id", scene);
        queryWrapper.eq("is_enabled", General.ENABLE);
        AiModuleConfig aiModuleConfig = aiModuleConfigMapper.selectOne(queryWrapper);
        if(aiModuleConfig != null) {
            invalidateSceneAndAggregates(scene, aiModuleConfig.getIsTool(), aiModuleConfig.getModuleKey());
        }
    }

    public void invalidateSceneAndAggregates(String scene,Integer isTool,String moduleKey) {
        List<String> keys = new ArrayList<>();
        keys.add(redisPromptProperties.getRedisKey(scene));
        if(General.IS_TOOL.equals(isTool)){
            addIfConfigured(keys, redisPromptProperties.getAllGenerate());
            addIfConfigured(keys, redisPromptProperties.getAllModify());
            addIfConfigured(keys, redisPromptProperties.getAllTool());
        } else if (General.GENERATE.equals(moduleKey)) {
            addIfConfigured(keys, redisPromptProperties.getAllGenerate());
        } else if (General.MODIFY_TOOL.equals(moduleKey)) {
            addIfConfigured(keys, redisPromptProperties.getAllModify());
        }
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
    public void savePrompt(String scene) {
        stringRedisTemplate.opsForValue().set(redisPromptProperties.getRedisKey(scene), scene);
    }
    private void addIfConfigured(List<String> keys, String suffix) {
        if (StringUtils.hasText(suffix)) {
            keys.add(redisPromptProperties.getRedisKey(suffix));
        }
    }
}
