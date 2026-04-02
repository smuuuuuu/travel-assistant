package com.itbaizhan.traveluser;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@SpringBootApplication
@EnableDiscoveryClient
@RefreshScope
@EnableDubbo
@MapperScan("com.itbaizhan.traveluser.mapper")
public class TravelUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelUserApplication.class, args);
    }
    @Bean
    public RedisTemplate<String, Object> edisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate= new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(factory);
        //设置通用序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }
}
