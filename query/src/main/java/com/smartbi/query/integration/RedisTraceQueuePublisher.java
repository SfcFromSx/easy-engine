package com.smartbi.query.integration;

import com.smartbi.query.config.QueryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class RedisTraceQueuePublisher implements TraceQueuePublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisTraceQueuePublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final QueryProperties queryProperties;
    private final Executor queryTraceExecutor;

    public RedisTraceQueuePublisher(StringRedisTemplate redisTemplate,
                                    QueryProperties queryProperties,
                                    Executor queryTraceExecutor) {
        this.redisTemplate = redisTemplate;
        this.queryProperties = queryProperties;
        this.queryTraceExecutor = queryTraceExecutor;
    }

    @Override
    public void publish(final String listKey, final String payload) {
        queryTraceExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    redisTemplate.opsForList().leftPush(listKey, payload);
                    redisTemplate.opsForList().trim(listKey, 0, queryProperties.getTrace().getMaxListLength());
                } catch (Exception ex) {
                    log.warn("Redis LPUSH failed for {}: {}", listKey, ex.getMessage());
                }
            }
        });
    }
}
