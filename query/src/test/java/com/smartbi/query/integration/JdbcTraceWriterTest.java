package com.smartbi.query.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.domain.ParseStatus;
import com.smartbi.query.domain.SqlExecutionRecord;
import com.smartbi.query.domain.SqlPatternStats;
import com.smartbi.query.repo.SqlExecutionRecordRepository;
import com.smartbi.query.repo.SqlPatternStatsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcTraceWriterTest {

    // Covers JdbcTraceWriter#publish blank-payload skip branch.
    @Test
    void shouldIgnoreBlankTracePayloads() {
        SqlExecutionRecordRepository recordRepository = mock(SqlExecutionRecordRepository.class);
        SqlPatternStatsRepository patternStatsRepository = mock(SqlPatternStatsRepository.class);
        JdbcTraceWriter writer = new JdbcTraceWriter(new ObjectMapper(), recordRepository, patternStatsRepository);

        writer.publish(" ");

        verify(recordRepository, never()).save(any(SqlExecutionRecord.class));
        verify(patternStatsRepository, never()).save(any(SqlPatternStats.class));
    }

    // Covers JdbcTraceWriter#publish invalid-json error-path persistence.
    @Test
    void shouldPersistParseErrorsForInvalidJsonPayloads() {
        SqlExecutionRecordRepository recordRepository = mock(SqlExecutionRecordRepository.class);
        SqlPatternStatsRepository patternStatsRepository = mock(SqlPatternStatsRepository.class);
        JdbcTraceWriter writer = new JdbcTraceWriter(new ObjectMapper(), recordRepository, patternStatsRepository);
        ArgumentCaptor<SqlExecutionRecord> recordCaptor = ArgumentCaptor.forClass(SqlExecutionRecord.class);

        writer.publish("{not-json");

        verify(recordRepository).save(recordCaptor.capture());
        SqlExecutionRecord record = recordCaptor.getValue();
        assertEquals(ParseStatus.ERROR, record.getParseStatus());
        assertNotNull(record.getParseError());
        verify(patternStatsRepository, never()).save(any(SqlPatternStats.class));
    }

    // Covers JdbcTraceWriter#publish happy path, JdbcTraceWriter#fillRecord, and JdbcTraceWriter#upsertPatternStats new-row branch.
    @Test
    void shouldPersistFingerprintAndCreatePatternStatsForValidPayloads() {
        SqlExecutionRecordRepository recordRepository = mock(SqlExecutionRecordRepository.class);
        SqlPatternStatsRepository patternStatsRepository = mock(SqlPatternStatsRepository.class);
        when(patternStatsRepository.findBySqlFingerprint(any(String.class))).thenReturn(Optional.<SqlPatternStats>empty());
        JdbcTraceWriter writer = new JdbcTraceWriter(new ObjectMapper(), recordRepository, patternStatsRepository);
        ArgumentCaptor<SqlExecutionRecord> recordCaptor = ArgumentCaptor.forClass(SqlExecutionRecord.class);
        ArgumentCaptor<SqlPatternStats> statsCaptor = ArgumentCaptor.forClass(SqlPatternStats.class);

        writer.publish("{\"datasourceName\":\"default\",\"datasourceType\":\"h2\",\"originalSql\":\"SELECT * FROM SALES\",\"cleanSql\":\"SELECT * FROM SALES\",\"paramFingerprint\":\"fp\",\"parameterPayload\":null,\"executionMode\":\"STATEMENT\",\"success\":true,\"cacheHit\":false,\"cacheKey\":\"kylin_cache:default:fp\",\"durationMs\":12}");

        verify(recordRepository).save(recordCaptor.capture());
        SqlExecutionRecord record = recordCaptor.getValue();
        assertEquals(ParseStatus.OK, record.getParseStatus());
        assertEquals("default", record.getDatasourceName());
        assertEquals("h2", record.getDatasourceType());
        assertEquals("STATEMENT", record.getExecutionMode());
        assertEquals("kylin_cache:default:fp", record.getCacheKey());
        assertEquals(Long.valueOf(12L), record.getDurationMs());
        assertNotNull(record.getSqlFingerprint());

        verify(patternStatsRepository).save(statsCaptor.capture());
        SqlPatternStats stats = statsCaptor.getValue();
        assertEquals(1L, stats.getExecutionCount());
        assertEquals("SELECT * FROM SALES", stats.getCleanSqlSample());
    }

    // Covers JdbcTraceWriter#publish skipped-parse branch when fingerprint SQL is absent.
    @Test
    void shouldMarkTraceRowsAsSkippedWhenSqlIsMissing() {
        SqlExecutionRecordRepository recordRepository = mock(SqlExecutionRecordRepository.class);
        SqlPatternStatsRepository patternStatsRepository = mock(SqlPatternStatsRepository.class);
        JdbcTraceWriter writer = new JdbcTraceWriter(new ObjectMapper(), recordRepository, patternStatsRepository);
        ArgumentCaptor<SqlExecutionRecord> recordCaptor = ArgumentCaptor.forClass(SqlExecutionRecord.class);

        writer.publish("{\"datasourceName\":\"default\",\"success\":true}");

        verify(recordRepository).save(recordCaptor.capture());
        SqlExecutionRecord record = recordCaptor.getValue();
        assertEquals(ParseStatus.SKIPPED, record.getParseStatus());
        assertNull(record.getSqlFingerprint());
        verify(patternStatsRepository, never()).save(any(SqlPatternStats.class));
    }

    // Covers JdbcTraceWriter#upsertPatternStats existing-row update branch.
    @Test
    void shouldUpdateExistingPatternStatsWithAverageSampleAndSignature() {
        SqlExecutionRecordRepository recordRepository = mock(SqlExecutionRecordRepository.class);
        SqlPatternStatsRepository patternStatsRepository = mock(SqlPatternStatsRepository.class);
        JdbcTraceWriter writer = new JdbcTraceWriter(new ObjectMapper(), recordRepository, patternStatsRepository);
        SqlExecutionRecord row = new SqlExecutionRecord();
        row.setSqlFingerprint("fp");
        row.setDurationMs(Long.valueOf(30L));
        row.setSignatureJson("{\"kind\":\"sig\"}");

        SqlPatternStats existing = new SqlPatternStats();
        existing.setSqlFingerprint("fp");
        existing.setExecutionCount(1L);
        existing.setAvgDurationMs(Double.valueOf(10D));
        existing.setCleanSqlSample(null);
        when(patternStatsRepository.findBySqlFingerprint("fp")).thenReturn(Optional.of(existing));

        ReflectionTestUtils.invokeMethod(writer, "upsertPatternStats", row, "SELECT * FROM SALES");

        verify(patternStatsRepository).save(existing);
        assertEquals(2L, existing.getExecutionCount());
        assertEquals(Double.valueOf(20D), existing.getAvgDurationMs());
        assertEquals("SELECT * FROM SALES", existing.getCleanSqlSample());
        assertEquals("{\"kind\":\"sig\"}", existing.getSignatureJson());
    }

    // Covers JdbcTraceWriter#truncate truncation branch.
    @Test
    void shouldTruncateLongPatternSamples() {
        SqlExecutionRecordRepository recordRepository = mock(SqlExecutionRecordRepository.class);
        SqlPatternStatsRepository patternStatsRepository = mock(SqlPatternStatsRepository.class);
        JdbcTraceWriter writer = new JdbcTraceWriter(new ObjectMapper(), recordRepository, patternStatsRepository);
        String longSql = repeat("a", 4100);

        String truncated = ReflectionTestUtils.invokeMethod(writer, "truncate", longSql, 4000);

        assertEquals(4000, truncated.length());
    }

    private static String repeat(String value, int times) {
        StringBuilder builder = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
