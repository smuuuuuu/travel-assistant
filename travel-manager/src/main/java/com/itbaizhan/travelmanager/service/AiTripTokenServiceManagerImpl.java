package com.itbaizhan.travelmanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.AiTripToken;
import com.itbaizhan.travelcommon.service.AiTripTokenService;
import com.itbaizhan.travelmanager.mapper.AiTripTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * travel-manager 进程内实现，直连与用户行程服务相同库表。
 */
@Service
@Primary
@RequiredArgsConstructor
public class AiTripTokenServiceManagerImpl implements AiTripTokenService {

    private final AiTripTokenMapper aiTripTokenMapper;


    @Override
    public IPage<AiTripToken> pageForAdmin(Page<AiTripToken> page, Long userId, String tripId, Integer type) {
        QueryWrapper<AiTripToken> q = new QueryWrapper<>();
        if (userId != null) {
            q.eq("userId", userId);
        }
        if (StringUtils.hasText(tripId)) {
            q.eq("trip_id", tripId);
        }
        if (type != null) {
            q.eq("type", type);
        }
        q.orderByDesc("use_token");
        return aiTripTokenMapper.selectPage(page, q);
    }

    @Override
    public long sumUseTokenForAdmin(Long userId, String tripId, Integer type) {
        QueryWrapper<AiTripToken> q = new QueryWrapper<>();
        q.select("IFNULL(SUM(use_token),0) AS total");
        if (userId != null) {
            q.eq("userId", userId);
        }
        if (StringUtils.hasText(tripId)) {
            q.eq("trip_id", tripId);
        }
        if (type != null) {
            q.eq("type", type);
        }
        List<Map<String, Object>> maps = aiTripTokenMapper.selectMaps(q);
        if (maps.isEmpty() || maps.get(0).get("total") == null) {
            return 0L;
        }
        Object total = maps.get(0).get("total");
        return total instanceof Number ? ((Number) total).longValue() : 0L;
    }
}
