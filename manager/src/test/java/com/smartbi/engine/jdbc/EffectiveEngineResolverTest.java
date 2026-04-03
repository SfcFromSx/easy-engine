package com.smartbi.engine.jdbc;

import com.smartbi.analyze.sql.ParsedSql;
import com.smartbi.analyze.sql.SqlCommentParser;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EffectiveEngineResolverTest {

    @Test
    void shouldFallbackToParsedEngineWhenRedisMisses() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("report-99")).thenReturn(null);

        EffectiveEngineResolver resolver = new EffectiveEngineResolver(redisTemplate);
        ParsedSql parsed = SqlCommentParser.parse("/* ENGINE=default YH_RPTID=report-99 */ SELECT 1");

        assertEquals("default", resolver.resolve(parsed));
    }
}
