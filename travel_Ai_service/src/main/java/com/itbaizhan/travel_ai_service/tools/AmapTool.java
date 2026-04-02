package com.itbaizhan.travel_ai_service.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class AmapTool {
    private final RestTemplate restTemplate;

    public AmapTool(RedisTemplate<String,Object> redisTemplate,String amapKey,String amaps) {
        this.restTemplate = new RestTemplate();
        this.redisTemplate = redisTemplate;
        this.amapKey = amapKey;
        this.amaps = amaps;
    }

    public static final String url = "https://restapi.amap.com/v5/place/text?" +
            "keywords=%s&types=%s&region=%s&key=%s&page_size=%s&page_num=%s&show_fields=children,business,navi,photos";
    private static final Integer PAGE_SIZE = 25;

    private final RedisTemplate<String,Object> redisTemplate;

    private final String amapKey;

    private final String amaps;
    @Tool(description = "关键词搜，根据用户传入关键词，搜索出相关的POI，参数只能使用中文。尽量优先使用这个方法")
    public String mapsTextSearch(@ToolParam(description = "需要被检索的地点文本信息。只支持一个关键字,最好使用当地出名的文本信息当作关键字 ") String keywords,
                                 @ToolParam(description = "POI类型，比如加油站。如果是餐饮=050000，旅游景点=110000，住宿=100000") String type,
                                 @ToolParam(description = "城市") String region,
                                 @ToolParam(description = "当前分页展示的数据条数。尽量使用10的倍数") String pageSize,
                                 @ToolParam(description = "请求第几分页") String pageNum) {
        if(Integer.parseInt(pageSize) % 10 != 0){
            JSONArray jsonArray = this.getAmaps(keywords, type, region, pageSize, pageNum);
            if(jsonArray != null){
                return jsonArray.toString();
            }else {
                return null;
            }
        }
        String key = amaps + region + ":" + type + ":" + keywords;
        int interval = (Integer.parseInt(pageNum) - 1) * Integer.parseInt(pageSize) / 10;
        int size = Integer.parseInt(pageSize) / 10;
        StringBuilder builder = new StringBuilder();
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        int p = 1;
        for(int i = 0;i < size;i++) {
            int current = interval + i;
            String field = this.getField(current);
            if(redisTemplate.opsForHash().hasKey(key, field)) {
                Object object = redisTemplate.opsForHash().get(key, field);
                builder.append(JSON.toJSONString(object)).append("\n");
                p = 1;
            }else {
                if(map.containsKey(current - p)) {
                    map.compute(current - p, (k, pre) -> pre + 10);
                    p++;
                }else {
                    map.put(current,10);
                    builder.append("{{").append(current).append("}}").append("\n");
                }
            }
        }
        if(!map.isEmpty()){
            return handle(map,key, builder.toString(), keywords, type, region);
        }
        return builder.toString();
    }

    private String handle(Map<Integer,Integer> map,String key,String s,String keywords, String type, String region) {
        String replaceStr = s;
        Map<Integer,Integer> copy = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            Integer i = e.getKey();
            Integer value = e.getValue();
            copy.put(i, value);
        }
        StringBuilder sb = new StringBuilder();
        int index,count = 0;
        Map<Integer, String> integerStringMap = mapToString(map, keywords, type, region);
        for (Map.Entry<Integer, Integer> e : copy.entrySet()) {
            Integer i = e.getKey();
            Integer value = e.getValue();
            String string = integerStringMap.get(i);
            replaceStr = replaceStr.replace("{{" + i + "}}", string);
            if(value <= 10){
                if(StrUtil.isNotBlank(string)){
                    redisTemplate.opsForHash().put(key, this.getField(i), string);
                }
            }else {
                index = i;
                count = 1;
                String s1 = "[" + string.trim() + "]";
                JSONArray jsonArray = JSON.parseArray(s1);
                sb.setLength(0);
                for (Object o : jsonArray) {
                    if (count > 10) {
                        sb.deleteCharAt(sb.length() - 1);
                        if(!StrUtil.isNotBlank(sb.toString())){
                            redisTemplate.opsForHash().put(key, this.getField(index), sb.toString());
                        }
                        sb.setLength(0);
                        index++;
                        count = 1;
                    }
                    sb.append(JSON.toJSONString(o)).append(",");
                    count++;
                }
                if(!sb.isEmpty()){
                    redisTemplate.opsForHash().put(key, this.getField(index), sb.toString());
                }
            }
        }
        return replaceStr;
    }

    private Map<Integer,String> mapToString(Map<Integer,Integer> map,String keywords, String type, String region) {
        Map<Integer,String> replace = new HashMap<>();
        List<Integer> list = new ArrayList<>(map.keySet());
        Map<Integer,Integer> record = new HashMap<>();
        Collections.sort(list);
        int index = 0;
        //for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
        for (Integer a : list) {
            if(index >= list.size()){
                break;
            }
            if(a < 0){
                index++;
                continue;
            }
            Integer key1 = a;
            Integer value = map.get(a);
            int n,m,start,end = 0;
            int total = 0;
            int skip = key1 * 10;
            int summarize = skip + value;
            StringBuilder stringBuilder = new StringBuilder();
            if(value < PAGE_SIZE){
                if(summarize % value == 0){
                    n = (summarize / value);
                    JSONArray jsonArray = this.getAmaps(keywords, type, region, value, n);
                    if(jsonArray != null){
                        replace.put(key1,jsonArray.toString().substring(1, jsonArray.toString().length() - 1));
                    }else {
                        replace.put(key1,"");
                    }
                    index++;
                    continue;
                }
            }
            end = summarize / PAGE_SIZE;
            if(summarize % PAGE_SIZE != 0){
                end++;
            }
            total = end * PAGE_SIZE;
            start = skip / PAGE_SIZE + 1;
            JSONArray jsonArray = new JSONArray();
            for(int i = start; i <= end; i++) {
                JSONArray array = this.getAmaps(keywords, type, region, PAGE_SIZE, i);
                if(array != null){
                    jsonArray.addAll(array);
                }
            }

            m = skip % PAGE_SIZE;
            int requiredEnd = m + value - 1;
            if (jsonArray.size() <= requiredEnd) {
                replace.put(key1, "");
                index++;
                continue;
            }
            for(int j = m; j < m + value; j++) {
                Object object = jsonArray.get(j);
                stringBuilder.append(JSON.toJSONString(object)).append(",");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            if(replace.containsKey(key1)) {
                replace.put(key1,replace.get(key1) + stringBuilder.toString());
            }else {
                replace.put(key1,stringBuilder.toString());
            }
            if(index + 1 >= list.size()){
                stringBuilder.setLength(0);
                index++;
                continue;
            }
            Integer nextKey = list.get(index + 1);
            Integer nextValue = map.get(nextKey);
            if(total > (nextKey * 10) && (total - nextKey * 10) % 10 == 0){
                stringBuilder.setLength(0);
                skip = nextKey * 10;
                int obtain = total - skip;
                for(int k = jsonArray.size() - obtain; k < jsonArray.size(); k++) {
                    Object object = jsonArray.get(k);
                    stringBuilder.append(JSON.toJSONString(object)).append(",");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                int replaceKey = nextKey + obtain / 10;
                replace.put(replaceKey,stringBuilder.toString());
                map.remove(nextKey);
                if (obtain < nextValue) {
                    list.set(index + 1, replaceKey);
                    map.put(replaceKey, nextValue - obtain);
                }else {
                    list.set(index + 1, -1);
                    //index++;
                }
                record.put(nextKey,replaceKey);
            }
            stringBuilder.setLength(0);
            index++;
        }
        if(!record.isEmpty()){
            record.forEach((k,v)->{
                if(replace.containsKey(v)) {
                    replace.put(k,replace.get(v));
                    replace.remove(v);
                }
            });
        }
        return replace;
    }

    private String getField(Integer i) {
        return "page:" + i;
    }

    private JSONArray getAmaps(String keywords, String type, String region,Integer pageSize, Integer pageNum){
        return getAmaps(keywords,type,region,String.valueOf(pageSize),String.valueOf(pageNum));
    }

    private JSONArray getAmaps(String keywords, String type, String region,String pageSize, String pageNum) {
        try {
            String format = String.format(url, keywords, type, region, amapKey, pageSize, pageNum);
            String string = restTemplate.getForObject(format, String.class);
            JSONObject jsonObject = JSON.parseObject(string);
            if(jsonObject != null) {
                if("1".equals(jsonObject.getString("status"))) {
                    return jsonObject.getJSONArray("pois");
                }
            }
        } catch (RestClientException e) {
            return null;
        }
        return null;
    }
}
