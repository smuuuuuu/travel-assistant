package com.itbaizhan.travelmanager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.TripGaodeType;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.service.GaoDeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manager/gaodeType")
public class ManagerGaoDeTypeController {
    @Autowired
    private GaoDeTypeService gaoDeTypeService;

    @GetMapping("/getListByPage")
    public BaseResult<Page<TripGaodeType>> getListByPage(@RequestParam int pageNo,@RequestParam int pageSize){
        return BaseResult.success(gaoDeTypeService.getGaoDeTypeByPage(pageNo, pageSize));
    }
    @PostMapping("/insert")
    public BaseResult<?> insertGaoDeType(@RequestBody TripGaodeType gaoDeType) {
        gaoDeTypeService.insertGaoDeType(gaoDeType);
        return BaseResult.success();
    }
    @PutMapping("/update")
    public BaseResult<?> updateGaoDeType(@RequestBody TripGaodeType gaoDeType) {
        gaoDeTypeService.updateGaoDeType(gaoDeType);
        return BaseResult.success();
    }
    @DeleteMapping("/delete/{id}")
    public BaseResult<?> deleteGaoDeType(@PathVariable Long id) {
        gaoDeTypeService.deleteGaoDeType(id);
        return BaseResult.success();
    }
    @PutMapping("/updateAiModuleId")
    public BaseResult<?> updateGaoDeTypeAiModuleId(@RequestParam Long id, @RequestParam Long aiModuleId) {
        gaoDeTypeService.updateGaoDeTypeAiModuleId(id, aiModuleId);
        return BaseResult.success();
    }
}
