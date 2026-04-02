package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.AiMessageDetail;
import com.itbaizhan.travelmanager.mapper.AiMessageDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ManagerMessageTokenService {
    @Autowired
    private AiMessageDetailMapper aiMessageDetailMapper;

    public Page<AiMessageDetail> pageMessageDetails(Page<AiMessageDetail> page, Long userId, String tripId,Integer isAgent) {
        QueryWrapper<AiMessageDetail> queryWrapper = new QueryWrapper<>();
        if(userId != null) {
            queryWrapper.eq("user_id", userId);
        }
        if(StringUtils.hasText(tripId)) {
            queryWrapper.eq("trip_id", tripId);
        }
        if(isAgent != null) {
            queryWrapper.eq("is_agent", isAgent);
        }
        queryWrapper.orderByDesc("use_token");
        return aiMessageDetailMapper.selectPage(page,queryWrapper);
    }

    public Long sumMessageDetail(Long userId, String tripId,Integer isAgent) {
        QueryWrapper<AiMessageDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("IFNULL(SUM(use_token),0) AS total");
        if(userId != null) {
            queryWrapper.eq("user_id", userId);
        }
        if(StringUtils.hasText(tripId)) {
            queryWrapper.eq("trip_id", tripId);
        }
        if (isAgent != null) {
            queryWrapper.eq("is_agent", isAgent);
        }
        return aiMessageDetailMapper.selectMaps(queryWrapper).get(0).get("total") == null ? 0L : Long.parseLong(aiMessageDetailMapper.selectMaps(queryWrapper).get(0).get("total").toString());
    }
}
