package com.itbaizhan.travel_mail_service;

import com.itbaizhan.travelcommon.result.BaseResult;
import com.itbaizhan.travelcommon.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TravelMailServiceApplicationTests {

    @Autowired
    private MailService mailService;

    @Test
    void contextLoads() {
        BaseResult<?> baseResult = mailService.sendMail("1849891624@qq.com", "测试邮件", "测试");
        System.out.println(baseResult);
    }

}
