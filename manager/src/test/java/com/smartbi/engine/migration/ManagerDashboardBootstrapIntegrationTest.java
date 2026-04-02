package com.smartbi.engine.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.engine.EngineApplication;
import com.smartbi.engine.support.ManagerTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = EngineApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@AutoConfigureMockMvc
class ManagerDashboardBootstrapIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void flywayProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ManagerDashboardBootstrapIntegrationTest::jdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> ManagerTestFixtures.get("manager.test.shared.driver-class-name"));
        registry.add("spring.datasource.username", () -> ManagerTestFixtures.get("manager.test.shared.username"));
        registry.add("spring.datasource.password", () -> ManagerTestFixtures.get("manager.test.shared.password"));
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
    }

    @Test
    // Covers the default Flyway bootstrap path used by the dashboard data APIs.
    void shouldLeaveDashboardApisEmptyUntilLiveDataArrives() throws Exception {
        assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM manager_sql_execution_record", Long.class));
        assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM manager_sql_pattern_stats", Long.class));
        assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM manager_acceleration_table", Long.class));
        assertEquals(Long.valueOf(2L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM manager_query_datasource_config", Long.class));
        assertEquals(Long.valueOf(8L), jdbcTemplate.queryForObject("SELECT MAX(installed_rank) FROM manager_flyway_schema_history", Long.class));

        JsonNode summary = JSON.readTree(mockMvc.perform(get("/api/v1/stats/summary"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        JsonNode patterns = JSON.readTree(mockMvc.perform(get("/api/v1/patterns/top"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        JsonNode traces = JSON.readTree(mockMvc.perform(get("/api/v1/traces"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        JsonNode accelerations = JSON.readTree(mockMvc.perform(get("/api/v1/acceleration-tables"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertEquals(0, summary.path("totalTraces").asInt());
        assertEquals(0, summary.path("patternCount").asInt());
        assertEquals(0, summary.path("activeAccelerationCount").asInt());
        assertEquals(0, summary.path("draftAccelerationCount").asInt());
        assertEquals(0, patterns.path("content").size());
        assertEquals(0, traces.path("content").size());
        assertEquals(0, accelerations.path("content").size());
    }

    private static String jdbcUrl() {
        return ManagerTestFixtures.h2JdbcUrl("manager.test.flyway.dashboard-db-name");
    }
}
