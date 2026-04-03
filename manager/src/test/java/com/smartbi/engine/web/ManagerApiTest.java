package com.smartbi.engine.web;

import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.domain.SqlPatternStats;
import com.smartbi.engine.domain.ParseStatus;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManagerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SqlExecutionRecordRepository recordRepository;

    @Autowired
    private SqlPatternStatsRepository patternStatsRepository;

    @BeforeEach
    void setUp() {
        SqlExecutionRecord record = new SqlExecutionRecord();
        record.setDatasourceName("default");
        record.setOriginalSql("SELECT 1");
        record.setReceivedAt(Instant.now());
        record.setParseStatus(ParseStatus.OK);
        record.setSqlFingerprint("fp1");
        record.setRawPayload("{\"sql\":\"SELECT 1\"}");
        record.setCacheHit(Boolean.FALSE);
        record.setCacheKey("kylin_cache:default:fp1");
        record.setParameterPayload("[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"1\"}]");
        record.setExecutionMode("PREPARED_STATEMENT");
        recordRepository.save(record);

        SqlExecutionRecord filteredRecord = new SqlExecutionRecord();
        filteredRecord.setDatasourceName("analytics");
        filteredRecord.setOriginalSql("SELECT customer_id FROM orders");
        filteredRecord.setReceivedAt(Instant.now().plusSeconds(1));
        filteredRecord.setParseStatus(ParseStatus.ERROR);
        filteredRecord.setSqlFingerprint("orders_fp");
        filteredRecord.setRawPayload("{\"sql\":\"SELECT customer_id FROM orders\"}");
        filteredRecord.setCacheHit(Boolean.TRUE);
        filteredRecord.setCacheKey("kylin_cache:analytics:orders_fp");
        recordRepository.save(filteredRecord);

        SqlPatternStats stats = new SqlPatternStats();
        stats.setSqlFingerprint("fp123");
        stats.setCleanSqlSample("SELECT 1");
        stats.setExecutionCount(10);
        stats.setLastSeenAt(Instant.now());
        patternStatsRepository.save(stats);

        SqlPatternStats filteredStats = new SqlPatternStats();
        filteredStats.setSqlFingerprint("sales_fp");
        filteredStats.setCleanSqlSample("SELECT customer_id FROM sales");
        filteredStats.setExecutionCount(40);
        filteredStats.setLastSeenAt(Instant.now().plusSeconds(1));
        patternStatsRepository.save(filteredStats);
    }

    @Test
    // Covers StatsController#summary via /api/v1/stats/summary.
    void shouldReturnStatsSummary() throws Exception {
        mockMvc.perform(get("/api/v1/stats/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTraces").value(2))
                .andExpect(jsonPath("$.patternCount").value(2));
    }

    @Test
    // Covers PatternController#top via /api/v1/patterns/top.
    void shouldReturnTopPatterns() throws Exception {
        mockMvc.perform(get("/api/v1/patterns/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sqlFingerprint").value("sales_fp"))
                .andExpect(jsonPath("$.content[0].executionCount").value(40));
    }

    @Test
    // Covers PatternController#top combined filter path.
    void shouldReturnFilteredTopPatterns() throws Exception {
        mockMvc.perform(get("/api/v1/patterns/top")
                        .param("fingerprint", "sales_fp")
                        .param("sqlKeyword", "sales")
                        .param("minExecutionCount", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sqlFingerprint").value("sales_fp"));
    }

    @Test
    // Covers TraceController#page via /api/v1/traces.
    void shouldReturnTraces() throws Exception {
        mockMvc.perform(get("/api/v1/traces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].originalSql").value("SELECT customer_id FROM orders"))
                .andExpect(jsonPath("$.content[0].cacheKey").value("kylin_cache:analytics:orders_fp"));
    }

    @Test
    // Covers TraceController#page combined filter path.
    void shouldReturnFilteredTraces() throws Exception {
        mockMvc.perform(get("/api/v1/traces")
                        .param("datasource", "analytics")
                        .param("cacheKey", "orders_fp")
                        .param("sourceFlag", "JDBC")
                        .param("cacheHit", "true")
                        .param("parseStatus", "ERROR")
                        .param("sqlKeyword", "orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].datasourceName").value("analytics"))
                .andExpect(jsonPath("$.content[0].originalSql").value("SELECT customer_id FROM orders"))
                .andExpect(jsonPath("$.content[0].cacheKey").value("kylin_cache:analytics:orders_fp"));
    }
}
