package com.smartbi.query.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.EngineQueryApplication;
import com.smartbi.query.support.InMemoryQueryInfrastructure;
import com.smartbi.query.support.QueryTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = EngineQueryApplication.class,
        properties = {
                "engine.query.datasource.default.name=default",
                "engine.query.datasource.default.type=h2",
                "engine.query.datasource.default.driver-class=org.h2.Driver",
                "engine.query.datasource.default.jdbc-url=jdbc:h2:mem:webtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "engine.query.datasource.default.username=sa",
                "engine.query.datasource.default.password=",
                "engine.query.datasource.named.presto_local.name=presto_local",
                "engine.query.datasource.named.presto_local.type=presto",
                "engine.query.datasource.named.presto_local.driver-class=org.h2.Driver",
                "engine.query.datasource.named.presto_local.jdbc-url=jdbc:h2:mem:webtest_presto;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "engine.query.datasource.named.presto_local.username=sa",
                "engine.query.datasource.named.presto_local.password=",
                "engine.query.auth.username=ADMIN",
                "engine.query.auth.password=KYLIN"
        }
)
@AutoConfigureMockMvc
@Import(QueryTestConfiguration.class)
class QueryWebIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryQueryInfrastructure infrastructure;

    @BeforeEach
    void setUp() throws Exception {
        infrastructure.clear();
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:webtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS SALES");
            statement.execute("CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')");
        }
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:webtest_presto;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS NATION");
            statement.execute("CREATE TABLE NATION (NATIONKEY INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO NATION (NATIONKEY, NAME) VALUES (1, 'presto-alpha'), (2, 'presto-beta')");
        }
    }

    @Test
    void shouldRejectRemovedNonQueryEndpoints() throws Exception {
        mockMvc.perform(post("/kylin/api/user/authentication")
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/kylin/api/tables_and_columns")
                        .param("project", "demo")
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldServeCacheHitOnSecondQuery() throws Exception {
        String body = "{\"sql\":\"SELECT NAME FROM SALES ORDER BY ID\",\"project\":\"demo\"}";

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageCacheUsed").value(false))
                .andExpect(jsonPath("$.results[0][0]").value("alpha"));

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageCacheUsed").value(true))
                .andExpect(jsonPath("$.results[1][0]").value("beta"));
    }

    @Test
    void shouldReturnKylinStyleExceptionPayloadForNonQuerySql() throws Exception {
        String body = "{\"sql\":\"DELETE FROM SALES WHERE ID = 1\",\"project\":\"demo\"}";

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isException").value(true))
                .andExpect(jsonPath("$.exceptionMessage").exists());
    }

    @Test
    void shouldPublishStatementExecutionModeInTracePayload() throws Exception {
        String body = "{\"sql\":\"SELECT NAME FROM SALES ORDER BY ID\",\"project\":\"demo\"}";

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        JsonNode trace = lastTrace();
        org.junit.jupiter.api.Assertions.assertEquals("STATEMENT", trace.path("executionMode").asText());
        org.junit.jupiter.api.Assertions.assertTrue(trace.path("parameterPayload").isMissingNode()
                || trace.path("parameterPayload").isNull());
    }

    @Test
    void shouldPublishPreparedExecutionModeInTracePayload() throws Exception {
        String body = "{\"sql\":\"SELECT NAME FROM SALES WHERE ID = ?\",\"project\":\"demo\",\"params\":[{\"className\":\"java.lang.Integer\",\"value\":\"1\"}]}";

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0][0]").value("alpha"));

        JsonNode trace = lastTrace();
        org.junit.jupiter.api.Assertions.assertEquals("PREPARED_STATEMENT", trace.path("executionMode").asText());
        org.junit.jupiter.api.Assertions.assertTrue(trace.path("parameterPayload").isMissingNode()
                || trace.path("parameterPayload").isNull());
    }

    @Test
    void shouldPublishReadableParameterPayloadForFailedPreparedExecution() throws Exception {
        String body = "{\"sql\":\"SELECT NAME FROM SALES WHERE ID = ?\",\"project\":\"demo\",\"params\":[{\"className\":\"java.lang.Integer\",\"value\":\"not-a-number\"}]}";

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isException").value(true));

        JsonNode trace = lastTrace();
        org.junit.jupiter.api.Assertions.assertEquals("PREPARED_STATEMENT", trace.path("executionMode").asText());
        org.junit.jupiter.api.Assertions.assertEquals(
                "[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"not-a-number\"}]",
                trace.path("parameterPayload").asText());
    }

    @Test
    void shouldRouteByPreservedMetadataHint() throws Exception {
        String body = "{\"sql\":\"/* YH_TARGET_ENGINE=presto_local */ SELECT NAME FROM NATION WHERE NATIONKEY = 1\",\"project\":\"demo\"}";

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0][0]").value("presto-alpha"));
    }

    private static String authHeader() {
        return "Basic " + Base64.getEncoder().encodeToString("ADMIN:KYLIN".getBytes());
    }

    private JsonNode lastTrace() throws Exception {
        java.util.List<String> traces = infrastructure.publishedTraces();
        org.junit.jupiter.api.Assertions.assertFalse(traces.isEmpty());
        return JSON.readTree(traces.get(traces.size() - 1));
    }
}
