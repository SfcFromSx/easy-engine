package com.smartbi.e2e;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the standard Apache Kylin JDBC driver connects directly to the
 * query service (port 8092) and executes SQL correctly.
 *
 * The JDBC URL points at query, not at the real Kylin server, which is the key
 * proof that kylin-jdbc-cache is no longer needed in the data path.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KylinJdbcE2ETest extends E2ETestBase {

    /** jdbc:kylin://localhost:8092/learn_kylin — query service, not Kylin directly */
    private static final String JDBC_URL =
            "jdbc:kylin://localhost:8092/" + E2EConfig.KYLIN_PROJECT;

    @BeforeAll
    static void loadDriver() throws ClassNotFoundException {
        Class.forName("org.apache.kylin.jdbc.Driver");
    }

    @Test
    @Order(1)
    @DisplayName("Kylin JDBC can open a connection to the query service")
    void testJdbcConnectionToQueryService() throws SQLException {
        try (Connection c = DriverManager.getConnection(
                JDBC_URL, E2EConfig.KYLIN_USER, E2EConfig.KYLIN_PASS)) {
            assertThat(c.isClosed()).isFalse();
        }
    }

    @Test
    @Order(2)
    @DisplayName("Kylin JDBC Statement executes SELECT and returns ResultSet")
    void testJdbcStatementQuery() throws SQLException {
        try (Connection c = DriverManager.getConnection(
                     JDBC_URL, E2EConfig.KYLIN_USER, E2EConfig.KYLIN_PASS);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) AS cnt FROM KYLIN_SALES")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("cnt")).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Kylin JDBC PreparedStatement binds parameters correctly")
    void testJdbcPreparedStatement() throws SQLException {
        String sql = "SELECT count(*) AS cnt FROM KYLIN_SALES WHERE PART_DT > ?";
        try (Connection c = DriverManager.getConnection(
                     JDBC_URL, E2EConfig.KYLIN_USER, E2EConfig.KYLIN_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf("2010-01-01"));
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong("cnt")).isGreaterThanOrEqualTo(0);
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Kylin JDBC ResultSetMetaData exposes correct column names and types")
    void testJdbcResultSetMetaData() throws SQLException {
        try (Connection c = DriverManager.getConnection(
                     JDBC_URL, E2EConfig.KYLIN_USER, E2EConfig.KYLIN_PASS);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) AS cnt FROM KYLIN_SALES")) {
            ResultSetMetaData meta = rs.getMetaData();
            assertThat(meta.getColumnCount()).isGreaterThanOrEqualTo(1);
            assertThat(meta.getColumnLabel(1)).isEqualToIgnoringCase("cnt");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Kylin JDBC query triggers a trace record in MySQL")
    void testJdbcQueryWritesTrace() throws Exception {
        String sql = "SELECT count(*) AS jdbc_trace_probe FROM KYLIN_SALES";
        try (Connection c = DriverManager.getConnection(
                     JDBC_URL, E2EConfig.KYLIN_USER, E2EConfig.KYLIN_PASS);
             Statement st = c.createStatement()) {
            st.executeQuery(sql);
        }

        waitFor(5_000, "JDBC trace in MySQL", () -> {
            try {
                return countMysqlRows(
                    "SELECT count(*) FROM manager_sql_execution_record WHERE original_sql LIKE '%jdbc_trace_probe%'") > 0;
            } catch (Exception e) { return false; }
        });

        int rows = countMysqlRows(
            "SELECT count(*) FROM manager_sql_execution_record WHERE original_sql LIKE '%jdbc_trace_probe%'");
        assertThat(rows).isGreaterThan(0);
    }

    @Test
    @Order(6)
    @DisplayName("Kylin JDBC invalid SQL returns SQLException (not a raw crash)")
    void testJdbcInvalidSqlThrowsSqlException() {
        org.junit.jupiter.api.Assertions.assertThrows(SQLException.class, () -> {
            try (Connection c = DriverManager.getConnection(
                         JDBC_URL, E2EConfig.KYLIN_USER, E2EConfig.KYLIN_PASS);
                 Statement st = c.createStatement()) {
                st.executeQuery("NOT VALID SQL !!!");
            }
        });
    }

    @Test
    @Order(7)
    @DisplayName("Kylin direct REST is reachable and returns 200 on authentication")
    void testKylinDirectAuthEndpoint() {
        managerSpec() // reuse JSON spec; just need an HTTP client
                .relaxedHTTPSValidation()
                .auth().preemptive().basic(E2EConfig.KYLIN_USER, E2EConfig.KYLIN_PASS)
                .get("http://" + E2EConfig.KYLIN_HOST + ":" + E2EConfig.KYLIN_PORT
                        + "/kylin/api/user/authentication")
                .then().statusCode(200);
    }
}
