package com.example.taskservice.infrastructure.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RateLimiterConfig {

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, org.redisson.spring.cache.CacheConfig> config = new HashMap<>();
        // Create "rate-limit-buckets" cache configuration
        config.put("rate-limit-buckets", new org.redisson.spring.cache.CacheConfig());
        return new RedissonSpringCacheManager(redissonClient, config);
    }
}
