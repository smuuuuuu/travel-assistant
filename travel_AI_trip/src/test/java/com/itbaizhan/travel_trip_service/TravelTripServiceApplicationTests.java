package com.itbaizhan.travel_trip_service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itbaizhan.travel_trip_service.tool.CodeTools;
import com.itbaizhan.travelcommon.AiSessionDto.TravelPlanResponse;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
class TravelTripServiceApplicationTests {

    @Autowired
    private List<McpAsyncClient> mcpAsyncClients;

    @Test
    void listAllTools(){
        ToolCallback[] from = ToolCallbacks.from(new CodeTools());
        for (ToolCallback toolCallback : from) {
            System.out.println("----------------------------------------");
            System.out.println(toolCallback.getToolDefinition().description());
            System.out.println(toolCallback.getToolDefinition().inputSchema());
            System.out.println(toolCallback.getToolMetadata());
        }

    }
    @Test
    void listAllMcpTools() {
        if (mcpAsyncClients == null || mcpAsyncClients.isEmpty()) {
            System.out.println("没有检测到 MCP 客户端连接");
            return;
        }

        System.out.println("========== 开始扫描 MCP 工具 ==========");

        for (McpAsyncClient client : mcpAsyncClients) {
            try {
                // 阻塞式获取工具列表（仅用于测试打印）
                var result = client.listTools(null).block();

                if (result != null && result.tools() != null) {
                    result.tools().forEach(tool -> {
                        System.out.println("--------------------------------------------------");
                        System.out.println("工具名称: " + tool.name());
                        System.out.println("工具描述: " + tool.description());
                        System.out.println("参数结构: " + tool.inputSchema());
                        System.out.println("--------------------------------------------------");
                    });
                }
            } catch (Exception e) {
                System.err.println("获取工具列表失败: " + e.getMessage());
            }
        }

        System.out.println("========== 扫描结束 ==========");
    }
    @Test
    public void test2(){
        McpAsyncClient client = mcpAsyncClients.get(0);
        Map<String,Object> map = new HashMap<>();
        map.put("stationNames","徐州|南京");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("get-station-code-by-names", map);
        McpSchema.CallToolResult result = client.callTool(request).block();
        System.out.println(result.content());
    }
    @Test
    public void test1(){
        /*
        1、12306
        2、fly
        3、高德
         */
        if (mcpAsyncClients == null || mcpAsyncClients.isEmpty()) {
            System.out.println("No MCP clients connected.");
            return;
        }
        McpAsyncClient client = mcpAsyncClients.get(0);
        Map<String,Object> map = new HashMap<>();

        // 查询 2026-01-17 的所有类型车票 (G=高铁, D=动车, Z=直达, T=特快, K=快速, O=其他)
        // 想要同时查询高铁和普通火车，只需要将对应的标志位拼接在 trainFilterFlags 即可
        // 例如 "G" 查高铁，"K" 查快速，"GK" 就同时查高铁和快速
        map.put("date","2026-01-24");
        map.put("fromStation","XCH");
        map.put("toStation","NJH");
        map.put("trainFilterFlags","ZTKO");
        map.put("format","json");
        
        try {
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("get-tickets", map);
            McpSchema.CallToolResult result = client.callTool(request).block();
            System.out.println("Result content: " + result.content());
            
            // result.content() 返回的是 List<Content>，其中 Content 可能是 TextContent
            // TextContent 的 toString() 可能会包含 "TextContent[...]" 这样的包裹信息，导致 JSON 解析失败
            // 应该先判断类型，然后调用 text() 方法获取纯 JSON 字符串
            var contentList = result.content();
            if (contentList != null && !contentList.isEmpty()) {
                var firstContent = contentList.get(0);
                if (firstContent instanceof io.modelcontextprotocol.spec.McpSchema.TextContent) {
                    String jsonContent = ((io.modelcontextprotocol.spec.McpSchema.TextContent) firstContent).text();
                    System.out.println("Raw JSON Content: " + jsonContent);
                    
                    JSONArray tickets = JSON.parseArray(jsonContent);

                    // 过滤出真正符合日期的车次
                    List<Object> filteredTickets = tickets.stream()
                            .filter(ticket -> {
                                JSONObject t = (JSONObject) ticket;
                                // 只保留 start_date 等于查询日期的车次
                                return "2026-01-17".equals(t.getString("start_date"));
                            })
                            .collect(Collectors.toList());

                    System.out.println("Filtered Tickets: " + JSON.toJSONString(filteredTickets));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void test3(){
        /*
        1、12306
        2、fly
        3、高德
         */
        if (mcpAsyncClients == null || mcpAsyncClients.isEmpty()) {
            System.out.println("No MCP clients connected.");
            return;
        }
        McpAsyncClient client = mcpAsyncClients.get(0);
        Map<String,Object> map = new HashMap<>();

        // 查询 2026-01-17 的所有类型车票 (G=高铁, D=动车, Z=直达, T=特快, K=快速, O=其他)
        // 想要同时查询高铁和普通火车，只需要将对应的标志位拼接在 trainFilterFlags 即可
        // 例如 "G" 查高铁，"K" 查快速，"GK" 就同时查高铁和快速
        map.put("date","2026-08-30");
        map.put("fromStation","XCH");
        map.put("toStation","NJH");
        map.put("showWZ",true);
        map.put("trainFilterFlags","GDZTKOFS");
        map.put("format","json");

        try {
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("get-interline-tickets", map);
            McpSchema.CallToolResult result = client.callTool(request).block();
            System.out.println("Result content: " + result.content());

            // result.content() 返回的是 List<Content>，其中 Content 可能是 TextContent
            // TextContent 的 toString() 可能会包含 "TextContent[...]" 这样的包裹信息，导致 JSON 解析失败
            // 应该先判断类型，然后调用 text() 方法获取纯 JSON 字符串
            var contentList = result.content();
            if (contentList != null && !contentList.isEmpty()) {
                var firstContent = contentList.get(0);
                if (firstContent instanceof io.modelcontextprotocol.spec.McpSchema.TextContent) {
                    String jsonContent = ((io.modelcontextprotocol.spec.McpSchema.TextContent) firstContent).text();
                    System.out.println("Raw JSON Content: " + jsonContent);

                    JSONArray tickets = JSON.parseArray(jsonContent);

                    // 过滤出真正符合日期的车次
                    List<Object> filteredTickets = tickets.stream()
                            .filter(ticket -> {
                                JSONObject t = (JSONObject) ticket;
                                // 只保留 start_date 等于查询日期的车次
                                return "2026-01-17".equals(t.getString("start_date"));
                            })
                            .collect(Collectors.toList());

                    System.out.println("Filtered Tickets: " + JSON.toJSONString(filteredTickets));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void test4(){
        /*
        1、12306
        2、fly
        3、高德
         */
        if (mcpAsyncClients == null || mcpAsyncClients.isEmpty()) {
            System.out.println("No MCP clients connected.");
            return;
        }
        McpAsyncClient client = mcpAsyncClients.get(1);
        Map<String,Object> map = new HashMap<>();

        // 查询 2026-01-17 的所有类型车票 (G=高铁, D=动车, Z=直达, T=特快, K=快速, O=其他)
        // 想要同时查询高铁和普通火车，只需要将对应的标志位拼接在 trainFilterFlags 即可
        // 例如 "G" 查高铁，"K" 查快速，"GK" 就同时查高铁和快速
        /*map.put("depcity", "BJS");depCityCode
        map.put("arrcity", "SHA");
        map.put("date", "2026-01-28");*/

        map.put("depCityCode", "BJS");
        map.put("arrCityCode", "SHA");
        map.put("depDate", "2026-01-28");

        /*map.put("dep", "PKX");
        map.put("arr", "SHA");
        map.put("date", "2026-01-28");*/
        //map.put("format","json");

        /*map.put("fnum", "HO1254");
        map.put("date", "2026-01-23");*/


        try { //searchFlightItineraries     searchFlightsByDepArr
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("searchFlightItineraries", map);
            McpSchema.CallToolResult result = client.callTool(request).block();
            System.out.println("Result content: " + result.content());

            // result.content() 返回的是 List<Content>，其中 Content 可能是 TextContent
            // TextContent 的 toString() 可能会包含 "TextContent[...]" 这样的包裹信息，导致 JSON 解析失败
            // 应该先判断类型，然后调用 text() 方法获取纯 JSON 字符串
            var contentList = result.content();
            if (contentList != null && !contentList.isEmpty()) {
                var firstContent = contentList.get(0);
                if (firstContent instanceof io.modelcontextprotocol.spec.McpSchema.TextContent) {
                    String jsonContent = ((io.modelcontextprotocol.spec.McpSchema.TextContent) firstContent).text();
                    System.out.println("Raw JSON Content: " + jsonContent);

                    /*JSONArray tickets = JSON.parseArray(jsonContent);

                    // 过滤出真正符合日期的车次
                    List<Object> filteredTickets = tickets.stream()
                            .filter(ticket -> {
                                JSONObject t = (JSONObject) ticket;
                                // 只保留 start_date 等于查询日期的车次
                                return "2026-01-17".equals(t.getString("start_date"));
                            })
                            .collect(Collectors.toList());

                    System.out.println("Filtered Tickets: " + JSON.toJSONString(filteredTickets));*/
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void test5(){
        if (mcpAsyncClients == null || mcpAsyncClients.isEmpty()) {
            System.out.println("No MCP clients connected.");
            return;
        }
        McpAsyncClient client = mcpAsyncClients.get(2);
        Map<String,Object> map = new HashMap<>();

        //map.put("keywords","景区");
        map.put("keywords","民宿|酒店");
        map.put("city","徐州");
        map.put("offset","10");
        //map.put("types", "风景名胜");
        map.put("types", "住宿");
        try {
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("maps_text_search", map);
            McpSchema.CallToolResult result = client.callTool(request).block();
            System.out.println("Result content: " + result.content());

            var contentList = result.content();
            if (contentList != null && !contentList.isEmpty()) {
                var firstContent = contentList.get(0);
                if (firstContent instanceof io.modelcontextprotocol.spec.McpSchema.TextContent) {
                    String jsonContent = ((io.modelcontextprotocol.spec.McpSchema.TextContent) firstContent).text();
                    System.out.println("Raw JSON Content: " + jsonContent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void test6(){
        String amapKey = "9bed5ea5759acfd7d3f88ea190d5aa5a";
        String types = "风景名胜";
        String city = "徐州";
        String url = String.format(
                "https://restapi.amap.com/v3/place/text?key=%s&types=%scity=%s",
                amapKey, types, city
        );
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        JSONObject json = JSON.parseObject(response);
        System.out.println(json.toJSONString());
    }


}