package com.smartbi.engine.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.engine.EngineApplication;
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
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "filesystem:" + resolveFlywayDirectory().toAbsolutePath());
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
    }

    @Test
    // Covers the default Flyway bootstrap path used by the dashboard data APIs.
    void shouldLeaveDashboardApisEmptyUntilLiveDataArrives() throws Exception {
        assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sql_execution_record", Long.class));
        assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sql_pattern_stats", Long.class));
        assertEquals(Long.valueOf(0L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM acceleration_table", Long.class));
        assertEquals(Long.valueOf(2L), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM query_datasource_config", Long.class));

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

    private static Path resolveFlywayDirectory() {
        Path cwd = Paths.get("").toAbsolutePath();
        Path moduleLocal = cwd.resolve("src/main/resources/db/migration");
        if (Files.isDirectory(moduleLocal)) {
            return moduleLocal;
        }
        Path repoRoot = cwd.resolve("manager/src/main/resources/db/migration");
        if (Files.isDirectory(repoRoot)) {
            return repoRoot;
        }
        throw new IllegalStateException("Unable to locate manager Flyway migrations from " + cwd);
    }

    private static String jdbcUrl() {
        return "jdbc:h2:mem:managerdashboardbootstrap;" +
                "MODE=MySQL;" +
                "DATABASE_TO_LOWER=TRUE;" +
                "DEFAULT_NULL_ORDERING=HIGH;" +
                "DB_CLOSE_DELAY=-1;" +
                "DB_CLOSE_ON_EXIT=FALSE";
    }
}
