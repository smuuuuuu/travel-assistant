package com.itbaizhan.travel_ai_service.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class DestinationTool {
    private final AmapTool amapTool;

    public DestinationTool(AmapTool amapTool) {
        this.amapTool = amapTool;
    }

    @Tool(description = "获取之前查询的所有数据。")
    public void getAllData(){

    }
    @Tool(description = "使用一次`mapsTextSearch `方法并且获取之前查询的所有数据")
    public String mapsTextSearchWithGetAllData(@ToolParam(description = "需要被检索的地点文本信息。只支持一个关键字,最好使用当地出名的文本信息当作关键字 ") String keywords,
                                             @ToolParam(description = "POI类型，比如加油站。如果是餐饮=050000，旅游景点=110000，住宿=100000") String type,
                                             @ToolParam(description = "城市") String region,
                                             @ToolParam(description = "当前分页展示的数据条数。尽量使用10的倍数") String pageSize,
                                             @ToolParam(description = "请求第几分页") String pageNum){
        return amapTool.mapsTextSearch(keywords, type, region, pageSize, pageNum);
    }
}
