package com.smartbi.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all E2E tests.
 * Provides helpers for HTTP, MySQL, and Redis interactions.
 */
public abstract class E2ETestBase {

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // --- HTTP helpers ---

    protected static RequestSpecification querySpec() {
        return RestAssured.given()
                .baseUri(E2EConfig.QUERY_URL)
                .header("Authorization", E2EConfig.QUERY_AUTH_HEADER)
                .contentType(ContentType.JSON);
    }

    protected static RequestSpecification managerSpec() {
        return RestAssured.given()
                .baseUri(E2EConfig.MANAGER_URL)
                .contentType(ContentType.JSON);
    }

    protected static Map<String, Object> statementRequest(String sql) {
        Map<String, Object> body = new HashMap<>();
        body.put("sql", sql);
        body.put("project", E2EConfig.KYLIN_PROJECT);
        body.put("acceptPartial", false);
        return body;
    }

    protected static Map<String, Object> preparedRequest(String sql, Object... params) {
        Map<String, Object> body = new HashMap<>();
        body.put("sql", sql);
        body.put("project", E2EConfig.KYLIN_PROJECT);
        body.put("acceptPartial", false);
        java.util.List<Map<String, String>> paramList = new java.util.ArrayList<>();
        for (Object p : params) {
            Map<String, String> param = new HashMap<>();
            param.put("className", p.getClass().getName());
            param.put("value", String.valueOf(p));
            paramList.add(param);
        }
        body.put("params", paramList);
        return body;
    }

    // --- MySQL helpers ---

    protected static Connection mysqlConnection() throws SQLException {
        return DriverManager.getConnection(E2EConfig.MYSQL_URL, E2EConfig.MYSQL_USER, E2EConfig.MYSQL_PASSWORD);
    }

    protected static int countMysqlRows(String sql, Object... args) throws SQLException {
        try (Connection c = mysqlConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    protected static ResultSet queryMysql(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
        return ps.executeQuery();
    }

    // --- Redis helpers ---

    protected static Jedis jedis() {
        return new Jedis(E2EConfig.REDIS_HOST, E2EConfig.REDIS_PORT);
    }

    protected static long redisCacheKeyCount() {
        try (Jedis j = jedis()) {
            return j.keys(E2EConfig.REDIS_CACHE_KEY_PREFIX + "*").size();
        }
    }

    /** Wait up to maxMs for a condition, polling every 200ms. */
    protected static void waitFor(long maxMs, String desc, java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + maxMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        throw new AssertionError("Timed out waiting for: " + desc);
    }
}
