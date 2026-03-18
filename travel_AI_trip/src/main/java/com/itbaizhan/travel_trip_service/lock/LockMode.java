package com.itbaizhan.travel_trip_service.lock;

/**
 * 分布式读写锁模式。
 */
public enum LockMode {
    NONE,
    READ,
    WRITE,
    BACKUP
}

