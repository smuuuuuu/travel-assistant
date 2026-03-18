package com.itbaizhan.travel_trip_service.entity;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

public class ChangeTargets {
    // 存储所有模块的开关状态，key=module_key, value=true/false
    private final Map<String, Boolean> targets = new HashMap<>();

    // 使用 Jackson 的 @JsonAnySetter 自动将 JSON 中的所有字段映射到 Map 中
    // 这样 AI 返回 {"playground": true, "transport": false} 都能自动装进去
    @JsonAnySetter
    public void set(String key, Boolean value) {
        targets.put(key, value);
    }

    // 提供便捷方法判断某个模块是否开启
    public boolean isEnabled(String key) {

        return targets.getOrDefault(key, false);
    }
    public boolean isAllBoolean(boolean value){
        return targets.containsValue(value);
    }
    
    // 如果需要转 JSON 字符串（用于日志或回显），可以加 @JsonAnyGetter
    @JsonAnyGetter
    public Map<String, Boolean> getTargets() {
        return targets;
    }
}