package com.itbaizhan.travel_ai_service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableDubbo
@EnableAsync
@EnableDiscoveryClient
@RefreshScope
@MapperScan("com.itbaizhan.travel_ai_service.mapper")
@SpringBootApplication
public class TravelAiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelAiServiceApplication.class, args);
    }
}
