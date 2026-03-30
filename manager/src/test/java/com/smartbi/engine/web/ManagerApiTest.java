package com.smartbi.engine.web;

import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.domain.SqlPatternStats;
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
        record.setOriginalSql("SELECT 1");
        record.setReceivedAt(Instant.now());
        record.setParseStatus(com.smartbi.engine.domain.ParseStatus.OK);
        record.setSqlFingerprint("fp1");
        record.setParameterPayload("[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"1\"}]");
        record.setExecutionMode("PREPARED_STATEMENT");
        recordRepository.save(record);

        SqlPatternStats stats = new SqlPatternStats();
        stats.setSqlFingerprint("fp123");
        stats.setCleanSqlSample("SELECT 1");
        stats.setExecutionCount(10);
        stats.setLastSeenAt(Instant.now());
        patternStatsRepository.save(stats);
    }

    @Test
    void shouldReturnStatsSummary() throws Exception {
        mockMvc.perform(get("/api/v1/stats/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTraces").value(1))
                .andExpect(jsonPath("$.patternCount").value(1));
    }

    @Test
    void shouldReturnTopPatterns() throws Exception {
        mockMvc.perform(get("/api/v1/patterns/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sqlFingerprint").value("fp123"))
                .andExpect(jsonPath("$.content[0].executionCount").value(10));
    }

    @Test
    void shouldReturnTraces() throws Exception {
        mockMvc.perform(get("/api/v1/traces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].originalSql").value("SELECT 1"))
                .andExpect(jsonPath("$.content[0].parameterPayload")
                        .value("[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"1\"}]"))
                .andExpect(jsonPath("$.content[0].executionMode").value("PREPARED_STATEMENT"));
    }
}
