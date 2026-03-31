package com.smartbi.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the manager control-plane API.
 * Covers: datasource config CRUD, trace browsing, pattern stats, acceleration lifecycle.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ManagerApiE2ETest extends E2ETestBase {

    private static Long createdDsId;

    // --- Datasource config CRUD ---

    @Test
    @Order(1)
    @DisplayName("GET /api/v1/query-datasources returns seeded datasources")
    void testListDatasources() {
        Response r = managerSpec()
                .get("/api/v1/query-datasources")
                .then().statusCode(200).extract().response();

        List<Map<String, Object>> list = r.jsonPath().getList("");
        assertThat(list).isNotEmpty();
        assertThat(list.stream().anyMatch(d -> "default".equals(d.get("name")))).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/query-datasources creates a new datasource config")
    void testCreateDatasource() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "e2e_test_ds");
        body.put("type", "presto");
        body.put("driverClass", "com.facebook.presto.jdbc.PrestoDriver");
        body.put("jdbcUrl", "jdbc:presto://localhost:18081/tpch/tiny");
        body.put("username", "admin");
        body.put("password", "");
        body.put("maxPoolSize", 2);
        body.put("minIdle", 1);
        body.put("connectionTimeoutMs", 5000);
        body.put("isDefault", false);

        Response r = managerSpec()
                .body(body)
                .post("/api/v1/query-datasources")
                .then().statusCode(200).extract().response();

        createdDsId = r.jsonPath().getLong("id");
        assertThat(createdDsId).isGreaterThan(0);
        assertThat(r.jsonPath().getString("name")).isEqualTo("e2e_test_ds");
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/query-datasources/{id} returns the created datasource")
    void testGetDatasource() {
        assertThat(createdDsId).isNotNull();
        Response r = managerSpec()
                .get("/api/v1/query-datasources/" + createdDsId)
                .then().statusCode(200).extract().response();
        assertThat(r.jsonPath().getString("name")).isEqualTo("e2e_test_ds");
        assertThat(r.jsonPath().getString("type")).isEqualTo("presto");
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/v1/query-datasources/{id} updates the datasource")
    void testUpdateDatasource() {
        assertThat(createdDsId).isNotNull();
        Map<String, Object> body = new HashMap<>();
        body.put("name", "e2e_test_ds");
        body.put("type", "presto");
        body.put("driverClass", "com.facebook.presto.jdbc.PrestoDriver");
        body.put("jdbcUrl", "jdbc:presto://localhost:18081/tpch/sf1");
        body.put("username", "admin");
        body.put("password", "");
        body.put("maxPoolSize", 4);
        body.put("minIdle", 1);
        body.put("connectionTimeoutMs", 5000);
        body.put("isDefault", false);

        Response r = managerSpec()
                .body(body)
                .put("/api/v1/query-datasources/" + createdDsId)
                .then().statusCode(200).extract().response();
        assertThat(r.jsonPath().getString("jdbcUrl")).contains("sf1");
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /api/v1/query-datasources/{id} removes the datasource")
    void testDeleteDatasource() {
        assertThat(createdDsId).isNotNull();
        managerSpec().delete("/api/v1/query-datasources/" + createdDsId).then().statusCode(200);
        managerSpec().get("/api/v1/query-datasources/" + createdDsId).then().statusCode(404);
    }

    // --- Trace browsing ---

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/traces returns paginated trace list with required fields")
    void testTraceBrowsing() {
        // ensure at least one trace exists
        querySpec().body(statementRequest("SELECT 1")).post("/kylin/api/query");
        waitFor(5_000, "trace visible in manager", () ->
            managerSpec().get("/api/v1/traces?page=0&size=1")
                         .jsonPath().getList("content").size() > 0);

        Response r = managerSpec().get("/api/v1/traces?page=0&size=10")
                .then().statusCode(200).extract().response();
        List<Map<String, Object>> content = r.jsonPath().getList("content");
        assertThat(content).isNotEmpty();
        Map<String, Object> row = content.get(0);
        assertThat(row).containsKeys("originalSql", "datasourceName", "executionMode",
                                     "success", "durationMs", "sqlFingerprint");
    }

    // --- Stats summary ---

    @Test
    @Order(7)
    @DisplayName("GET /api/v1/stats returns summary with positive totalTraces")
    void testStatsSummary() {
        Response r = managerSpec().get("/api/v1/stats")
                .then().statusCode(200).extract().response();
        assertThat(r.jsonPath().getLong("totalTraces")).isGreaterThan(0);
        assertThat(r.jsonPath().getMap("")).containsKeys(
                "totalTraces", "parseOk", "parseError", "patternCount",
                "activeAccelCount", "draftAccelCount", "cacheHitCount");
    }

    // --- Acceleration lifecycle ---

    @Test
    @Order(8)
    @DisplayName("Acceleration table can be created, status updated, and deleted")
    void testAccelerationLifecycle() {
        // create
        Map<String, Object> body = new HashMap<>();
        body.put("tableName", "e2e_accel_" + System.currentTimeMillis());
        body.put("datasourceName", "default");
        body.put("status", "DRAFT");
        body.put("source", "MANUAL");

        Response created = managerSpec().body(body).post("/api/v1/accelerations")
                .then().statusCode(200).extract().response();
        long id = created.jsonPath().getLong("id");
        assertThat(id).isGreaterThan(0);
        assertThat(created.jsonPath().getString("status")).isEqualTo("DRAFT");

        // update status
        Map<String, String> statusBody = new HashMap<>();
        statusBody.put("status", "ACTIVE");
        managerSpec().body(statusBody).put("/api/v1/accelerations/" + id + "/status")
                .then().statusCode(200);

        // verify
        Response fetched = managerSpec().get("/api/v1/accelerations/" + id)
                .then().statusCode(200).extract().response();
        assertThat(fetched.jsonPath().getString("status")).isEqualTo("ACTIVE");

        // delete
        managerSpec().delete("/api/v1/accelerations/" + id).then().statusCode(200);
    }
}
