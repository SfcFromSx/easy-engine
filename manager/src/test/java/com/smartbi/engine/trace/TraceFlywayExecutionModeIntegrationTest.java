package com.smartbi.engine.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.engine.EngineApplication;
import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import com.smartbi.engine.support.ManagerTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = EngineApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
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
        registry.add("spring.datasource.driver-class-name", () -> ManagerTestFixtures.get("manager.test.shared.driver-class-name"));
        registry.add("spring.datasource.username", () -> ManagerTestFixtures.get("manager.test.shared.username"));
        registry.add("spring.datasource.password", () -> ManagerTestFixtures.get("manager.test.shared.password"));
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> true);
        registry.add("spring.flyway.baseline-version", () -> "4");
    }

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        patternStatsRepository.deleteAll();
    }

    @Test
    // Covers TraceIngestionService#ingestJson and TraceController#page against Flyway-backed schema changes.
    void shouldApplyFlywayTraceColumnsAndExposeStoredValues() throws Exception {
        Integer executionModeColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_schema) = LOWER(DATABASE()) " +
                        "AND LOWER(table_name) = 'manager_sql_execution_record' " +
                        "AND LOWER(column_name) = 'execution_mode'",
                Integer.class);
        Integer parameterPayloadColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_schema) = LOWER(DATABASE()) " +
                        "AND LOWER(table_name) = 'manager_sql_execution_record' " +
                        "AND LOWER(column_name) = 'parameter_payload'",
                Integer.class);
        Integer cacheKeyColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_schema) = LOWER(DATABASE()) " +
                        "AND LOWER(table_name) = 'manager_sql_execution_record' " +
                        "AND LOWER(column_name) = 'cache_key'",
                Integer.class);
        assertEquals(Integer.valueOf(1), executionModeColumnCount);
        assertEquals(Integer.valueOf(1), parameterPayloadColumnCount);
        assertEquals(Integer.valueOf(1), cacheKeyColumnCount);

        ingestionService.ingestJson("{\"datasourceName\":\"default\",\"datasourceType\":\"mysql\",\"originalSql\":\"SELECT 1\",\"parameterPayload\":\"[{\\\"position\\\":1,\\\"className\\\":\\\"java.lang.Integer\\\",\\\"value\\\":\\\"7\\\"}]\",\"executionMode\":\"PREPARED_STATEMENT\",\"success\":false,\"cacheKey\":\"kylin_cache:default:select-1\",\"durationMs\":12}");
        ingestionService.ingestJson("{\"datasourceName\":\"default\",\"datasourceType\":\"mysql\",\"originalSql\":\"SELECT 2\",\"success\":true,\"durationMs\":8}");

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
        assertEquals("kylin_cache:default:select-1", prepared.getCacheKey());
        assertNull(legacy.getParameterPayload());
        assertNull(legacy.getCacheKey());
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
        assertEquals("kylin_cache:default:select-1", preparedTrace.path("cacheKey").asText());
        assertTrue(legacyTrace.path("cacheKey").isMissingNode() || legacyTrace.path("cacheKey").isNull());
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

    private static String baselineJdbcUrl() {
        return ManagerTestFixtures.mysqlJdbcUrlWithInit(
                "manager.test.flyway.trace-db-name",
                "manager.test.flyway.baseline-v4-resource");
    }
}
