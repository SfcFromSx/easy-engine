package com.smartbi.engine.migration;

import com.smartbi.engine.EngineApplication;
import com.smartbi.engine.support.ManagerTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = EngineApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
class ManagerFlywayHistoryRenameIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void flywayProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ManagerFlywayHistoryRenameIntegrationTest::legacyFlywayJdbcUrl);
        registry.add("spring.datasource.driver-class-name",
                () -> ManagerTestFixtures.get("manager.test.shared.driver-class-name"));
        registry.add("spring.datasource.username",
                () -> ManagerTestFixtures.get("manager.test.shared.username"));
        registry.add("spring.datasource.password",
                () -> ManagerTestFixtures.get("manager.test.shared.password"));
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
    }

    @Test
    // Covers FlywayConfig#managerFlywayMigrationStrategy against a legacy manager
    // schema history table.
    void shouldRenameLegacyFlywayHistoryAndPrefixManagerTables() {
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE LOWER(table_schema) = LOWER(DATABASE()) "
                        + "AND LOWER(table_name) = 'manager_flyway_schema_history'",
                Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM manager_flyway_schema_history WHERE version = '9'",
                Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE LOWER(table_schema) = LOWER(DATABASE()) "
                        + "AND LOWER(table_name) = 'manager_sql_execution_record'",
                Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE LOWER(table_schema) = LOWER(DATABASE()) "
                        + "AND LOWER(table_name) = 'manager_sql_pattern_stats'",
                Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE LOWER(table_schema) = LOWER(DATABASE()) "
                        + "AND LOWER(table_name) = 'manager_acceleration_table'",
                Integer.class));
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE LOWER(table_schema) = LOWER(DATABASE()) "
                        + "AND LOWER(table_name) = 'manager_query_datasource_config'",
                Integer.class));
        assertEquals(Long.valueOf(2L), jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM manager_query_datasource_config",
                Long.class));
    }

    private static String legacyFlywayJdbcUrl() {
        return ManagerTestFixtures.mysqlJdbcUrlWithInit(
                "manager.test.flyway.legacy-db-name",
                "manager.test.flyway.legacy-v8-resource");
    }
}
