package com.smartbi.benchmark.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:benchmarktest;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class BenchmarkSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldManageDatasource() throws Exception {
        String dsJson = "{\"name\":\"test_ds\",\"type\":\"h2\",\"driverClass\":\"org.h2.Driver\",\"jdbcUrl\":\"jdbc:h2:mem:test\",\"jdbcUser\":\"sa\",\"jdbcPassword\":\"\"}";
        mockMvc.perform(post("/api/v1/datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dsJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test_ds"));

        mockMvc.perform(get("/api/v1/datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void shouldManageTemplates() throws Exception {
        String templateJson = "{\"name\":\"T1\",\"sqlText\":\"SELECT 1\",\"executionMode\":\"STATEMENT\",\"weight\":1.0}";
        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("T1"));
    }
}
