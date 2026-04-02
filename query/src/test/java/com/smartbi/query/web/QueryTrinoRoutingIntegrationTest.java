package com.smartbi.query.web;

import com.smartbi.query.EngineQueryApplication;
import com.smartbi.query.support.QueryTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EngineQueryApplication.class)
@AutoConfigureMockMvc
@Import(QueryTestConfiguration.class)
class QueryTrinoRoutingIntegrationTest {

    private static final String H2_DRIVER_CLASS = "org.h2.Driver";
    private static final String QUERY_ENDPOINT = "/kylin/api/query";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String AUTH_SEPARATOR = ":";
    private static final String ROUTED_QUERY_BODY =
            "{\"sql\":\"/* YH_TARGET_ENGINE=trino_local */ SELECT NAME FROM REGION WHERE REGIONKEY = 1\",\"project\":\"demo\"}";
    private static final String REGION_DROP_SQL = "DROP TABLE IF EXISTS REGION";
    private static final String REGION_CREATE_SQL =
            "CREATE TABLE REGION (REGIONKEY INT PRIMARY KEY, NAME VARCHAR(32))";
    private static final String REGION_INSERT_SQL =
            "INSERT INTO REGION (REGIONKEY, NAME) VALUES (1, 'trino-africa'), (2, 'trino-america')";
    private static final String TRINO_AFRICA = "trino-africa";

    @Resource
    private MockMvc mockMvc;

    @Value("${engine.query.datasource.named.trino_local.jdbc-url}")
    private String trinoJdbcUrl;

    @Value("${engine.query.datasource.named.trino_local.username}")
    private String trinoJdbcUser;

    @Value("${engine.query.datasource.named.trino_local.password:}")
    private String trinoJdbcPassword;

    @Value("${engine.query.auth.username}")
    private String authUsername;

    @Value("${engine.query.auth.password}")
    private String authPassword;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName(H2_DRIVER_CLASS);
        try (Connection connection = DriverManager.getConnection(trinoJdbcUrl, trinoJdbcUser, trinoJdbcPassword);
             Statement statement = connection.createStatement()) {
            statement.execute(REGION_DROP_SQL);
            statement.execute(REGION_CREATE_SQL);
            statement.execute(REGION_INSERT_SQL);
        }
    }

    // Covers routed Trino execution through the HTTP compatibility path.
    @Test
    void shouldRouteQueriesToTrinoDatasources() throws Exception {
        mockMvc.perform(post(QUERY_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTED_QUERY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0][0]").value(TRINO_AFRICA));
    }

    private String authHeader() {
        String credentials = authUsername + AUTH_SEPARATOR + authPassword;
        return BASIC_PREFIX + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
