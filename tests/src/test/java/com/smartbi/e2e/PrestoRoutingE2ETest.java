package com.smartbi.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Presto routing:
 * - HTTP query with YH_TARGET_ENGINE hint routes to Presto.
 * - Direct Presto JDBC connection works.
 * - Trace records show the correct datasource_name.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PrestoRoutingE2ETest extends E2ETestBase {

    private static final String PRESTO_SQL_HTTP =
            "/* YH_TARGET_ENGINE=presto_local */ SELECT count(*) AS nation_cnt FROM tpch.tiny.nation";
    private static final String PRESTO_SQL_ENGINE_HINT =
            "-- engine:presto_local\nSELECT 1 AS presto_hint_probe";

    @BeforeAll
    static void loadPrestoDriver() throws ClassNotFoundException {
        Class.forName("com.facebook.presto.jdbc.PrestoDriver");
    }

    @Test
    @Order(1)
    @DisplayName("YH_TARGET_ENGINE comment routes request to Presto via query HTTP API")
    void testYhTargetEngineRoutesToPresto() {
        Response r = querySpec()
                .body(statementRequest(PRESTO_SQL_HTTP))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        assertThat(r.jsonPath().getList("results")).isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("Presto-routed trace records datasource_name=presto_local in MySQL")
    void testPrestoTraceHasCorrectDatasource() throws Exception {
        querySpec().body(statementRequest(PRESTO_SQL_HTTP)).post("/kylin/api/query");

        waitFor(5_000, "presto trace in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM sql_execution_record WHERE datasource_name = ?",
                    E2EConfig.PRESTO_DS_NAME) > 0;
            } catch (Exception e) { return false; }
        });

        try (java.sql.Connection c = mysqlConnection();
             ResultSet rs = queryMysql(c,
                 "SELECT datasource_name, success, execution_mode "
               + "FROM sql_execution_record WHERE datasource_name = ? "
               + "ORDER BY received_at DESC LIMIT 1",
                 E2EConfig.PRESTO_DS_NAME)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("datasource_name")).isEqualTo(E2EConfig.PRESTO_DS_NAME);
            assertThat(rs.getBoolean("success")).isTrue();
            assertThat(rs.getString("execution_mode")).isEqualTo("STATEMENT");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Direct Presto JDBC connection to Presto server works")
    void testDirectPrestoJdbcConnection() throws SQLException {
        try (Connection c = DriverManager.getConnection(
                E2EConfig.PRESTO_JDBC_URL, "admin", "")) {
            assertThat(c.isClosed()).isFalse();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Direct Presto JDBC query returns results")
    void testDirectPrestoJdbcQuery() throws SQLException {
        try (Connection c = DriverManager.getConnection(
                     E2EConfig.PRESTO_JDBC_URL, "admin", "");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) AS cnt FROM tpch.tiny.nation")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("cnt")).isGreaterThan(0);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Presto routed query via query service matches direct Presto count")
    void testPrestoRoutedCountMatchesDirect() throws Exception {
        // Get count via query service HTTP API
        Response r = querySpec()
                .body(statementRequest(PRESTO_SQL_HTTP))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();
        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        String routedCount = r.jsonPath().getList("results").get(0).toString();

        // Get count directly from Presto
        try (Connection c = DriverManager.getConnection(
                     E2EConfig.PRESTO_JDBC_URL, "admin", "");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) AS cnt FROM tpch.tiny.nation")) {
            assertThat(rs.next()).isTrue();
            long directCount = rs.getLong("cnt");
            // routed result string contains the count value
            assertThat(routedCount).contains(String.valueOf(directCount));
        }
    }

    @Test
    @Order(6)
    @DisplayName("Manager trace API filters by datasource_name=presto_local")
    void testManagerTraceFilterByDatasource() {
        Response r = managerSpec()
                .get("/api/v1/traces?page=0&size=20")
                .then().statusCode(200).extract().response();

        java.util.List<java.util.Map<String, Object>> content = r.jsonPath().getList("content");
        assertThat(content).isNotEmpty();
        assertThat(content.stream().anyMatch(row -> E2EConfig.PRESTO_DS_NAME.equals(row.get("datasourceName")))).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("Fallback to default datasource when unknown engine hint is given")
    void testFallbackToDefaultDatasource() throws Exception {
        String sql = "/* YH_TARGET_ENGINE=nonexistent_engine */ SELECT 1 AS fallback_probe";
        Response r = querySpec()
                .body(statementRequest(sql))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();
        // Should not crash — falls back to default datasource
        assertThat(r.jsonPath().getString("exceptionMessage")).isNull();

        waitFor(5_000, "fallback trace in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM sql_execution_record WHERE original_sql LIKE '%fallback_probe%'") > 0;
            } catch (Exception e) { return false; }
        });

        try (java.sql.Connection c = mysqlConnection();
             ResultSet rs = queryMysql(c,
                 "SELECT datasource_name FROM sql_execution_record "
               + "WHERE original_sql LIKE '%fallback_probe%' ORDER BY received_at DESC LIMIT 1")) {
            assertThat(rs.next()).isTrue();
            // Should have fallen back to the default datasource, not the nonexistent one
            assertThat(rs.getString("datasource_name")).isNotEqualTo("nonexistent_engine");
        }
    }
}
