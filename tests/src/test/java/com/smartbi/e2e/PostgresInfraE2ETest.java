package com.smartbi.e2e;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies PostgreSQL infrastructure and schema correctness.
 * Checks that all expected tables exist with the correct columns,
 * and that Flyway migrations have run cleanly.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgresInfraE2ETest extends E2ETestBase {

    @Test
    @Order(1)
    @DisplayName("PostgreSQL is reachable and engine_db is accessible")
    void testPgConnectivity() throws SQLException {
        try (Connection c = pgConnection()) {
            assertThat(c.isClosed()).isFalse();
            assertThat(c.getCatalog()).isEqualTo("engine_db");
        }
    }

    @Test
    @Order(2)
    @DisplayName("sql_execution_record table exists with required columns")
    void testSqlExecutionRecordSchema() throws SQLException {
        try (Connection c = pgConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            ResultSet cols = meta.getColumns(null, "public", "sql_execution_record", null);
            java.util.Set<String> colNames = new java.util.HashSet<>();
            while (cols.next()) colNames.add(cols.getString("COLUMN_NAME").toLowerCase());
            assertThat(colNames).contains(
                "id", "original_sql", "datasource_name", "execution_mode",
                "success", "duration_ms", "sql_fingerprint", "created_at"
            );
        }
    }

    @Test
    @Order(3)
    @DisplayName("sql_pattern_stats table exists with required columns")
    void testSqlPatternStatsSchema() throws SQLException {
        try (Connection c = pgConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            ResultSet cols = meta.getColumns(null, "public", "sql_pattern_stats", null);
            java.util.Set<String> colNames = new java.util.HashSet<>();
            while (cols.next()) colNames.add(cols.getString("COLUMN_NAME").toLowerCase());
            assertThat(colNames).contains(
                "id", "sql_fingerprint", "execution_count", "sample_sql"
            );
        }
    }

    @Test
    @Order(4)
    @DisplayName("acceleration_table table exists with required columns")
    void testAccelerationTableSchema() throws SQLException {
        try (Connection c = pgConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            ResultSet cols = meta.getColumns(null, "public", "acceleration_table", null);
            java.util.Set<String> colNames = new java.util.HashSet<>();
            while (cols.next()) colNames.add(cols.getString("COLUMN_NAME").toLowerCase());
            assertThat(colNames).contains("id", "table_name", "status", "source");
        }
    }

    @Test
    @Order(5)
    @DisplayName("query_datasource_config table exists with required columns")
    void testQueryDatasourceConfigSchema() throws SQLException {
        try (Connection c = pgConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            ResultSet cols = meta.getColumns(null, "public", "query_datasource_config", null);
            java.util.Set<String> colNames = new java.util.HashSet<>();
            while (cols.next()) colNames.add(cols.getString("COLUMN_NAME").toLowerCase());
            assertThat(colNames).contains(
                "id", "name", "type", "driver_class", "jdbc_url",
                "username", "is_default", "created_at"
            );
        }
    }

    @Test
    @Order(6)
    @DisplayName("Flyway schema history records all migrations as successful")
    void testFlywayMigrationsAllSucceeded() throws SQLException {
        try (Connection c = pgConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT count(*) AS failed FROM flyway_schema_history WHERE success = false")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("failed")).isEqualTo(0);
        }
    }

    @Test
    @Order(7)
    @DisplayName("query_datasource_config is seeded with default Kylin datasource")
    void testDatasourceConfigSeeded() throws SQLException {
        int rows = countPgRows(
            "SELECT count(*) FROM query_datasource_config WHERE name = 'default'");
        assertThat(rows).isGreaterThanOrEqualTo(1);
    }
}
