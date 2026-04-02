package com.itbaizhan.travelcommon.service;

import com.itbaizhan.travelcommon.pojo.AiModuleConfig;

import java.util.List;

public interface PromptSelectService {
    String getToolRestriction();
    String getAllGaodeType();
    String getAllGenPrompt();
    String getAllTools();

    String getPrompt(String scene);
}
