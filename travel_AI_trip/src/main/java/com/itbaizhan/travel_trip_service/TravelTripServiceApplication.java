package com.itbaizhan.travel_trip_service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@RefreshScope
@EnableDubbo
@MapperScan("com.itbaizhan.travel_trip_service.mapper")
@EnableScheduling
public class TravelTripServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelTripServiceApplication.class, args);
    }



}
