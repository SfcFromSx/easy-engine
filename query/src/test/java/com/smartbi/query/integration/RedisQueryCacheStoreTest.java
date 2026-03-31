package com.smartbi.query.integration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisQueryCacheStoreTest {

    // Covers RedisQueryCacheStore#get successful lookup branch.
    @Test
    void shouldReadCachedValuesFromRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache-key")).thenReturn("cached-value");

        RedisQueryCacheStore store = new RedisQueryCacheStore(redisTemplate);

        assertEquals("cached-value", store.get("cache-key"));
    }

    // Covers RedisQueryCacheStore#get failure fallback branch.
    @Test
    void shouldReturnNullWhenRedisGetFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache-key")).thenThrow(new RuntimeException("redis down"));

        RedisQueryCacheStore store = new RedisQueryCacheStore(redisTemplate);

        assertNull(store.get("cache-key"));
    }

    // Covers RedisQueryCacheStore#set successful write branch.
    @Test
    void shouldWriteCachedValuesToRedisWithTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RedisQueryCacheStore store = new RedisQueryCacheStore(redisTemplate);

        store.set("cache-key", "cached-value", 15);

        verify(valueOperations).set("cache-key", "cached-value", Duration.ofSeconds(15));
    }

    // Covers RedisQueryCacheStore#set failure swallowing branch.
    @Test
    void shouldSwallowRedisSetFailures() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down")).when(valueOperations).set("cache-key", "cached-value", Duration.ofSeconds(15));

        RedisQueryCacheStore store = new RedisQueryCacheStore(redisTemplate);

        assertDoesNotThrow(() -> store.set("cache-key", "cached-value", 15));
    }
}
