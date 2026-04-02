package com.itbaizhan.travelmanager.controller;

import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.config.RedisPromptProperties;
import com.itbaizhan.travelmanager.dto.PromptUpsertRequest;
import com.itbaizhan.travelmanager.mongo.AiPromptDocument;
import com.itbaizhan.travelmanager.service.PromptAdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manager/prompts")
@RequiredArgsConstructor
public class ManagerPromptController {

    private final PromptAdminService promptAdminService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private RedisPromptProperties redisPromptProperties;


    @GetMapping("/{id}")
    public BaseResult<AiPromptDocument> getById(@PathVariable String id) {
        return BaseResult.success(promptAdminService.getPrompt(id));
    }

    @PutMapping("/{id}")
    public BaseResult<?> update(@PathVariable String id, @RequestBody PromptUpsertRequest body) {
        promptAdminService.update(id, body.getContent(), body.getDescription());
        return BaseResult.success();
    }

    @PostMapping("/save")
    public BaseResult<String> create(@RequestBody AiPromptDocument promptDocument) {
        return BaseResult.success(promptAdminService.savePrompt(promptDocument));
    }

    /**
     * 获取agent-next-step
     * @return agent-next-step
     */
    @GetMapping("/agent")
    public BaseResult<String> getAgent() {
        String s = (String) redisTemplate.opsForValue().get(redisPromptProperties.getAgentNextStep());
        return BaseResult.success(s);
    }
    @PutMapping("/updateAgent")
    public BaseResult<?> updateAgent(@RequestBody NextStepRequest body) {
        redisTemplate.opsForValue().set(redisPromptProperties.getAgentNextStep(), body.content());
        return BaseResult.success();
    }
    private record NextStepRequest(String content){}
}
