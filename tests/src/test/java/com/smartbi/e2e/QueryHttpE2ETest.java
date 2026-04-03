package com.smartbi.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the query HTTP API (POST /kylin/api/query).
 * Covers statement, prepared-statement, error, and routing scenarios.
 * Prerequisites: all docker-compose services running, including --profile olap.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryHttpE2ETest extends E2ETestBase {

    private static final String SIMPLE_SQL  = "SELECT 1 AS val";
    private static final String KYLIN_SQL   = "SELECT count(*) AS cnt FROM KYLIN_SALES";
    private static final String INVALID_SQL = "NOT A VALID SQL !!!";
    private static final String DML_SQL     = "INSERT INTO foo VALUES (1)";
    private static final String PRESTO_SQL  =
            "/* ENGINE=presto_local */ SELECT count(*) AS cnt FROM tpch.tiny.nation";

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Statement query returns non-exception response with column metadata")
    void testStatementQuerySuccess() {
        Response r = querySpec()
                .body(statementRequest(SIMPLE_SQL))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        assertThat(r.jsonPath().getList("columnMetas")).isNotEmpty();
        assertThat(r.jsonPath().getList("results")).isNotEmpty();
        assertThat(r.jsonPath().getLong("duration")).isGreaterThanOrEqualTo(0);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Kylin OLAP query returns results and storageCacheUsed=false on first run")
    void testKylinOlapQueryFirstRun() {
        Response r = querySpec()
                .body(statementRequest(KYLIN_SQL))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        assertThat(r.jsonPath().getList("results")).isNotEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Same query second run returns storageCacheUsed=true from Redis cache")
    void testCacheHitOnSecondQuery() {
        // warm up
        querySpec().body(statementRequest(KYLIN_SQL)).post("/kylin/api/query");
        // second call should be cached
        Response r = querySpec()
                .body(statementRequest(KYLIN_SQL))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        assertThat(r.jsonPath().getBoolean("storageCacheUsed")).isTrue();
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Prepared-statement query with params succeeds")
    void testPreparedStatementQuerySuccess() {
        String sql = "SELECT count(*) AS cnt FROM KYLIN_SALES WHERE PART_DT > ?";
        Response r = querySpec()
                .body(preparedRequest(sql, java.sql.Date.valueOf("2010-01-01")))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        assertThat(r.jsonPath().getList("results")).isNotEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Invalid SQL returns isException=true with non-blank message")
    void testInvalidSqlReturnsException() {
        Response r = querySpec()
                .body(statementRequest(INVALID_SQL))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isTrue();
        assertThat(r.jsonPath().getString("exceptionMessage")).isNotBlank();
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("DML SQL is rejected as non-query with isException=true")
    void testDmlSqlRejected() {
        Response r = querySpec()
                .body(statementRequest(DML_SQL))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isTrue();
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("ENGINE hint routes to Presto and returns results")
    void testRoutingViaYhTargetEngine() {
        Response r = querySpec()
                .body(statementRequest(PRESTO_SQL))
                .post("/kylin/api/query")
                .then().statusCode(200).extract().response();

        assertThat(r.jsonPath().getBoolean("isException")).isFalse();
        assertThat(r.jsonPath().getList("results")).isNotEmpty();
    }
}
