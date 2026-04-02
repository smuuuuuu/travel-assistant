package com.itbaizhan.travelmanager.controller;

import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.service.HotStyleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manager/hot")
public class HotStyleController {
    @Autowired
    private HotStyleService hotStyleService;

    @GetMapping
    public BaseResult<String> hotStyle() {
        return BaseResult.success(hotStyleService.getHotStyle());
    }

    @PutMapping
    public BaseResult<?> updateHotStyle(@RequestParam String hotStyle) {
        hotStyleService.setHotStyle(hotStyle);
        return BaseResult.success();
    }
}
