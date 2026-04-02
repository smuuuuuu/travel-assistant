package com.itbaizhan.travel_trip_service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itbaizhan.travel_trip_service.config.RedisPromptProperties;
import com.itbaizhan.travel_trip_service.entity.mongo.AiPrompt;
import com.itbaizhan.travel_trip_service.mapper.TripGaodeTypeMapper;
import com.itbaizhan.travel_trip_service.repository.mongo.AiPromptRepository;
import com.itbaizhan.travel_trip_service.utils.PromptUtil;
import com.itbaizhan.travelcommon.pojo.AiModuleConfig;
import com.itbaizhan.travelcommon.pojo.TripGaodeType;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.service.AiModuleConfigService;
import com.itbaizhan.travelcommon.service.PromptSelectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI Prompt 管理服务 (MongoDB + Redis)
 */
@Service
@Slf4j
public class PromptSelectServiceImpl implements PromptSelectService {

    @Autowired
    private AiPromptRepository aiPromptRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisPromptProperties redisPromptProperties;
    @Autowired
    private AiModuleConfigService aiModuleConfigService;
    @Autowired
    private TripGaodeTypeMapper tripGaodeTypeMapper;

    private static final Duration CACHE_TTL = Duration.ofHours(24); // 内容缓存时间

    @Override
    public String getAllGaodeType() {
        StringBuilder stringBuilder = new StringBuilder();
        QueryWrapper<TripGaodeType> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("ai_module_id",0);
        List<TripGaodeType> tripGaodeTypes = tripGaodeTypeMapper.selectList(queryWrapper);
        if(tripGaodeTypes.isEmpty()) {
            return "";
        }
        for (TripGaodeType tripGaodeType : tripGaodeTypes) {
            stringBuilder.append(tripGaodeType.getName()).append("=").append(tripGaodeType.getType()).append("/");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    @Override
    public String getAllGenPrompt() {
        String key = redisPromptProperties.getRedisKey(redisPromptProperties.getAllGenerate());
        String prompt = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.hasText(prompt)) {
            return prompt;
        }
        String s = PromptUtil.renderPromptTemplate(this.getPrompt(redisPromptProperties.getGenerate()),
                Map.of("TOOL_RULES",getAllTools()));
        stringRedisTemplate.opsForValue().set(key, s);
        return s;
    }
    @Override
    public String getToolRestriction() {
        String key = redisPromptProperties.getRedisKey(redisPromptProperties.getAllModify());
        String prompt = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.hasText(prompt)) {
            return prompt;
        }

        String s = PromptUtil.renderPromptTemplate(this.getPrompt(redisPromptProperties.getToolRestriction()),
                Map.of("POI_TYPE", this.getAllGaodeType())) +
                "\n" +
                this.getAllTools();
        stringRedisTemplate.opsForValue().set(key, s);
        return s;
    }
    @Override
    public String getAllTools(){
        StringBuilder stringBuilder = new StringBuilder();
        List<AiModuleConfig> aiModuleConfig1 = aiModuleConfigService.getAiModuleToolEnable();
        if(aiModuleConfig1 != null && !aiModuleConfig1.isEmpty()) {
            for (AiModuleConfig aiModuleConfig : aiModuleConfig1) {
                stringBuilder.append(this.getPrompt(aiModuleConfig.getPromptId())).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 获取指定场景的 Prompt 内容45
     * 流程：
     * 1. 根据 logicalKey 查 Redis 映射 -> 得到 realScene (支持动态切换)
     * 2. 根据 realScene 查 Redis 内容缓存
     * 3. Redis 未命中 -> 查 MongoDB -> 回写 Redis
     *
     * @param realScene 业务逻辑标识 (代码中的常量)
     * @return Prompt 文本
     */
    @Override
    public String getPrompt(String realScene) {
        /*// 1. 获取真实 Scene Key (配置映射)
        // 优先从 Redis Hash 中获取当前配置的映射关系
        Object mappedSceneObj = stringRedisTemplate.opsForHash().get(redisKeyProperties.getScenePrompt(), logicalKey);
        String realScene = mappedSceneObj != null ? mappedSceneObj.toString() : logicalKey;*/

        // 2. 尝试从 Redis 获取内容
        String cacheKey = redisPromptProperties.getRedisKey(realScene);
        String cachedPrompt = null;
        try {
            cachedPrompt = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("Failed to load prompt from Redis for scene: {}", realScene, e);
        }
        if (StringUtils.hasText(cachedPrompt)) {
            return cachedPrompt;
        }

        // 3. Redis 未命中，从 MongoDB 获取
        try {
            Optional<AiPrompt> promptOpt = aiPromptRepository.findById(realScene);
            if(promptOpt.isEmpty()) {
                promptOpt = aiPromptRepository.findByScene(realScene);
            }

            if (promptOpt.isPresent()) {
                String content = promptOpt.get().getContent();
                // 写入 Redis 缓存
                try {
                    stringRedisTemplate.opsForValue().set(cacheKey, content, CACHE_TTL);
                } catch (Exception e) {
                    log.error("Failed to set prompt to Redis for scene: {}", realScene, e);
                }
                return content;
            }
        } catch (Exception e) {
            log.error("Failed to load prompt from MongoDB for scene: {}", realScene, e);
        }

        throw new BusException(CodeEnum.TRIP_PROMPT_ERROR);
    }
}
