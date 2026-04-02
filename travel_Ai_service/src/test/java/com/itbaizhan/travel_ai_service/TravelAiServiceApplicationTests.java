package com.itbaizhan.travel_ai_service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travel_ai_service.controller.AiAssistantController;
import com.itbaizhan.travel_ai_service.mapper.AiConversationsMapper;
import com.itbaizhan.travel_ai_service.service.AiAssistantServiceImpl;
import com.itbaizhan.travel_ai_service.service.FaqServiceImpl;
import com.itbaizhan.travel_ai_service.tools.WebSearchTool;
import com.itbaizhan.travelcommon.pojo.AiConversations;
import com.itbaizhan.travelcommon.result.BaseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
class TravelAiTests {
    /*@Autowired
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
    }*/

    /*@Autowired
    private AmapTool amapTool;
    @Test
    void contextLoads() {
        *//*String s = amapTool.mapsTextSearch("餐厅", "餐饮", "徐州市", "20", "1");
        System.out.println(s);
        String s1 = amapTool.mapsTextSearch("餐厅", "餐饮", "徐州市", "20", "2");
        System.out.println(s1);
        String s2 = amapTool.mapsTextSearch("餐厅", "餐饮", "徐州市", "10", "4");
        System.out.println(s2);
        String s3 = amapTool.mapsTextSearch("餐厅", "餐饮", "徐州市", "10", "8");
        System.out.println(s3);*//*
        String s4 = amapTool.mapsTextSearch("餐厅", "餐饮", "徐州市", "40", "2");
        System.out.println(s4);
    }*/
    /*@Value("${search.key}")
    private String key;
    @Test
    void contextLoads() {
        WebSearchTool webSearchTool = new WebSearchTool(key);
        String string = webSearchTool.webSearch("徐州旅游景点推荐 带图片");
        System.out.println(string);
    }*/
    @Autowired
    private FaqServiceImpl faqService;
    @Test
    public void contextLoads() {
        String s = "工具名称：mapsTextSearch，参数：{\"keywords\": \"景点\", \"type\": \"110000\", \"region\": \"徐州\", \"pageSize\": \"10\", \"pageNum\": \"1\"}";
        String s1 = "工具名称：mapsTextSearch，参数：{\"keywords\": \"美食\", \"type\": \"050000\", \"region\": \"徐州\", \"pageSize\": \"10\", \"pageNum\": \"1\"}";
        System.out.println(s.replace("{", "").replace("}", "").replace("\"", ""));
        System.out.println(s1.replace("{", "").replace("}", "").replace("\"", ""));
    }
}
