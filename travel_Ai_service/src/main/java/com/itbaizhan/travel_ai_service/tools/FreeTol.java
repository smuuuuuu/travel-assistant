package com.itbaizhan.travel_ai_service.tools;

import org.springframework.ai.tool.annotation.Tool;

public class FreeTol {


    @Tool(description = "Use it when you don't need to use tools but don't want to end the current task.")
    public String idleTool(){
        return "idle";
    }
}
