package com.smartbi.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that every query execution writes a correct SqlExecutionRecord to
 * MySQL and that manager's /api/v1/traces API reflects it.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TraceRecordE2ETest extends E2ETestBase {

    private static final String UNIQUE_SQL =
            "SELECT 42 AS trace_probe_" + System.currentTimeMillis();
    private static final String PRESTO_SQL =
            "/* YH_TARGET_ENGINE=presto_local */ SELECT 1 AS presto_trace_probe";

    @Test
    @Order(1)
    @DisplayName("Statement query writes a trace record to MySQL")
    void testStatementTraceWrittenToMysql() throws Exception {
        querySpec().body(statementRequest(UNIQUE_SQL)).post("/kylin/api/query");

        waitFor(5_000, "trace row in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM manager_sql_execution_record WHERE original_sql = ?",
                    UNIQUE_SQL) > 0;
            } catch (Exception e) { return false; }
        });

        try (Connection c = mysqlConnection();
             ResultSet rs = queryMysql(c,
                 "SELECT datasource_name, execution_mode, success, duration_ms "
               + "FROM manager_sql_execution_record WHERE original_sql = ? LIMIT 1",
                 UNIQUE_SQL)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("datasource_name")).isNotBlank();
            assertThat(rs.getString("execution_mode")).isEqualTo("STATEMENT");
            assertThat(rs.getBoolean("success")).isTrue();
            assertThat(rs.getLong("duration_ms")).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Prepared-statement query writes PREPARED_STATEMENT executionMode to MySQL")
    void testPreparedTraceWrittenToMysql() throws Exception {
        String sql = "SELECT count(*) FROM KYLIN_SALES WHERE PART_DT > ?";
        querySpec().body(preparedRequest(sql, java.sql.Date.valueOf("2010-01-01")))
                   .post("/kylin/api/query");

        waitFor(5_000, "prepared trace row in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM manager_sql_execution_record WHERE execution_mode = 'PREPARED_STATEMENT'") > 0;
            } catch (Exception e) { return false; }
        });

        try (Connection c = mysqlConnection();
             ResultSet rs = queryMysql(c,
                 "SELECT execution_mode FROM manager_sql_execution_record "
               + "WHERE execution_mode = 'PREPARED_STATEMENT' ORDER BY received_at DESC LIMIT 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("execution_mode")).isEqualTo("PREPARED_STATEMENT");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Failed query writes success=false trace with non-null error message")
    void testFailedQueryTraceWritten() throws Exception {
        String badSql = "SELECT * FROM nonexistent_table_xyz_" + System.currentTimeMillis();
        querySpec().body(statementRequest(badSql)).post("/kylin/api/query");

        waitFor(5_000, "failed trace row in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM manager_sql_execution_record WHERE success = false AND original_sql = ?",
                    badSql) > 0;
            } catch (Exception e) { return false; }
        });

        try (Connection c = mysqlConnection();
             ResultSet rs = queryMysql(c,
                 "SELECT success, error_message FROM manager_sql_execution_record WHERE original_sql = ? LIMIT 1",
                 badSql)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("success")).isFalse();
            assertThat(rs.getString("error_message")).isNotBlank();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Presto-routed query records correct datasource_name in MySQL")
    void testPrestoRouteTraceHasCorrectDatasource() throws Exception {
        querySpec().body(statementRequest(PRESTO_SQL)).post("/kylin/api/query");

        waitFor(5_000, "presto trace in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM manager_sql_execution_record WHERE datasource_name = ?",
                    E2EConfig.PRESTO_DS_NAME) > 0;
            } catch (Exception e) { return false; }
        });

        try (Connection c = mysqlConnection();
             ResultSet rs = queryMysql(c,
                 "SELECT datasource_name FROM manager_sql_execution_record "
               + "WHERE datasource_name = ? ORDER BY received_at DESC LIMIT 1",
                 E2EConfig.PRESTO_DS_NAME)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("datasource_name")).isEqualTo(E2EConfig.PRESTO_DS_NAME);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Manager /api/v1/traces returns records written by query")
    void testManagerApiReturnsTraceRecords() {
        Response r = managerSpec()
                .get("/api/v1/traces?page=0&size=10")
                .then().statusCode(200).extract().response();

        List<Map<String, Object>> content = r.jsonPath().getList("content");
        assertThat(content).isNotEmpty();
        Map<String, Object> first = content.get(0);
        assertThat(first).containsKey("originalSql");
        assertThat(first).containsKey("datasourceName");
        assertThat(first).containsKey("executionMode");
        assertThat(first).containsKey("cacheHit");
        assertThat(first).containsKey("parseStatus");
    }

    @Test
    @Order(6)
    @DisplayName("Pattern stats increment execution_count for repeated identical SQL")
    void testPatternStatsIncrement() throws Exception {
        String sql = "SELECT 1 AS pattern_probe";
        // get baseline
        int before = countMysqlRows(
            "SELECT coalesce(sum(execution_count),0) FROM manager_sql_pattern_stats WHERE clean_sql_sample = ?", sql);
        // execute twice more
        querySpec().body(statementRequest(sql)).post("/kylin/api/query");
        querySpec().body(statementRequest(sql)).post("/kylin/api/query");

        waitFor(10_000, "pattern stats updated", () -> {
            try {
                return countMysqlRows(
                    "SELECT coalesce(sum(execution_count),0) FROM manager_sql_pattern_stats WHERE clean_sql_sample = ?",
                    sql) > before;
            } catch (Exception e) { return false; }
        });

        int after = countMysqlRows(
            "SELECT coalesce(sum(execution_count),0) FROM manager_sql_pattern_stats WHERE clean_sql_sample = ?", sql);
        assertThat(after).isGreaterThan(before);
    }
}
