package com.itbaizhan.travel_ai_service.util;

import java.util.Map;

public class PromptUtil {
    private PromptUtil() {
    }

    /**
     * 渲染提示词模板：将 {{KEY}} 替换为变量值。
     *
     * @param template  模板文本
     * @param variables 变量键值对
     * @return 渲染后的文本
     */
    public static String renderPromptTemplate(String template,String key, Integer variables) {
        String out = template == null ? "" : template;
        if (variables == null) return out;
        return out.replace("{{" + key + "}}", String.valueOf(variables));
    }
}