package com.itbaizhan.travel_ai_service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travel_ai_service.controller.AiAssistantController;
import com.itbaizhan.travel_ai_service.mapper.AiConversationsMapper;
import com.itbaizhan.travel_ai_service.service.AiAssistantServiceImpl;
import com.itbaizhan.travelcommon.pojo.AiConversations;
import com.itbaizhan.travelcommon.result.BaseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
class TravelAiServiceApplicationTests {
    @Autowired
    private AiConversationsMapper aiConversationsMapper;
    @Test
    void contextLoads() {
        IPage<AiConversations> aiConversationsIPage = new Page<>(1, 10);
        QueryWrapper<AiConversations> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", 1).eq("context_type", 1);
        queryWrapper.orderByDesc("createdAt");
        IPage<AiConversations> aiConversationsIPage1 = aiConversationsMapper.selectPage(aiConversationsIPage, queryWrapper);
        System.out.println(aiConversationsIPage1.getRecords());
        System.out.println(aiConversationsIPage1.getTotal());
    }

}
