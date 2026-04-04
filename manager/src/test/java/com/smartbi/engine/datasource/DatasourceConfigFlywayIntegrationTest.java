package com.smartbi.engine.datasource;

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
        registry.add("spring.datasource.driver-class-name",
                () -> ManagerTestFixtures.get("manager.test.shared.driver-class-name"));
        registry.add("spring.datasource.username",
                () -> ManagerTestFixtures.get("manager.test.shared.username"));
        registry.add("spring.datasource.password",
                () -> ManagerTestFixtures.get("manager.test.shared.password"));
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> true);
        registry.add("spring.flyway.baseline-version", () -> "4");
    }

    @Test
    // Covers DatasourceConfigController#list and Flyway datasource schema seeding.
    void shouldCreateDatasourceConfigTableAndSeedQueryDefaults() throws Exception {
        Integer defaultColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_schema) = LOWER(DATABASE()) " +
                        "AND LOWER(table_name) = 'manager_query_datasource_config' " +
                        "AND LOWER(column_name) = 'is_default'",
                Integer.class);
        Integer updatedAtColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(table_schema) = LOWER(DATABASE()) " +
                        "AND LOWER(table_name) = 'manager_query_datasource_config' " +
                        "AND LOWER(column_name) = 'updated_at'",
                Integer.class);

        assertEquals(Integer.valueOf(1), defaultColumnCount);
        assertEquals(Integer.valueOf(1), updatedAtColumnCount);
        assertEquals(Long.valueOf(3L), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM manager_query_datasource_config",
                Long.class));

        JsonNode rows = JSON.readTree(mockMvc.perform(get("/api/v1/query-datasources"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertEquals(3, rows.size());
        assertEquals("default", rows.get(0).path("name").asText());
        assertEquals(true, rows.get(0).path("isDefault").asBoolean());
        assertEquals("org.apache.kylin.jdbc.Driver", rows.get(0).path("driverClass").asText());
        assertEquals("presto_local", rows.get(1).path("name").asText());
        assertEquals(false, rows.get(1).path("isDefault").asBoolean());
        assertEquals("com.facebook.presto.jdbc.PrestoDriver", rows.get(1).path("driverClass").asText());
        assertEquals("trino_local", rows.get(2).path("name").asText());
        assertEquals("io.trino.jdbc.TrinoDriver", rows.get(2).path("driverClass").asText());
    }

    private static String baselineJdbcUrl() {
        return ManagerTestFixtures.mysqlJdbcUrlWithInit(
                "manager.test.flyway.datasource-db-name",
                "manager.test.flyway.baseline-v4-resource");
    }
}
