package com.itbaizhan.travel_ai_service.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WebSearchTool {
    public WebSearchTool(String key) {
        this.key = key;
    }

    private static final String WEB_URL = "https://www.searchapi.io/api/v1/search?engine=baidu";

    private final String key;

    @Tool(description = "Search for information from Baidu Search Engine")
    public String webSearch(@ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("q", query);
        params.put("api_key",key);

        try {
            String response = HttpUtil.get(WEB_URL, params);
            JSONObject entries = JSONUtil.parseObj(response);
            JSONArray jsonArray = entries.getJSONArray("organic_results");

            List<Object> subList = jsonArray.subList(0, Math.min(jsonArray.size(), 5));
            return subList.stream().map(s -> {
                JSONObject jsonObject = (JSONObject) s;
                return jsonObject.toString();
            }).collect(Collectors.joining(","));
        } catch (Exception e) {
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
