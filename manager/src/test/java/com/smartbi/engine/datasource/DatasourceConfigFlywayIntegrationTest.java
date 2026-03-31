package com.smartbi.engine.datasource;

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
class DatasourceConfigFlywayIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void flywayProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DatasourceConfigFlywayIntegrationTest::baselineJdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "filesystem:" + resolveFlywayDirectory().toAbsolutePath());
        registry.add("spring.flyway.baseline-on-migrate", () -> true);
        registry.add("spring.flyway.baseline-version", () -> "4");
    }

    @Test
    void shouldCreateDatasourceConfigTableAndSeedQueryDefaults() throws Exception {
        Integer defaultColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'query_datasource_config' AND LOWER(column_name) = 'is_default'",
                Integer.class);
        Integer updatedAtColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'query_datasource_config' AND LOWER(column_name) = 'updated_at'",
                Integer.class);

        assertEquals(Integer.valueOf(1), defaultColumnCount);
        assertEquals(Integer.valueOf(1), updatedAtColumnCount);
        assertEquals(Long.valueOf(2L), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM query_datasource_config",
                Long.class));

        JsonNode rows = JSON.readTree(mockMvc.perform(get("/api/v1/query-datasources"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertEquals(2, rows.size());
        assertEquals("default", rows.get(0).path("name").asText());
        assertEquals(true, rows.get(0).path("isDefault").asBoolean());
        assertEquals("org.apache.kylin.jdbc.Driver", rows.get(0).path("driverClass").asText());
        assertEquals("presto_local", rows.get(1).path("name").asText());
        assertEquals(false, rows.get(1).path("isDefault").asBoolean());
        assertEquals("com.facebook.presto.jdbc.PrestoDriver", rows.get(1).path("driverClass").asText());
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

    private static String baselineJdbcUrl() {
        Path baselineScript = resolveBaselineSchema();
        return "jdbc:h2:mem:managerdatasourceflyway;" +
                "MODE=MySQL;" +
                "DATABASE_TO_LOWER=TRUE;" +
                "DEFAULT_NULL_ORDERING=HIGH;" +
                "DB_CLOSE_DELAY=-1;" +
                "DB_CLOSE_ON_EXIT=FALSE;" +
                "INIT=RUNSCRIPT FROM '" + baselineScript.toAbsolutePath().toString().replace("'", "''") + "'";
    }

    private static Path resolveBaselineSchema() {
        Path cwd = Paths.get("").toAbsolutePath();
        Path moduleLocal = cwd.resolve("src/test/resources/db/manager-v4-baseline.sql");
        if (Files.isRegularFile(moduleLocal)) {
            return moduleLocal;
        }
        Path repoRoot = cwd.resolve("manager/src/test/resources/db/manager-v4-baseline.sql");
        if (Files.isRegularFile(repoRoot)) {
            return repoRoot;
        }
        throw new IllegalStateException("Unable to locate manager test baseline schema from " + cwd);
    }
}
