package com.itbaizhan.travelmanager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.AiMessageDetail;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.service.ManagerMessageTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/messages")
public class ManagerMessageController {
    @Autowired
    private ManagerMessageTokenService managerMessageTokenService;

    @GetMapping
    public BaseResult<Page<AiMessageDetail>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tripId,
            @RequestParam(required = false) Integer isAgent) {
        Page<AiMessageDetail> p = new Page<>(current, size);
        return BaseResult.success(managerMessageTokenService.pageMessageDetails(p, userId, tripId,isAgent));
    }
    @GetMapping("/summary")
    public BaseResult<Long> summary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tripId,
            @RequestParam(required = false) Integer isAgent) {
        long sum = managerMessageTokenService.sumMessageDetail(userId, tripId,isAgent);
        return BaseResult.success(sum);
    }
}
