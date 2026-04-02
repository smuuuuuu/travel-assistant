package com.itbaizhan.travel_ai_service.tools;

import com.itbaizhan.travel_ai_service.service.FaqServiceImpl;
import com.itbaizhan.travelcommon.pojo.TravelFaq;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class FaqTool {

    private final FaqServiceImpl faqService;

    public FaqTool(FaqServiceImpl faqService) {
        this.faqService = faqService;
    }

    @Tool(description = "Query from the vector database")
    public String searchFromVector(@ToolParam(description = "Search query keyword") String query){
        TravelFaq travelFaq = faqService.searchBestAnswer(query);
        if(travelFaq != null) {
            return travelFaq.getAnswer();
        }
        return "There is no data related to this question in the vector database.";
    }
}
