package com.smartbi.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Trino routing:
 * - HTTP query with YH_TARGET_ENGINE hint routes to Trino.
 * - Direct Trino JDBC connection works.
 * - Trace records show the correct datasource_name.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrinoRoutingE2ETest extends E2ETestBase {

    private static final String TRINO_SQL_HTTP =
            "/* YH_TARGET_ENGINE=trino_local */ SELECT count(*) AS nation_cnt FROM tpch.tiny.nation";

    @BeforeAll
    static void loadTrinoDriver() throws ClassNotFoundException {
        Class.forName("io.trino.jdbc.TrinoDriver");
    }

    @Test
    @Order(1)
    @DisplayName("YH_TARGET_ENGINE comment routes request to Trino via query HTTP API")
    void testYhTargetEngineRoutesToTrino() {
        Response r = querySpec()
                .body(statementRequest(TRINO_SQL_HTTP))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        assertThat(r.jsonPath().getList("results")).isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("Trino-routed trace records datasource_name=trino_local in MySQL")
    void testTrinoTraceHasCorrectDatasource() throws Exception {
        querySpec().body(statementRequest(TRINO_SQL_HTTP)).post("/kylin/api/query");

        waitFor(5_000, "trino trace in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM manager_sql_execution_record WHERE datasource_name = ?",
                    E2EConfig.TRINO_DS_NAME) > 0;
            } catch (Exception e) { return false; }
        });

        try (java.sql.Connection c = mysqlConnection();
             ResultSet rs = queryMysql(c,
                 "SELECT datasource_name, success, execution_mode "
               + "FROM manager_sql_execution_record WHERE datasource_name = ? "
               + "ORDER BY received_at DESC LIMIT 1",
                 E2EConfig.TRINO_DS_NAME)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("datasource_name")).isEqualTo(E2EConfig.TRINO_DS_NAME);
            assertThat(rs.getBoolean("success")).isTrue();
            assertThat(rs.getString("execution_mode")).isEqualTo("STATEMENT");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Direct Trino JDBC connection to Trino server works")
    void testDirectTrinoJdbcConnection() throws SQLException {
        try (Connection c = DriverManager.getConnection(
                E2EConfig.TRINO_JDBC_URL, "admin", "")) {
            assertThat(c.isClosed()).isFalse();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Direct Trino JDBC query returns results")
    void testDirectTrinoJdbcQuery() throws SQLException {
        try (Connection c = DriverManager.getConnection(
                     E2EConfig.TRINO_JDBC_URL, "admin", "");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) AS cnt FROM tpch.tiny.nation")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("cnt")).isGreaterThan(0);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Trino routed query via query service matches direct Trino count")
    void testTrinoRoutedCountMatchesDirect() throws Exception {
        // Get count via query service HTTP API
        Response r = querySpec()
                .body(statementRequest(TRINO_SQL_HTTP))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();
        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        String routedCount = r.jsonPath().getList("results").get(0).toString();

        // Get count directly from Trino
        try (Connection c = DriverManager.getConnection(
                     E2EConfig.TRINO_JDBC_URL, "admin", "");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) AS cnt FROM tpch.tiny.nation")) {
            assertThat(rs.next()).isTrue();
            long directCount = rs.getLong("cnt");
            // routed result string contains the count value
            assertThat(routedCount).contains(String.valueOf(directCount));
        }
    }
}
