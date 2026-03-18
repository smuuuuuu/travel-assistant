package com.itbaizhan.travel_trip_service.lock;

import com.itbaizhan.travel_trip_service.constant.TripConstant;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 行程相关分布式读写锁切面：统一处理 update/backup/delete/clear 的并发互斥。
 */
@Aspect
@Component
@Slf4j
public class TripLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 对标注 {@link TripLocks} 的方法统一加锁。
     *
     * <p>加锁顺序固定：用户锁 -> 行程锁，避免死锁。</p>
     */
    @Around("@annotation(com.itbaizhan.travel_trip_service.lock.TripLocks)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        TripLocks tripLocks = method.getAnnotation(TripLocks.class);

        if (tripLocks == null) {
            // 尝试从目标类获取（兼容接口代理场景）
            try {
                Method targetMethod = pjp.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());
                tripLocks = targetMethod.getAnnotation(TripLocks.class);
            } catch (Exception e) {
                // ignore
            }
        }

        // 如果无法获取注解，直接放行
        if (tripLocks == null) {
             return pjp.proceed();
        }

        Object[] args = pjp.getArgs();
        String userId = extractIdArg(args, tripLocks.userIdIndex());
        String tripId = tripLocks.tripIdIndex() >= 0 ? extractTripIdArg(args, tripLocks.tripIdIndex()) : "";

        if (!StringUtils.hasText(userId)) {
            return pjp.proceed();
        }

        List<RLock> acquired = new ArrayList<>(2);
        try {
            if (tripLocks.user() != LockMode.NONE) {
                RLock lock = getUserLock(userId, tripLocks.user());
                tryLockOrThrow(lock, tripLocks);
                acquired.add(lock);
            }

            if (tripLocks.trip() != LockMode.NONE && StringUtils.hasText(tripId)) {
                RLock lock = getTripLock(userId, tripId, tripLocks.trip(), TripConstant.NO_BACKUP);
                tryLockOrThrow(lock, tripLocks);
                acquired.add(lock);
            }
            if (tripLocks.backup() != LockMode.NONE && StringUtils.hasText(tripId)) {
                RLock lock = getTripLock(userId, tripId, tripLocks.backup(), TripConstant.BACKUP);
                tryLockOrThrow(lock, tripLocks);
                acquired.add(lock);
            }

            boolean unlockByTx = TransactionSynchronizationManager.isSynchronizationActive();
            if (unlockByTx) {
                registerUnlockAfterTx(acquired);
                return pjp.proceed();
            }

            try {
                return pjp.proceed();
            } finally {
                unlockQuietly(acquired);
            }
        } catch (BusException e) {
            throw e;
        } catch (Throwable t) {
            throw t;
        } finally {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                // 非事务场景已在 proceed 的 finally 解锁，这里不再重复解锁
            }
        }
    }

    /**
     * 获取用户级读写锁。
     */
    private RLock getUserLock(String userId, LockMode mode) {
        String key = "lock:user:" + userId + ":trips";
        RReadWriteLock rw = redissonClient.getReadWriteLock(key);
        return mode == LockMode.WRITE ? rw.writeLock() : rw.readLock();
    }

    /**
     * 获取行程级读写锁。
     */
    private RLock getTripLock(String userId, String tripId, LockMode mode,int isBackup) {
        String key = "lock:trip:" + userId + ":" + tripId + ":" + isBackup;
        RReadWriteLock rw = redissonClient.getReadWriteLock(key);
        return mode == LockMode.WRITE ? rw.writeLock() : rw.readLock();
    }

    /**
     * 尝试加锁；失败则抛业务异常用于“直接失败提示”。
     */
    private void tryLockOrThrow(RLock lock, TripLocks tripLocks) {
        boolean ok;
        try {
            //ok = lock.tryLock(tripLocks.waitMillis(), TimeUnit.MILLISECONDS);
            if (tripLocks.leaseMillis() > 0) {
                ok = lock.tryLock(tripLocks.waitMillis(), tripLocks.leaseMillis(), TimeUnit.MILLISECONDS);
            } else {
                // leaseMillis <= 0 时启用 Watchdog 机制（自动续期）
                ok = lock.tryLock(tripLocks.waitMillis(),30000, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ok = false;
        }
        if (!ok) {
            String msg = StringUtils.hasText(tripLocks.failMessage())
                    ? tripLocks.failMessage()
                    : CodeEnum.TRIP_LOCKED.getMessage();
            throw new BusException(CodeEnum.TRIP_LOCKED.getCode(), msg);
        }
    }

    /**
     * 事务场景：在事务完成后释放锁，确保提交/回滚期间不被其它操作穿透。
     */
    private void registerUnlockAfterTx(List<RLock> acquired) {
        if (acquired == null || acquired.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                unlockQuietly(acquired);
            }
        });
    }

    /**
     * 逆序释放锁，避免锁升级/降级场景下的潜在问题。
     */
    private void unlockQuietly(List<RLock> locks) {
        if (locks == null || locks.isEmpty()) return;
        for (int i = locks.size() - 1; i >= 0; i--) {
            RLock lock = locks.get(i);
            if (lock == null) continue;
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                if (isInterrupted(e)) {
                    log.warn("unlock interrupted for lock: {}", lock.getName());
                    Thread.currentThread().interrupt();
                } else {
                    log.warn("unlock failed", e);
                }
            }
        }
    }

    private boolean isInterrupted(Throwable e) {
        if (e instanceof InterruptedException) return true;
        if (e.getCause() != null) {
            return isInterrupted(e.getCause());
        }
        return false;
    }

    /**
     * 从参数中提取 userId（支持 Long/String）。
     */
    private String extractIdArg(Object[] args, int index) {
        if (args == null || index < 0 || index >= args.length) return "";
        Object v = args[index];
        if (v == null) return "";
        if (v instanceof Long l) return String.valueOf(l);
        if (v instanceof Integer i) return String.valueOf(i);
        return String.valueOf(v);
    }

    /**
     * 从参数中提取 tripId：优先 String；否则尝试反射调用 getTripId()。
     */
    private String extractTripIdArg(Object[] args, int index) {
        if (args == null || index < 0 || index >= args.length) return "";
        Object v = args[index];
        if (v == null) return "";
        if (v instanceof String s) return s;
        try {
            Method m = v.getClass().getMethod("getTripId");
            Object r = m.invoke(v);
            return r == null ? "" : String.valueOf(r);
        } catch (Exception ignored) {
        }
        return String.valueOf(v);
    }
}

