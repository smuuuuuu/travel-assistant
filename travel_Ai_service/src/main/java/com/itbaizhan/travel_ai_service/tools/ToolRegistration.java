package com.itbaizhan.travel_ai_service.tools;

import com.itbaizhan.travel_ai_service.service.FaqServiceImpl;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class ToolRegistration {
    @Value("${amap.web.key}")
    private String amapKey;
    @Value("${redis.amaps}")
    private String amaps;
    @Value("${search.key}")
    private String key;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private FaqServiceImpl faqService;

    @Bean(name = "agentTools")
    public ToolCallback[] agentTools(WeatherService weatherService){
        AmapTool amapTool = new AmapTool(redisTemplate,amapKey,amaps);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        WebSearchTool webSearchTool = new WebSearchTool(key);
        TerminateTool terminateTool = new TerminateTool();
        FaqTool faqTool = new FaqTool(faqService);
        FreeTol freeTol = new FreeTol();
        DestinationTool destinationTool = new DestinationTool(amapTool);
        return ToolCallbacks.from(amapTool,webScrapingTool,webSearchTool,terminateTool,weatherService,faqTool,freeTol,destinationTool);
    }
}
