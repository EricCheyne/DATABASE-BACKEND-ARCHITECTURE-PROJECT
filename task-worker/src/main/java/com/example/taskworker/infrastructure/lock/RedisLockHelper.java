package com.example.taskworker.infrastructure.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockHelper {

    private final StringRedisTemplate redisTemplate;

    public <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> task) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", timeout);

        if (Boolean.TRUE.equals(acquired)) {
            try {
                return task.get();
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            log.warn("Could not acquire lock for key: {}", lockKey);
            throw new RuntimeException("Lock already held for key: " + lockKey);
        }
    }
}
