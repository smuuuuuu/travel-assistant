package com.itbaizhan.travel_trip_service.service;

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
        List<TripGaodeType> tripGaodeTypes = tripGaodeTypeMapper.selectList(null);
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
    public String getAllModPrompt() {
        String key = redisPromptProperties.getRedisKey(redisPromptProperties.getAllModify());
        String prompt = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.hasText(prompt)) {
            return prompt;
        }

        String s = PromptUtil.renderPromptTemplate(this.getPrompt(redisPromptProperties.getModify()),
                Map.of("TOOL_RULES", getAllTools()));
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

    /**
     * 仅根据 Scene 获取内容 (不走映射逻辑，用于兜底或直接查询)
     */
    private String getPromptContentOnly(String scene) {
        try {
            Optional<AiPrompt> promptOpt = aiPromptRepository.findById(scene);
            if (promptOpt.isPresent()) {
                return promptOpt.get().getContent();
            }
        } catch (Exception e) {
             // ignore
        }
        return null;
    }

    /**
     * 更新映射配置 (管理员修改了 Scene 名称或切换版本时调用)
     * @param logicalKey 代码中的常量 (如 prompt_accommodation)
     * @param newRealScene 数据库中新的 Scene (如 prompt_accommodation_v2)
     */
    public void updatePromptMapping(String logicalKey, String newRealScene) {
        //stringRedisTemplate.opsForHash().put(redisKeyProperties.getScenePrompt(), logicalKey, newRealScene);
    }

    /**
     * 刷新缓存 (当后台更新 Prompt 内容时调用)
     */
    @Override
    public void refreshCache(String scene) {
        String cacheKey = redisPromptProperties.getRedisKey(scene);
        stringRedisTemplate.delete(cacheKey);
    }
    
    /**
     * 创建或更新 Prompt (供后台管理使用)
     */
    @Override
    public void savePrompt(String scene, String content, String description) {
        AiPrompt prompt = aiPromptRepository.findById(scene).orElse(new AiPrompt());
        prompt.setScene(scene);
        prompt.setContent(content);
        prompt.setDescription(description);
        prompt.setIsActive(true);
        if (prompt.getId() == null) {
            // 新增时设置初始版本
            prompt.setVersion("v1.0.0");
        }
        // TODO: 版本号递增逻辑可根据需求完善
        
        aiPromptRepository.save(prompt);
        refreshCache(scene);
    }

}
