package com.smartbi.query.web;

import com.smartbi.query.EngineQueryApplication;
import com.smartbi.query.support.QueryTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = EngineQueryApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:webtest_trino_trace;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "engine.query.datasource.default.name=default",
                "engine.query.datasource.default.type=h2",
                "engine.query.datasource.default.driver-class=org.h2.Driver",
                "engine.query.datasource.default.jdbc-url=jdbc:h2:mem:webtest_trino_default;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "engine.query.datasource.default.username=sa",
                "engine.query.datasource.default.password=",
                "engine.query.datasource.named.trino_local.name=trino_local",
                "engine.query.datasource.named.trino_local.type=trino",
                "engine.query.datasource.named.trino_local.driver-class=org.h2.Driver",
                "engine.query.datasource.named.trino_local.jdbc-url=jdbc:h2:mem:webtest_trino_named;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "engine.query.datasource.named.trino_local.username=sa",
                "engine.query.datasource.named.trino_local.password=",
                "engine.query.manager-url=",
                "engine.query.auth.username=ADMIN",
                "engine.query.auth.password=KYLIN"
        }
)
@AutoConfigureMockMvc
@Import(QueryTestConfiguration.class)
class QueryTrinoRoutingIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:webtest_trino_named;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS REGION");
            statement.execute("CREATE TABLE REGION (REGIONKEY INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO REGION (REGIONKEY, NAME) VALUES (1, 'trino-africa'), (2, 'trino-america')");
        }
    }

    // Covers routed Trino execution through the HTTP compatibility path.
    @Test
    void shouldRouteQueriesToTrinoDatasources() throws Exception {
        String body = "{\"sql\":\"/* YH_TARGET_ENGINE=trino_local */ SELECT NAME FROM REGION WHERE REGIONKEY = 1\",\"project\":\"demo\"}";

        mockMvc.perform(post("/kylin/api/query")
                        .header("Authorization", authHeader())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0][0]").value("trino-africa"));
    }

    private static String authHeader() {
        return "Basic " + Base64.getEncoder().encodeToString("ADMIN:KYLIN".getBytes());
    }
}
