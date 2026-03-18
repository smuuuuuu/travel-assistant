/*
package com.itbaizhan.travel_trip_service.init;

import com.itbaizhan.travel_trip_service.entity.mongo.AiPrompt;
import com.itbaizhan.travel_trip_service.repository.mongo.AiPromptRepository;
import com.itbaizhan.travel_trip_service.utils.PromptUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

*/
/**
 * 初始化 AI Prompt 数据到 MongoDB
 *//*

@SpringBootTest
public class PromptInitTest {

    @Autowired
    private AiPromptRepository aiPromptRepository;

    @Test
    public void initPrompts() {
        // 定义需要初始化的 Prompt 列表 (对应 PromptUtil 中的常量)
        String[] prompts = {
            */
/*PromptUtil.ACCOMMODATION_PROMPT,
            PromptUtil.TRANSPORT_PROMPT,
            PromptUtil.CATERING_PROMPT,
                PromptUtil.SCENIC_PROMPT,
            PromptUtil.MODIFY_PROMPT,
            PromptUtil.TOOL_RESTRICTION_PROMPT,
                PromptUtil.SEARCH_PROMPT,
                PromptUtil.PRECHECK_GENERATE,
                PromptUtil.PRECHECK_MODIFY,
                PromptUtil.ARGHINT_PROMPT,
                PromptUtil.QUOTAHINT_PROMPT,
                PromptUtil.GENERATE_MD,*//*

                PromptUtil.GENERATE_PROMPT,
        };
        String[] sce = {
                */
/*"prompt_accommodation",
                "prompt_transport",
                "prompt_catering",
                "prompt_scenic",
                "prompt_modify",
                "prompt_tool_restriction",
                "prompt_search",
                "prompt_precheck_generate",
                "prompt_precheck_modify",
                "prompt_arghint",
                "prompt_quotahint",*//*

                "prompt_generate",
        };

        int i = 0;
        for (String scene : prompts) {
            try {
                // 1. 读取文件内容
                String content = PromptUtil.getPrompt(scene);
                if (content == null || content.isBlank()) {
                    System.out.println("Skipping empty prompt: " + scene);
                    continue;
                }

                // 2. 查找是否已存在
                AiPrompt prompt = aiPromptRepository.findByScene(scene).orElse(new AiPrompt());
                
                // 3. 设置/更新属性
                prompt.setScene(sce[i++]);
                prompt.setContent(content);
                prompt.setIsActive(true);
                prompt.setVersion("v1.0.0"); // 初始版本
                prompt.setDescription("Initialized from file: " + scene);
                
                if (prompt.getId() == null) {
                    prompt.setCreatedAt(LocalDateTime.now());
                }
                prompt.setUpdatedAt(LocalDateTime.now());

                // 4. 保存到 MongoDB
                aiPromptRepository.save(prompt);
                System.out.println("Successfully initialized prompt: " + scene);

            } catch (Exception e) {
                System.err.println("Failed to initialize prompt: " + scene);
                e.printStackTrace();
            }
        }
        */
/*AiPrompt prompt = aiPromptRepository.findByScene(PromptUtil.MONGO_ALL_MODIFY_PROMPT).orElse(new AiPrompt());

        // 3. 设置/更新属性
        prompt.setScene(PromptUtil.MONGO_ALL_MODIFY_PROMPT);
        prompt.setContent(PromptUtil.getAllToolPrompt());
        prompt.setIsActive(true);
        prompt.setVersion("v1.0.0"); // 初始版本
        prompt.setDescription("Initialized from file: " + PromptUtil.MONGO_ALL_MODIFY_PROMPT);
        if (prompt.getId() == null) {
            prompt.setCreatedAt(LocalDateTime.now());
        }
        prompt.setUpdatedAt(LocalDateTime.now());

        // 4. 保存到 MongoDB
        aiPromptRepository.save(prompt);
        System.out.println("Successfully initialized prompt: " + PromptUtil.MONGO_ALL_MODIFY_PROMPT);*//*

    }

    @Test
    public void test2(){
        aiPromptRepository.findAll().forEach(System.out::println);
    }
}
*/
