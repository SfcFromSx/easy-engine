package com.smartbi.engine.web;

import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationSource;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.domain.SqlPatternStats;
import com.smartbi.engine.repo.AccelerationTableRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccelerationLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SqlPatternStatsRepository patternStatsRepository;

    @Autowired
    private AccelerationTableRepository tableRepository;

    private long patternId;

    @BeforeEach
    void setUp() {
        SqlPatternStats stats = new SqlPatternStats();
        stats.setSqlFingerprint("accel_fp");
        stats.setCleanSqlSample("SELECT count(*) FROM table1");
        stats.setExecutionCount(50);
        stats = patternStatsRepository.save(stats);
        patternId = stats.getId();
    }

    @Test
    // Covers AccelerationController#fromPattern and AccelerationController#list.
    void shouldManageAccelerationLifecycle() throws Exception {
        // 1. Create from pattern
        String createJson = String.format("{\"patternStatsId\": %d, \"tableName\": \"mv_table1\", \"schemaName\": \"public\"}", patternId);
        mockMvc.perform(post("/api/v1/acceleration-tables/from-pattern")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("mv_table1"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        long tableId = tableRepository.findAll().get(0).getId();

        // 2. List
        mockMvc.perform(get("/api/v1/acceleration-tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("mv_table1"));

        // 3. (Manual) Note: We won't test ACTIVE status here because it triggers physical DDL
        // against external warehouse tables like KYLIN_SALES.
        // We will just verify it stays DRAFT for now or test a simple manual creation.
    }

    @Test
    // Covers AccelerationController#create.
    void shouldCreateManualTable() throws Exception {
        String manualJson = "{\"name\": \"manual_v\", \"ddlText\": \"CREATE VIEW v AS SELECT 1\"}";
        mockMvc.perform(post("/api/v1/acceleration-tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("manual_v"));
    }

    @Test
    // Covers AccelerationController#list combined filter path.
    void shouldFilterAccelerationList() throws Exception {
        AccelerationTable matching = new AccelerationTable();
        matching.setName("mv_sales_daily");
        matching.setSchemaName("analytics");
        matching.setDdlText("CREATE TABLE mv_sales_daily(id int)");
        matching.setRefreshSql("INSERT INTO mv_sales_daily SELECT 1");
        matching.setStatus(AccelerationStatus.ACTIVE);
        matching.setSource(AccelerationSource.RECOMMENDED);
        tableRepository.save(matching);

        AccelerationTable other = new AccelerationTable();
        other.setName("mv_other");
        other.setSchemaName("public");
        other.setDdlText("CREATE TABLE mv_other(id int)");
        other.setRefreshSql("INSERT INTO mv_other SELECT 1");
        other.setStatus(AccelerationStatus.DRAFT);
        other.setSource(AccelerationSource.MANUAL);
        tableRepository.save(other);

        mockMvc.perform(get("/api/v1/acceleration-tables")
                        .param("keyword", "sales")
                        .param("status", "ACTIVE")
                        .param("schemaName", "analytics")
                        .param("source", "RECOMMENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("mv_sales_daily"));
    }
}
