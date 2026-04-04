package com.smartbi.benchmark.web;

import com.smartbi.benchmark.support.BenchmarkSpringTestOverrides;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BenchmarkSmokeTest {

    private static final String DATASOURCES_ENDPOINT = "/api/v1/datasources";
    private static final String TEMPLATES_ENDPOINT = "/api/v1/templates";
    private static final String TEMPLATE_NAME = "T1";
    private static final String TEMPLATE_SQL = "SELECT 1";
    private static final String TEMPLATE_EXECUTION_MODE = "STATEMENT";

    @Autowired
    private MockMvc mockMvc;

    @Value("${benchmark.test.smoke.test-datasource.name}")
    private String datasourceName;

    @Value("${benchmark.test.smoke.test-datasource.type}")
    private String datasourceType;

    @Value("${benchmark.test.smoke.test-datasource.driver-class}")
    private String datasourceDriverClass;

    @Value("${benchmark.test.smoke.test-datasource.jdbc-url}")
    private String datasourceJdbcUrl;

    @Value("${benchmark.test.smoke.test-datasource.jdbc-user}")
    private String datasourceJdbcUser;

    @Value("${benchmark.test.smoke.test-datasource.jdbc-password:}")
    private String datasourceJdbcPassword;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        BenchmarkSpringTestOverrides.register(registry);
    }

    // Covers DataSourceController#create and #list through the HTTP API.
    @Test
    void shouldManageDatasource() throws Exception {
        mockMvc.perform(post(DATASOURCES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasourceJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(datasourceName));

        mockMvc.perform(get(DATASOURCES_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    // Covers TemplateController#create through the HTTP API.
    @Test
    void shouldManageTemplates() throws Exception {
        mockMvc.perform(post(TEMPLATES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TEMPLATE_NAME));
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

    private String templateJson() {
        return "{"
                + "\"name\":\"" + TEMPLATE_NAME + "\","
                + "\"sqlText\":\"" + TEMPLATE_SQL + "\","
                + "\"executionMode\":\"" + TEMPLATE_EXECUTION_MODE + "\","
                + "\"weight\":1.0"
                + "}";
    }
}
