package com.itbaizhan.travelmanager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.AiTripToken;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.service.AiTripTokenService;
import com.itbaizhan.travelmanager.dto.TripTokenSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/tripTokens")
@RequiredArgsConstructor
public class ManagerTripTokenController {

    private final AiTripTokenService aiTripTokenService;

    @GetMapping
    public BaseResult<IPage<AiTripToken>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tripId,
            @RequestParam(required = false) Integer type) {
        Page<AiTripToken> p = new Page<>(current, size);
        return BaseResult.success(aiTripTokenService.pageForAdmin(p, userId, tripId, type));
    }

    @GetMapping("/summary")
    public BaseResult<TripTokenSummaryResponse> summary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tripId,
            @RequestParam(required = false) Integer type) {
        long sum = aiTripTokenService.sumUseTokenForAdmin(userId, tripId, type);
        return BaseResult.success(new TripTokenSummaryResponse(sum));
    }
}
