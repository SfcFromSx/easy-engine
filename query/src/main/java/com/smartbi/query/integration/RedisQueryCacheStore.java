package com.smartbi.query.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisQueryCacheStore implements QueryCacheStore {

    private static final Logger log = LoggerFactory.getLogger(RedisQueryCacheStore.class);

    private final StringRedisTemplate redisTemplate;

    public RedisQueryCacheStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception ex) {
            log.warn("Redis GET failed for key {}: {}", key, ex.getMessage());
            return null;
        }
    }

    @Override
    public void set(String key, String value, int ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            log.warn("Redis SET failed for key {}: {}", key, ex.getMessage());
        }
    }
}
