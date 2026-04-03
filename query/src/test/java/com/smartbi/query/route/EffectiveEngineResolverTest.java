package com.smartbi.query.route;

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
    void shouldReturnParsedEngineWhenReportIdIsMissing() {
        EffectiveEngineResolver resolver = new EffectiveEngineResolver(null);
        ParsedSql parsed = SqlCommentParser.parse("/* ENGINE=presto_local */ SELECT 1");

        assertEquals("presto_local", resolver.resolve(parsed));
    }

    @Test
    void shouldPreferRedisOverrideWhenReportIdIsPresent() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("report-42")).thenReturn("trino_local");

        EffectiveEngineResolver resolver = new EffectiveEngineResolver(redisTemplate);
        ParsedSql parsed = SqlCommentParser.parse("/* ENGINE=default YH_RPTID=report-42 */ SELECT 1");

        assertEquals("trino_local", resolver.resolve(parsed));
    }

    @Test
    void shouldFallbackToParsedEngineWhenRedisLookupFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("report-42")).thenThrow(new RuntimeException("redis down"));

        EffectiveEngineResolver resolver = new EffectiveEngineResolver(redisTemplate);
        ParsedSql parsed = SqlCommentParser.parse("/* ENGINE=default YH_RPTID=report-42 */ SELECT 1");

        assertEquals("default", resolver.resolve(parsed));
    }
}
