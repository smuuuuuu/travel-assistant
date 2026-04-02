package com.itbaizhan.travelmanager;

import com.itbaizhan.travelmanager.config.ManagerJwtProperties;
import com.itbaizhan.travelmanager.config.RedisKeyProperties;
import com.itbaizhan.travelmanager.config.RedisPromptProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.itbaizhan.travelmanager.mapper")
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.itbaizhan.travelmanager.mongo")
@EnableConfigurationProperties({RedisKeyProperties.class, RedisPromptProperties.class, ManagerJwtProperties.class})
@EnableAsync
@EnableDiscoveryClient
@RefreshScope
public class TravelManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelManagerApplication.class, args);
    }

}
