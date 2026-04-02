package com.itbaizhan.travelmanager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itbaizhan.travelcommon.managerDto.GaoDeAiModuleDto;
import com.itbaizhan.travelcommon.pojo.AiModuleConfig;
import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelmanager.service.AiModuleConfigAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manger/aiModuleConfig")
@RequiredArgsConstructor
public class ManagerAiModuleConfigController {

    private final AiModuleConfigAdminService aiModuleConfigAdminService;

    /**
     * 分页查找AiModuleConfig
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/getListByPage")
    public BaseResult<IPage<AiModuleConfig>> getListByPage(@RequestParam int pageNo, @RequestParam int pageSize) {
        return BaseResult.success(aiModuleConfigAdminService.getListByPage(pageNo,pageSize));
    }

    /**
     * 获取所有的高德AiModuleConfig
     * @return AiModuleConfig + type + name
     */
    @GetMapping("/getGaoDe")
    public BaseResult<List<GaoDeAiModuleDto>> getGaoDeAiModuleList(){
        return BaseResult.success(aiModuleConfigAdminService.getGaoDeAiModuleList());
    }

    /**
     * 开启/关闭AiModuleConfig
     * @param id id
     * @return void
     */
    @PutMapping("/enable/{id}")
    public BaseResult<?> enable(@PathVariable Long id){
        aiModuleConfigAdminService.enable(id);
        return BaseResult.success();
    }

    /**
     * 创建需要promptId，先保存提示词获取promptId
     * @param body AiModuleConfig
     * @return AiModuleConfig
     */
    @PostMapping
    public BaseResult<AiModuleConfig> create(@RequestBody AiModuleConfig body) {
        return BaseResult.success(aiModuleConfigAdminService.create(body));
    }

    @PutMapping
    public BaseResult<AiModuleConfig> update(@RequestBody AiModuleConfig body) {
        return BaseResult.success(aiModuleConfigAdminService.update(body));
    }

    @DeleteMapping("/{id}")
    public BaseResult<Void> delete(@PathVariable Long id) {
        aiModuleConfigAdminService.delete(id);
        return BaseResult.success();
    }
}
