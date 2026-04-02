package com.itbaizhan.travelwebhooks.controller;

import com.itbaizhan.travelcommon.service.MailService;
import com.itbaizhan.travelwebhooks.dto.AlarmMessageDto;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//报警控制器
@RestController
public class WebHooksController {
    @DubboReference
    private MailService mailService;
    /**
     * skyWalking报警回调方法
     * @param alarmMessageDtoList
     */
    @PostMapping("/alarm")
    public void alarm(@RequestBody List<AlarmMessageDto> alarmMessageDtoList){
        StringBuilder builder = new StringBuilder();
        alarmMessageDtoList.forEach(info -> {
            builder.append("\nscopeId:").append(info.getScopeId())
                    .append("\nscope实体:").append(info.getScope())
                    .append("\n告警信息:").append(info.getAlarmMessage())
                    .append("\n告警规则:").append(info.getRuleName())
                    .append("\n\n----------------------\n\n");
        });
        //企业开发时，可以送数据库查找相关负责人
        mailService.sendMail("1849891624@qq.com",builder.toString(),"shopping alarm");
    }
}
