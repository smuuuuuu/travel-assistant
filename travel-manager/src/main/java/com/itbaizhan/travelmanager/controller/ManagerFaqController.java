package com.itbaizhan.travelmanager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.TravelFaq;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelmanager.service.ManagerFaqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/manager/faq")
public class ManagerFaqController {
    @Autowired
    private ManagerFaqService managerFaqService;

    @GetMapping("/page")
    public BaseResult<Page<TravelFaq>> page(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size) {
        return BaseResult.success(managerFaqService.page(page, size));
    }
    @GetMapping("/search")
    public BaseResult<List<TravelFaq>> search(@RequestParam(required = false) String query,
                                             @RequestParam(required = false) String city,
                                             @RequestParam(defaultValue = "5") Integer k) {
        if(!StringUtils.hasText(query) && !StringUtils.hasText(city)){
            throw new BusException(CodeEnum.MANAGER_FAQ_SEARCH_ERROR);
        }
        return BaseResult.success(managerFaqService.search(query, city, k));
    }
    @PostMapping("/insert")
    public BaseResult<?> insert(@RequestBody TravelFaq faq) {
        managerFaqService.add(faq);
        return BaseResult.success();
    }
    @PutMapping("/update")
    public BaseResult<?> update(@RequestBody TravelFaq faq) {
        managerFaqService.update(faq);
        return BaseResult.success();
    }
    @DeleteMapping("/delete")
    public BaseResult<?> delete(@RequestParam String id) {
        managerFaqService.delete(Arrays.stream(id.split(",")).toList());
        return BaseResult.success();
    }
    @PutMapping("/status")
    public BaseResult<?> status(@RequestParam String ids,@RequestParam Integer status) {
        managerFaqService.status(Arrays.stream(ids.split(",")).toList(), status);
        return BaseResult.success();
    }
    @RequestMapping("/upload")
    public BaseResult<?> upload(MultipartFile[] files) {
        managerFaqService.upload(files);
        return BaseResult.success();
    }
}
