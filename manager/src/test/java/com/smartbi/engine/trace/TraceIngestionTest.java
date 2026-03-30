package com.smartbi.engine.trace;
 
import com.smartbi.engine.domain.ParseStatus;
import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.domain.SqlPatternStats;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TraceIngestionTest {

    @Autowired
    private TraceIngestionService ingestionService;

    @Autowired
    private SqlExecutionRecordRepository recordRepository;

    @Autowired
    private SqlPatternStatsRepository patternStatsRepository;

    @Test
    void shouldIngestAndSummarizeTrace() {
        String json = "{\"datasourceName\":\"default\",\"datasourceType\":\"h2\",\"originalSql\":\"SELECT 1\",\"parameterPayload\":\"[{\\\"position\\\":1,\\\"className\\\":\\\"java.lang.Integer\\\",\\\"value\\\":\\\"1\\\"}]\",\"executionMode\":\"PREPARED_STATEMENT\",\"success\":false,\"durationMs\":10}";
        
        ingestionService.ingestJson(json);

        List<SqlExecutionRecord> records = recordRepository.findAll();
        assertFalse(records.isEmpty());
        SqlExecutionRecord record = records.get(0);
        assertEquals("SELECT 1", record.getOriginalSql());
        assertEquals("[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"1\"}]",
                record.getParameterPayload());
        assertEquals("PREPARED_STATEMENT", record.getExecutionMode());
        assertEquals(ParseStatus.OK, record.getParseStatus());
        assertNotNull(record.getSqlFingerprint());

        Optional<SqlPatternStats> statsOpt = patternStatsRepository.findBySqlFingerprint(record.getSqlFingerprint());
        assertTrue(statsOpt.isPresent());
        SqlPatternStats stats = statsOpt.get();
        assertEquals(1, stats.getExecutionCount());
        assertEquals(10.0, stats.getAvgDurationMs());
    }

    @Test
    void shouldAccumulateStatsForSameFingerprint() {
        String json1 = "{\"originalSql\":\"SELECT * FROM t1\",\"durationMs\":100}";
        String json2 = "{\"originalSql\":\"SELECT   *   FROM   t1\",\"durationMs\":200}"; 
        
        ingestionService.ingestJson(json1);
        ingestionService.ingestJson(json2);

        // We should search by fingerprint to avoid interference from other tests if they share the same DB
        String fp = FingerprintUtil.sha256Hex(FingerprintUtil.normalizeForFingerprint("SELECT * FROM t1"));
        Optional<SqlPatternStats> statsOpt = patternStatsRepository.findBySqlFingerprint(fp);
        
        assertTrue(statsOpt.isPresent(), "Pattern stats should be created");
        SqlPatternStats stats = statsOpt.get();
        assertEquals(2, stats.getExecutionCount(), "Should have aggregated 2 executions");
        assertEquals(150.0, stats.getAvgDurationMs(), "Average duration should be 150");
    }
}
