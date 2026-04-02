package com.itbaizhan.travelmanager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.TravelSensitiveWord;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.service.ManagerSensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/manager/sensitive")
public class ManagerSensitiveController {
    @Autowired
    private ManagerSensitiveWordService managerSensitiveWordService;

    @GetMapping("/list")
    public BaseResult<Page<TravelSensitiveWord>> getSensitiveWords(@RequestParam Integer pageNo,
                                                                   @RequestParam Integer pageSize) {
        Page<TravelSensitiveWord> sensitiveWords = managerSensitiveWordService.getSensitiveWords(pageNo, pageSize);
        return BaseResult.success(sensitiveWords);
    }
    @PostMapping("/add")
    public BaseResult<?> addSensitiveWord(@RequestBody TravelSensitiveWord sensitiveWord) {
        managerSensitiveWordService.insert(sensitiveWord);
        return BaseResult.success();
    }
    @DeleteMapping("/delete")
    public BaseResult<?> deleteSensitiveWord(@RequestParam String ids) {
        managerSensitiveWordService.delete(Arrays.stream(ids.split(",")).map(Long::valueOf).toList());
        return BaseResult.success();
    }
    @PutMapping("/status")
    public BaseResult<?> updateSensitiveWordStatus(@RequestParam String ids, @RequestParam Integer status) {
        managerSensitiveWordService.status(Arrays.stream(ids.split(",")).map(Long::valueOf).toList(), status);
        return BaseResult.success();
    }
}
