package com.itbaizhan.travel_ai_service.service;

import com.itbaizhan.travelcommon.service.SensitiveWordService;
import com.itbaizhan.travelcommon.util.SensitiveTextUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@DubboService
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Autowired
    private SensitiveTextUtil sensitiveTextUtil;

    @Override
    public void insert(List<String> word) {
        sensitiveTextUtil.add(word);
    }

    @Override
    public void delete(List<String> word) {
        sensitiveTextUtil.delete(word);
    }
}
