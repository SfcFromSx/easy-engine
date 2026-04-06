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
        assertMediumText("manager_sql_pattern_stats", "clean_sql_sample");
        assertMediumText("manager_sql_pattern_stats", "signature_json");
        assertMediumText("manager_acceleration_table", "ddl_text");
        assertMediumText("manager_acceleration_table", "refresh_sql");
        assertMediumText("manager_acceleration_table", "recommendation_note");
        assertMediumText("manager_query_datasource_config", "jdbc_url");
        assertEquals("SELECT customer_id, total_amount FROM sales WHERE ds = ?",
                jdbcTemplate.queryForObject(
                        "SELECT clean_sql_sample FROM manager_sql_pattern_stats WHERE sql_fingerprint = ?",
                        String.class,
                        "legacy-fingerprint"));
        assertEquals("{\"dimensions\":[\"customer_id\"],\"metrics\":[\"total_amount\"]}",
                jdbcTemplate.queryForObject(
                        "SELECT signature_json FROM manager_sql_pattern_stats WHERE sql_fingerprint = ?",
                        String.class,
                        "legacy-fingerprint"));
        assertEquals("CREATE TABLE analytics.daily_sales_rollup AS SELECT * FROM sales_daily",
                jdbcTemplate.queryForObject(
                        "SELECT ddl_text FROM manager_acceleration_table WHERE name = ?",
                        String.class,
                        "daily_sales_rollup"));
        assertEquals("REFRESH TABLE analytics.daily_sales_rollup",
                jdbcTemplate.queryForObject(
                        "SELECT refresh_sql FROM manager_acceleration_table WHERE name = ?",
                        String.class,
                        "daily_sales_rollup"));
        assertEquals("legacy recommendation note that must survive V11 rebuild",
                jdbcTemplate.queryForObject(
                        "SELECT recommendation_note FROM manager_acceleration_table WHERE name = ?",
                        String.class,
                        "daily_sales_rollup"));
        assertEquals("jdbc:kylin://localhost:17070/learn_kylin",
                jdbcTemplate.queryForObject(
                        "SELECT jdbc_url FROM manager_query_datasource_config WHERE name = ?",
                        String.class,
                        "default"));
        assertEquals("jdbc:trino://localhost:18080/tpch/tiny",
                jdbcTemplate.queryForObject(
                        "SELECT jdbc_url FROM manager_query_datasource_config WHERE name = ?",
                        String.class,
                        "trino_local"));
    }

    private void assertMediumText(String tableName, String columnName) {
        assertEquals("mediumtext", jdbcTemplate.queryForObject(
                "SELECT LOWER(data_type) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE LOWER(table_schema) = LOWER(DATABASE()) "
                        + "AND LOWER(table_name) = LOWER(?) "
                        + "AND LOWER(column_name) = LOWER(?)",
                String.class,
                tableName,
                columnName));
    }

    private static String legacyFlywayJdbcUrl() {
        return ManagerTestFixtures.mysqlJdbcUrlWithInit(
                "manager.test.flyway.legacy-db-name",
                "manager.test.flyway.legacy-v8-resource");
    }
}
