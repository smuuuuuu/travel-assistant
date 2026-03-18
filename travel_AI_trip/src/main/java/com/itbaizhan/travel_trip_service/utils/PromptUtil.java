package com.itbaizhan.travel_trip_service.utils;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PromptUtil {
    private PromptUtil() {}

    /**
     * 渲染提示词模板：将 {{KEY}} 替换为变量值。
     * @param template 模板文本
     * @param variables 变量键值对
     * @return 渲染后的文本
     */
    public static String renderPromptTemplate(String template, Map<String, String> variables) {
        String out = template == null ? "" : template;
        if (variables == null || variables.isEmpty()) return out;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) continue;
            String value = entry.getValue() == null ? "" : entry.getValue();
            out = out.replace("{{" + key + "}}", value);
        }
        return out;
    }



    public final static String ACCOMMODATION_PROMPT = "prompts/accommodation_rules.txt";
    public final static String PRECHECK_MODIFY = "prompts/modify_precheck_prompt.txt";
    public final static String PRECHECK_GENERATE = "prompts/precheck_generation_prompt.txt";
    public final static String ARGHINT_PROMPT = "prompts/argHint.txt";
    public final static String QUOTAHINT_PROMPT = "prompts/quotaHint.txt";

    public final static String TRANSPORT_PROMPT = "prompts/transport_rules.txt";

    public final static String CATERING_PROMPT = "prompts/poi_dining.txt";
    public final static String SEARCH_PROMPT = "prompts/poi_search_prompt.txt";
    public final static String SCENIC_PROMPT = "prompts/poi_attraction.txt";
    public final static String GENERATE_MD = "prompts/generateMd.txt";

    public final static String MODIFY_PROMPT = "prompts/modify_plan_prompt.txt";

    public final static String GENERATE_PROMPT = "prompts/base_role_generate.txt";

    public final static String TOOL_RESTRICTION_PROMPT = "prompts/tool_restriction_prompt.txt";

    public static String getPrompt(String path) {
        if(path == null) {
            return null;
        }
        try( InputStream in = PromptUtil.class.getClassLoader().getResourceAsStream(path)){
            if (in == null) {
                throw new IllegalStateException("未找到提示词资源文件: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }catch (IOException e) {
            throw new IllegalStateException("读取提示词资源文件失败: " + path, e);
        }

    }
    /*public static String getAllToolPrompt() {
        ClassLoader classLoader = PromptUtil.class.getClassLoader();
        StringBuilder prompt = new StringBuilder();
        try(InputStream accommodation = classLoader.getResourceAsStream(ACCOMMODATION_PROMPT);
            InputStream transport = classLoader.getResourceAsStream(TRANSPORT_PROMPT);
            //InputStream poi = classLoader.getResourceAsStream(POI_PROMPT);
            InputStream tool = classLoader.getResourceAsStream(TOOL_RESTRICTION_PROMPT);){
            if (tool == null || accommodation == null || transport == null || poi == null) {
                throw new IllegalStateException("未找到提示词资源文件");
            }
            prompt.append(new String(tool.readAllBytes(), StandardCharsets.UTF_8));
            prompt.append("\n");
            prompt.append(new String(accommodation.readAllBytes(), StandardCharsets.UTF_8));
            prompt.append("\n");
            prompt.append(new String(transport.readAllBytes(), StandardCharsets.UTF_8));
            prompt.append("\n");
            return prompt.append(new String(poi.readAllBytes(), StandardCharsets.UTF_8)).toString();
        }catch (IOException e) {
            throw new IllegalStateException("读取提示词资源文件失败: ", e);
        }
    }*/
}
