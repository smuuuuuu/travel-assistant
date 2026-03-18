package com.itbaizhan.travel_ai_service.tools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class AmapTool {
    private final RestTemplate restTemplate;

    public AmapTool() {
        this.restTemplate = new RestTemplate();
    }

    public static final String url = "https://restapi.amap.com/v5/place/text?" +
            "keywords=%s&types=%s&region=%s&key=%s&page_size=%s&page_num=%s&show_fields=children,business,navi,photos";
    private static final Integer PAGE_SIZE = 20;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Value("${amap.web.key}")
    private String amapKey;
    @Value("${redis.amaps}")
    private String amaps;

    public String mapsTextSearch(String keywords, String type, String region,String pageSize, String pageNum) {
        if(Integer.parseInt(pageSize) % 10 != 0){
            JSONArray jsonArray = this.getAmaps(keywords, type, region, pageSize, pageNum);
            return jsonArray.toString();
        }
        String key = amaps + ":" + type + ":" + region + ":" + keywords;
        int interval = (Integer.parseInt(pageNum) - 1) * Integer.parseInt(pageSize) / 10;
        int size = Integer.parseInt(pageSize) / 10;
        StringBuilder builder = new StringBuilder();
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        for(int i = 0;i < size;i++) {
            int current = interval + i;
            String field = this.getField(current);
            if(redisTemplate.opsForHash().hasKey(key, field)) {
                Object object = redisTemplate.opsForHash().get(key, field);
                builder.append(JSON.toJSONString(object)).append("\n");
            }else {
                if(map.containsKey(current - 1)) {
                    map.compute(current - 1, (k, pre) -> pre + 10);
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
                redisTemplate.opsForHash().put(key, this.getField(i), string);
            }else {
                index = i;
                count = 1;
                String s1 = "[" + string.trim() + "]";
                JSONArray jsonArray = JSON.parseArray(s1);
                sb.setLength(0);
                for (Object o : jsonArray) {
                    if (count >= 10) {
                        redisTemplate.opsForHash().put(key, this.getField(index), sb.toString());
                        sb.setLength(0);
                        index++;
                    }
                    sb.append(JSON.toJSONString(o));
                    count++;
                }
            }
        }
        return replaceStr;
    }

    private Map<Integer,String> mapToString(Map<Integer,Integer> map,String keywords, String type, String region) {
        Map<Integer,String> replace = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        Map<Integer,Integer> record = new HashMap<>();
        map.forEach((k,v)->{
            list.add(k);
        });
        int index = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            Integer key1 = entry.getKey();
            Integer value = entry.getValue();
            int n,m,start,end = 0;
            int total = 0;
            int skip = key1 * 10;
            int summarize = skip + value;
            StringBuilder stringBuilder = new StringBuilder();
            if(value < PAGE_SIZE){
                if(summarize % value == 0){
                    n = (summarize / value);
                    JSONArray jsonArray = this.getAmaps(keywords, type, region, n, value);
                    replace.put(key1,jsonArray.toString().substring(1, jsonArray.size() - 1));
                    index++;
                    continue;
                }
            }
            end = summarize / PAGE_SIZE;
            if(summarize % PAGE_SIZE != 0){
                end++;
            }
            total = end * PAGE_SIZE;
            start = key1 / PAGE_SIZE + 1;
            JSONArray jsonArray = new JSONArray();
            for(int i = start; i <= end; i++) {
                jsonArray.addAll(this.getAmaps(keywords,type,region,i,PAGE_SIZE));
            }
            m = key1 % PAGE_SIZE;
            for(int j = m; j < m + value; j++) {
                Object object = jsonArray.get(j);
                stringBuilder.append(JSON.toJSONString(object)).append("\n");
            }
            if(replace.containsKey(key1)) {
                replace.put(key1,replace.get(key1) + stringBuilder.toString());
            }else {
                replace.put(key1,stringBuilder.toString());
            }

            Integer nextKey = list.get(index + 1);
            Integer nextValue = map.get(nextKey);
            if(total > (nextKey * 10) && total - (nextKey * 10) % 10 == 0){
                stringBuilder.setLength(0);
                skip = nextKey * 10;
                int obtain = total - skip;
                for(int k = jsonArray.size() - obtain; k < jsonArray.size(); k++) {
                    Object object = jsonArray.get(k);
                    stringBuilder.append(JSON.toJSONString(object)).append("\n");
                }
                int replaceKey = nextKey + obtain / 10;
                replace.put(replaceKey,stringBuilder.toString());
                map.remove(nextKey);
                if (obtain < nextValue) {
                    map.put(replaceKey, nextValue - obtain);
                }else {
                    index++;
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
