package com.itbaizhan.travel_ai_service.config;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLock {
    @Autowired
    private RedissonClient redissonClient;

    //加锁
    /**
     *
     * @param key key(键)
     * @param expireTime
     * @return
     */
    public boolean lock(String key,long expireTime){
        RLock lock = redissonClient.getLock("lock:" + key);
        try {
            //如果规定的时间没有释放锁，直接释放
            return lock.tryLock(expireTime, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); //中断当前线程，加锁失败
            return false;
        }
    }
    //释放锁
    public void unlock(String key){
        RLock lock = redissonClient.getLock("lock:" + key);
        if(lock.isLocked()){
            lock.unlock();
        }
    }
}
