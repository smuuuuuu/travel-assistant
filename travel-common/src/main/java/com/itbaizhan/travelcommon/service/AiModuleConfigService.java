package com.itbaizhan.travelcommon.service;

import com.itbaizhan.travelcommon.pojo.AiModuleConfig;

import java.util.List;
import java.util.Map;

public interface AiModuleConfigService {

    List<AiModuleConfig> getAiModuleConfig();

    List<AiModuleConfig> getAiModuleToolEnable();
    String getPoiTypeWithAiModuleId(Map<String,Boolean> targets);

    List<AiModuleConfig> getAllPoiTypeAiModule();
}
