package com.itbaizhan.travel_ai_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync  //启动异步
@Configuration
public class AsyncConfig {

    //自定义线程池
    @Bean
    public Executor asyncExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);  //核心线程数
        executor.setMaxPoolSize(20); //最大线程数
        executor.setQueueCapacity(50); //队列容量
        executor.setThreadNamePrefix("Async-"); //线程名字前缀

        executor.initialize();
        return executor;
    }
}