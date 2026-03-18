package com.itbaizhan.travel_trip_service.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 行程相关分布式读写锁声明（基于 Redisson RReadWriteLock）。
 *
 * <p>锁 key 约定：</p>
 * <ul>
 *     <li>用户级：lock:user:{userId}:trips</li>
 *     <li>行程级：lock:trip:{userId}:{tripId}</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TripLocks {
    /**
     * 用户级锁模式（clear 用 WRITE，其它通常 READ）。
     */
    LockMode user() default LockMode.NONE;

    /**
     * 行程级锁模式（delete 用 WRITE，update/backup 通常 READ）。
     */
    LockMode trip() default LockMode.NONE;

    LockMode backup() default LockMode.NONE;
    /**
     * userId 参数索引（从 0 开始）。
     */
    int userIdIndex();

    /**
     * tripId 参数索引（从 0 开始）。
     *
     * <p>可指向 String tripId，也可指向包含 getTripId() 的对象（例如 TravelPlanResponse）。</p>
     */
    int tripIdIndex() default -1;

    /**
     * 获取锁的最大等待时间（毫秒）。设置为 0 表示直接失败。
     */
    long waitMillis() default 0;

    /**
     * 锁自动释放时间（毫秒），防止异常导致死锁。
     */
    long leaseMillis() default 60000;

    /**
     * 获取锁失败时返回给用户的提示文案（为空则使用默认文案）。
     */
    String failMessage() default "";

}

