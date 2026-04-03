package com.smartbi.query.route;

import com.smartbi.analyze.sql.ParsedSql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EffectiveEngineResolver {

    private static final Logger log = LoggerFactory.getLogger(EffectiveEngineResolver.class);

    private final StringRedisTemplate redisTemplate;

    public EffectiveEngineResolver(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String resolve(ParsedSql parsed) {
        String parsedEngine = normalize(parsed == null || parsed.metadata == null ? null : parsed.metadata.engine);
        String rptId = normalize(parsed == null || parsed.metadata == null ? null : parsed.metadata.rptId);
        if (!StringUtils.hasText(rptId) || redisTemplate == null) {
            return parsedEngine;
        }
        try {
            String overriddenEngine = normalize(redisTemplate.opsForValue().get(rptId));
            return StringUtils.hasText(overriddenEngine) ? overriddenEngine : parsedEngine;
        } catch (Exception ex) {
            log.warn("Redis ENGINE override lookup failed for report {}: {}", rptId, ex.getMessage());
            return parsedEngine;
        }
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
