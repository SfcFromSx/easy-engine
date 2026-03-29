package com.smartbi.engine.trace;

import com.smartbi.engine.config.EngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consumes traces from Redis List (JDBC uses LPUSH). Uses non-blocking RPOP polling so it works
 * with Lettuce pooled connections (BRPOP/rightPop-with-timeout needs a dedicated connection).
 */
@Component
public class RedisTraceConsumer {

    private static final Logger log = LoggerFactory.getLogger(RedisTraceConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final EngineProperties engineProperties;
    private final TraceIngestionService traceIngestionService;

    public RedisTraceConsumer(StringRedisTemplate redisTemplate,
                              EngineProperties engineProperties,
                              TraceIngestionService traceIngestionService) {
        this.redisTemplate = redisTemplate;
        this.engineProperties = engineProperties;
        this.traceIngestionService = traceIngestionService;
    }

    @Scheduled(fixedDelayString = "${engine.consumer.interval:500}")
    public void poll() {
        if (!engineProperties.getConsumer().isEnabled()) {
            return;
        }
        String key = engineProperties.getRedis().getTraceListKey();
        java.util.List<String> batch = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < 200; i++) {
                String json = redisTemplate.opsForList().rightPop(key);
                if (json == null) break;
                batch.add(json);
            }
            if (!batch.isEmpty()) {
                traceIngestionService.ingestBatch(batch);
                log.debug("Ingested batch of {} traces from Redis", batch.size());
            }
        } catch (Exception e) {
            log.error("Trace poll error (will retry next cycle): {}", e.getMessage());
        }
    }
}
