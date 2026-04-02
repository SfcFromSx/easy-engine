package com.smartbi.benchmark.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(locations = "classpath:benchmark-smoke-test.properties")
@AutoConfigureMockMvc
class BenchmarkSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${benchmark.smoke.test-datasource.name}")
    private String datasourceName;

    @Value("${benchmark.smoke.test-datasource.type}")
    private String datasourceType;

    @Value("${benchmark.smoke.test-datasource.driver-class}")
    private String datasourceDriverClass;

    @Value("${benchmark.smoke.test-datasource.jdbc-url}")
    private String datasourceJdbcUrl;

    @Value("${benchmark.smoke.test-datasource.jdbc-user}")
    private String datasourceJdbcUser;

    @Value("${benchmark.smoke.test-datasource.jdbc-password:}")
    private String datasourceJdbcPassword;

    // Covers DataSourceController#create and #list through the HTTP API.
    @Test
    void shouldManageDatasource() throws Exception {
        mockMvc.perform(post("/api/v1/datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasourceJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(datasourceName));

        mockMvc.perform(get("/api/v1/datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    // Covers TemplateController#create through the HTTP API.
    @Test
    void shouldManageTemplates() throws Exception {
        String templateJson = "{\"name\":\"T1\",\"sqlText\":\"SELECT 1\",\"executionMode\":\"STATEMENT\",\"weight\":1.0}";
        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("T1"));
    }

    private String datasourceJson() {
        return "{"
                + "\"name\":\"" + datasourceName + "\","
                + "\"type\":\"" + datasourceType + "\","
                + "\"driverClass\":\"" + datasourceDriverClass + "\","
                + "\"jdbcUrl\":\"" + datasourceJdbcUrl + "\","
                + "\"jdbcUser\":\"" + datasourceJdbcUser + "\","
                + "\"jdbcPassword\":\"" + datasourceJdbcPassword + "\""
                + "}";
    }
}
