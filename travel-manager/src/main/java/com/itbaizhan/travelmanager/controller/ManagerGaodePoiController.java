package com.itbaizhan.travelmanager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itbaizhan.travelcommon.pojo.GaodePoi;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.service.GaodePoiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/manager/gaodePoi")
public class ManagerGaodePoiController {
    @Autowired
    private GaodePoiService gaodePoiService;

    @GetMapping("/getListByPage")
    public BaseResult<Page<GaodePoi>> getListByPage(@RequestParam int pageNo,@RequestParam int pageSize) {
        return BaseResult.success(gaodePoiService.getGaodePoiPage(pageNo, pageSize));
    }
    @GetMapping("/getListByType")
    public BaseResult<Page<GaodePoi>> getListByType(@RequestParam int type,@RequestParam int pageNo,@RequestParam int pageSize) {
        return BaseResult.success(gaodePoiService.getGaodePoiPageByType(type,pageNo, pageSize));
    }
    @PostMapping("/insert")
    public BaseResult<?> insertGaodePoi(@RequestBody GaodePoi gaodePoi) {
        gaodePoiService.insertGaodePoi(gaodePoi);
        return BaseResult.success();
    }
    @PutMapping("/update")
    public BaseResult<?> updateGaodePoi(@RequestBody GaodePoi gaodePoi) {
        gaodePoiService.updateGaodePoi(gaodePoi);
        return BaseResult.success();
    }
    @DeleteMapping("/delete")
    public BaseResult<?> deleteGaodePoi(@RequestParam String ids) {
        gaodePoiService.deleteGaodePoi(Arrays.stream(ids.split(",")).map(Long::parseLong).toList());
        return BaseResult.success();
    }
}
