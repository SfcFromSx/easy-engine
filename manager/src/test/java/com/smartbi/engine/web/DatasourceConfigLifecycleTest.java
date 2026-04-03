package com.smartbi.engine.web;

import com.smartbi.engine.datasource.QueryDatasourceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DatasourceConfigLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueryDatasourceConfigRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    // Covers DatasourceConfigController#create, DatasourceConfigController#update, and DatasourceConfigController#delete.
    void shouldManageDatasourceCrudLifecycle() throws Exception {
        String defaultJson = "{" +
                "\"name\":\"default\"," +
                "\"type\":\"kylin\"," +
                "\"driverClass\":\"org.apache.kylin.jdbc.Driver\"," +
                "\"jdbcUrl\":\"jdbc:kylin://localhost:17070/learn_kylin\"," +
                "\"username\":\"ADMIN\"," +
                "\"password\":\"KYLIN\"," +
                "\"maxPoolSize\":4," +
                "\"minIdle\":1," +
                "\"connectionTimeoutMs\":10000," +
                "\"isDefault\":true" +
                "}";

        mockMvc.perform(post("/api/v1/query-datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(defaultJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("default"))
                .andExpect(jsonPath("$.isDefault").value(true));

        String prestoJson = "{" +
                "\"name\":\"presto_local\"," +
                "\"type\":\"presto\"," +
                "\"driverClass\":\"com.facebook.presto.jdbc.PrestoDriver\"," +
                "\"jdbcUrl\":\"jdbc:presto://localhost:18081/tpch/tiny\"," +
                "\"username\":\"admin\"," +
                "\"password\":\"\"," +
                "\"maxPoolSize\":4," +
                "\"minIdle\":1," +
                "\"connectionTimeoutMs\":10000," +
                "\"isDefault\":false" +
                "}";

        mockMvc.perform(post("/api/v1/query-datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prestoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("presto_local"));

        mockMvc.perform(get("/api/v1/query-datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("default"))
                .andExpect(jsonPath("$[1].name").value("presto_local"));

        long prestoId = repository.findByName("presto_local").orElseThrow(AssertionError::new).getId();
        mockMvc.perform(put("/api/v1/query-datasources/{id}", prestoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"presto_local\"," +
                                "\"type\":\"presto\"," +
                                "\"driverClass\":\"com.facebook.presto.jdbc.PrestoDriver\"," +
                                "\"jdbcUrl\":\"jdbc:presto://localhost:18081/tpch/tiny\"," +
                                "\"username\":\"svc_presto\"," +
                                "\"password\":\"\"," +
                                "\"maxPoolSize\":8," +
                                "\"minIdle\":2," +
                                "\"connectionTimeoutMs\":15000," +
                                "\"isDefault\":true" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("svc_presto"))
                .andExpect(jsonPath("$.maxPoolSize").value(8))
                .andExpect(jsonPath("$.isDefault").value(true));

        assertEquals(Boolean.FALSE, repository.findByName("default").orElseThrow(AssertionError::new).getIsDefault());
        assertEquals(Boolean.TRUE, repository.findByName("presto_local").orElseThrow(AssertionError::new).getIsDefault());

        mockMvc.perform(delete("/api/v1/query-datasources/{id}", prestoId))
                .andExpect(status().isNoContent());

        assertEquals(1L, repository.count());
        assertTrue(repository.findByName("default").orElseThrow(AssertionError::new).getIsDefault());
    }
}
