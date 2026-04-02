package com.itbaizhan.travelmanager.service;

import com.itbaizhan.travelmanager.config.RedisKeyProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class HotStyleService {
    @Autowired
    private RedisKeyProperties redisKeyProperties;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public String getHotStyle() {
        return stringRedisTemplate.opsForValue().get(redisKeyProperties.getHot());
    }

    public void setHotStyle(String hot) {
        stringRedisTemplate.opsForValue().set(redisKeyProperties.getHot(), hot);
    }
}
