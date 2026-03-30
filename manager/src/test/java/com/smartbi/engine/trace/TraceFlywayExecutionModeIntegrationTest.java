package com.smartbi.engine.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.engine.EngineApplication;
import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = EngineApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "engine.consumer.enabled=false"
        }
)
@AutoConfigureMockMvc
class TraceFlywayExecutionModeIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private TraceIngestionService ingestionService;

    @Autowired
    private SqlExecutionRecordRepository recordRepository;

    @Autowired
    private SqlPatternStatsRepository patternStatsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void flywayProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> baselineJdbcUrl());
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations",
                () -> "filesystem:" + resolveFlywayDirectory().toAbsolutePath());
        registry.add("spring.flyway.baseline-on-migrate", () -> true);
        registry.add("spring.flyway.baseline-version", () -> "4");
    }

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        patternStatsRepository.deleteAll();
    }

    @Test
    void shouldApplyFlywayTraceColumnsAndExposeStoredValues() throws Exception {
        Integer executionModeColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'sql_execution_record' AND LOWER(column_name) = 'execution_mode'",
                Integer.class);
        Integer parameterPayloadColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_name) = 'sql_execution_record' AND LOWER(column_name) = 'parameter_payload'",
                Integer.class);
        assertEquals(Integer.valueOf(1), executionModeColumnCount);
        assertEquals(Integer.valueOf(1), parameterPayloadColumnCount);

        ingestionService.ingestJson("{\"datasourceName\":\"default\",\"datasourceType\":\"h2\",\"originalSql\":\"SELECT 1\",\"parameterPayload\":\"[{\\\"position\\\":1,\\\"className\\\":\\\"java.lang.Integer\\\",\\\"value\\\":\\\"7\\\"}]\",\"executionMode\":\"PREPARED_STATEMENT\",\"success\":false,\"durationMs\":12}");
        ingestionService.ingestJson("{\"datasourceName\":\"default\",\"datasourceType\":\"h2\",\"originalSql\":\"SELECT 2\",\"success\":true,\"durationMs\":8}");

        SqlExecutionRecord prepared = recordRepository.findAll().stream()
                .filter(record -> "SELECT 1".equals(record.getOriginalSql()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Prepared trace not persisted"));
        SqlExecutionRecord legacy = recordRepository.findAll().stream()
                .filter(record -> "SELECT 2".equals(record.getOriginalSql()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Legacy trace not persisted"));

        assertEquals("PREPARED_STATEMENT", prepared.getExecutionMode());
        assertEquals("[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"7\"}]",
                prepared.getParameterPayload());
        assertNull(legacy.getParameterPayload());
        assertNull(legacy.getExecutionMode());
        assertNotNull(prepared.getSqlFingerprint());
        assertNotNull(legacy.getSqlFingerprint());

        JsonNode content = JSON.readTree(mockMvc.perform(get("/api/v1/traces"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .path("content");

        JsonNode preparedTrace = findTrace(content, "SELECT 1");
        JsonNode legacyTrace = findTrace(content, "SELECT 2");

        assertEquals("PREPARED_STATEMENT", preparedTrace.path("executionMode").asText());
        assertEquals("[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"7\"}]",
                preparedTrace.path("parameterPayload").asText());
        assertTrue(legacyTrace.path("parameterPayload").isMissingNode() || legacyTrace.path("parameterPayload").isNull());
        assertTrue(legacyTrace.path("executionMode").isMissingNode() || legacyTrace.path("executionMode").isNull());
    }

    private static JsonNode findTrace(JsonNode content, String originalSql) {
        Iterator<JsonNode> iterator = content.elements();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (originalSql.equals(item.path("originalSql").asText())) {
                return item;
            }
        }
        throw new AssertionError("Trace not found in API response for SQL: " + originalSql);
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
        return "jdbc:h2:mem:managertraceflyway;" +
                "MODE=PostgreSQL;" +
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
