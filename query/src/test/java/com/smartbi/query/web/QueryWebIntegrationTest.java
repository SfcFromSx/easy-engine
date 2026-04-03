package com.smartbi.query.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.EngineQueryApplication;
import com.smartbi.query.support.InMemoryQueryInfrastructure;
import com.smartbi.query.support.QueryTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EngineQueryApplication.class)
@AutoConfigureMockMvc
@Import(QueryTestConfiguration.class)
class QueryWebIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String H2_DRIVER_CLASS = "org.h2.Driver";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String AUTH_SEPARATOR = ":";
    private static final String AUTH_ENDPOINT = "/kylin/api/user/authentication";
    private static final String TABLES_ENDPOINT = "/kylin/api/tables_and_columns";
    private static final String QUERY_ENDPOINT = "/kylin/api/query";
    private static final String PROJECT_PARAM = "project";
    private static final String DEMO_PROJECT = "demo";
    private static final String AUTH_RESPONSE =
            "{\"authenticated\":true,\"userDetails\":{\"username\":\"ADMIN\"}}";
    private static final String CACHE_QUERY_BODY =
            "{\"sql\":\"SELECT NAME FROM SALES ORDER BY ID\",\"project\":\"demo\"}";
    private static final String NON_QUERY_BODY =
            "{\"sql\":\"DELETE FROM SALES WHERE ID = 1\",\"project\":\"demo\"}";
    private static final String BLANK_SQL_BODY = "{\"sql\":\"   \",\"project\":\"demo\"}";
    private static final String EMPTY_REQUEST_BODY = "";
    private static final String UNSUPPORTED_REQUEST_BODY =
            "{\"sql\":\"SELECT NAME FROM SALES\",\"project\":\"demo\",\"prepareSql\":true}";
    private static final String PREPARED_SUCCESS_BODY =
            "{\"sql\":\"SELECT NAME FROM SALES WHERE ID = ?\",\"project\":\"demo\",\"params\":[{\"className\":\"java.lang.Integer\",\"value\":\"1\"}]}";
    private static final String PREPARED_FAILURE_BODY =
            "{\"sql\":\"SELECT NAME FROM SALES WHERE ID = ?\",\"project\":\"demo\",\"params\":[{\"className\":\"java.lang.Integer\",\"value\":\"not-a-number\"}]}";
    private static final String PRESTO_ROUTE_BODY =
            "{\"sql\":\"/* ENGINE=presto_local */ SELECT NAME FROM NATION WHERE NATIONKEY = 1\",\"project\":\"demo\"}";
    private static final String OPTIMIZER_HINT_BODY =
            "{\"sql\":\"/*+ INDEX(SALES IDX_SALES_NAME) */ SELECT NAME FROM SALES ORDER BY ID\",\"project\":\"demo\"}";
    private static final String QUERY_SQL_REQUIRED_MESSAGE = "Query request must include SQL";
    private static final String UNSUPPORTED_FIELDS_MESSAGE =
            "Only query requests are supported by engine-query; unsupported fields: prepareSql";
    private static final String STATEMENT_EXECUTION_MODE = "STATEMENT";
    private static final String PREPARED_EXECUTION_MODE = "PREPARED_STATEMENT";
    private static final String TRACE_EXECUTION_MODE = "executionMode";
    private static final String TRACE_PARAMETER_PAYLOAD = "parameterPayload";
    private static final String FAILED_PARAMETER_PAYLOAD =
            "[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"not-a-number\"}]";
    private static final String SALES_DROP_SQL = "DROP TABLE IF EXISTS SALES";
    private static final String SALES_CREATE_SQL = "CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))";
    private static final String SALES_INSERT_SQL =
            "INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')";
    private static final String NATION_DROP_SQL = "DROP TABLE IF EXISTS NATION";
    private static final String NATION_CREATE_SQL =
            "CREATE TABLE NATION (NATIONKEY INT PRIMARY KEY, NAME VARCHAR(32))";
    private static final String NATION_INSERT_SQL =
            "INSERT INTO NATION (NATIONKEY, NAME) VALUES (1, 'presto-alpha'), (2, 'presto-beta')";
    private static final String PRESTO_ALPHA = "presto-alpha";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryQueryInfrastructure infrastructure;

    @Value("${engine.query.datasource.default.jdbc-url}")
    private String defaultJdbcUrl;

    @Value("${engine.query.datasource.default.username}")
    private String defaultJdbcUser;

    @Value("${engine.query.datasource.default.password:}")
    private String defaultJdbcPassword;

    @Value("${engine.query.datasource.named.presto_local.jdbc-url}")
    private String prestoJdbcUrl;

    @Value("${engine.query.datasource.named.presto_local.username}")
    private String prestoJdbcUser;

    @Value("${engine.query.datasource.named.presto_local.password:}")
    private String prestoJdbcPassword;

    @Value("${engine.query.auth.username}")
    private String authUsername;

    @Value("${engine.query.auth.password}")
    private String authPassword;

    @BeforeEach
    void setUp() throws Exception {
        infrastructure.clear();
        Class.forName(H2_DRIVER_CLASS);
        try (Connection connection = DriverManager.getConnection(defaultJdbcUrl, defaultJdbcUser, defaultJdbcPassword);
             Statement statement = connection.createStatement()) {
            statement.execute(SALES_DROP_SQL);
            statement.execute(SALES_CREATE_SQL);
            statement.execute(SALES_INSERT_SQL);
        }
        try (Connection connection = DriverManager.getConnection(prestoJdbcUrl, prestoJdbcUser, prestoJdbcPassword);
             Statement statement = connection.createStatement()) {
            statement.execute(NATION_DROP_SQL);
            statement.execute(NATION_CREATE_SQL);
            statement.execute(NATION_INSERT_SQL);
        }
    }

    // Covers QueryController#authenticate and QueryWebIntegrationTest's removed-metadata-endpoint contract guard.
    @Test
    void shouldExposeAuthenticationShimAndRejectRemovedMetadataEndpoints() throws Exception {
        mockMvc.perform(post(AUTH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader()))
                .andExpect(status().isOk())
                .andExpect(content().json(AUTH_RESPONSE));

        mockMvc.perform(get(TABLES_ENDPOINT)
                        .param(PROJECT_PARAM, DEMO_PROJECT)
                        .header(AUTHORIZATION_HEADER, authHeader()))
                .andExpect(status().isNotFound());
    }

    // Covers QueryExecutionService#execute cache-hit behavior through the HTTP path.
    @Test
    void shouldServeCacheHitOnSecondQuery() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CACHE_QUERY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageCacheUsed").value(false))
                .andExpect(jsonPath("$.results[0][0]").value("alpha"));

        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CACHE_QUERY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageCacheUsed").value(true))
                .andExpect(jsonPath("$.results[1][0]").value("beta"));
    }

    // Covers QueryExecutionService#execute non-query rejection through the HTTP path.
    @Test
    void shouldReturnKylinStyleExceptionPayloadForNonQuerySql() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NON_QUERY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isException").value(true))
                .andExpect(jsonPath("$.exceptionMessage").exists());
    }

    // Covers CachePolicy#isQuerySql leading-comment stripping through the HTTP path.
    @Test
    void shouldAcceptQuerySqlWithLeadingOptimizerHintComment() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OPTIMIZER_HINT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isException").value(false))
                .andExpect(jsonPath("$.results[0][0]").value("alpha"))
                .andExpect(jsonPath("$.results[1][0]").value("beta"));
    }

    // Covers QueryExecutionService#execute blank-request rejection through the HTTP path.
    @Test
    void shouldReturnKylinStyleExceptionPayloadForBlankSql() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLANK_SQL_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isException").value(true))
                .andExpect(jsonPath("$.exceptionMessage").value(QUERY_SQL_REQUIRED_MESSAGE));
    }

    // Covers QueryController#query empty-body handling through the HTTP path.
    @Test
    void shouldReturnKylinStyleExceptionPayloadForEmptyRequestBody() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isException").value(true))
                .andExpect(jsonPath("$.exceptionMessage").value(QUERY_SQL_REQUIRED_MESSAGE));
    }

    // Covers QueryExecutionService#execute unsupported-request-shape rejection through the HTTP path.
    @Test
    void shouldReturnKylinStyleExceptionPayloadForUnsupportedRequestShape() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UNSUPPORTED_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isException").value(true))
                .andExpect(jsonPath("$.exceptionMessage").value(UNSUPPORTED_FIELDS_MESSAGE));
    }

    // Covers QueryExecutionService#resolveExecutionMode and TraceReportingService#report statement traces through the HTTP path.
    @Test
    void shouldPublishStatementExecutionModeInTracePayload() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CACHE_QUERY_BODY))
                .andExpect(status().isOk());

        JsonNode trace = lastTrace();
        org.junit.jupiter.api.Assertions.assertEquals(STATEMENT_EXECUTION_MODE, trace.path(TRACE_EXECUTION_MODE).asText());
        org.junit.jupiter.api.Assertions.assertTrue(trace.path(TRACE_PARAMETER_PAYLOAD).isMissingNode()
                || trace.path(TRACE_PARAMETER_PAYLOAD).isNull());
    }

    // Covers QueryExecutionService#bindParameters and TraceReportingService#report prepared traces through the HTTP path.
    @Test
    void shouldPublishPreparedExecutionModeInTracePayload() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PREPARED_SUCCESS_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0][0]").value("alpha"));

        JsonNode trace = lastTrace();
        org.junit.jupiter.api.Assertions.assertEquals(PREPARED_EXECUTION_MODE, trace.path(TRACE_EXECUTION_MODE).asText());
        org.junit.jupiter.api.Assertions.assertTrue(trace.path(TRACE_PARAMETER_PAYLOAD).isMissingNode()
                || trace.path(TRACE_PARAMETER_PAYLOAD).isNull());
    }

    // Covers TraceReportingService#resolveParameterPayload failed-prepared branch through the HTTP path.
    @Test
    void shouldPublishReadableParameterPayloadForFailedPreparedExecution() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PREPARED_FAILURE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isException").value(true));

        JsonNode trace = lastTrace();
        org.junit.jupiter.api.Assertions.assertEquals(PREPARED_EXECUTION_MODE, trace.path(TRACE_EXECUTION_MODE).asText());
        org.junit.jupiter.api.Assertions.assertEquals(FAILED_PARAMETER_PAYLOAD, trace.path(TRACE_PARAMETER_PAYLOAD).asText());
    }

    // Covers SqlRouteService#routeAndRewrite ENGINE routing through the HTTP path.
    @Test
    void shouldRouteQueriesByEngineHint() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRESTO_ROUTE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0][0]").value(PRESTO_ALPHA));
    }

    private String authHeader() {
        String credentials = authUsername + AUTH_SEPARATOR + authPassword;
        return BASIC_PREFIX + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode lastTrace() throws Exception {
        java.util.List<String> traces = infrastructure.publishedTraces();
        org.junit.jupiter.api.Assertions.assertFalse(traces.isEmpty());
        return JSON.readTree(traces.get(traces.size() - 1));
    }
}
