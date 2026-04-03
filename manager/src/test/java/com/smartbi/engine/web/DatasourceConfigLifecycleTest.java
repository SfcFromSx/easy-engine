package com.smartbi.engine.web;

import com.smartbi.engine.datasource.QueryDatasourceConfigRepository;
import com.smartbi.engine.support.ManagerTestFixtures;
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

    private static final String DEFAULT_NAME = ManagerTestFixtures.get("manager.test.datasource-lifecycle.default.name");
    private static final String DEFAULT_TYPE = ManagerTestFixtures.get("manager.test.datasource-lifecycle.default.type");
    private static final String DEFAULT_DRIVER = ManagerTestFixtures.get("manager.test.datasource-lifecycle.default.driver-class");
    private static final String DEFAULT_JDBC_URL = ManagerTestFixtures.get("manager.test.datasource-lifecycle.default.jdbc-url");
    private static final String DEFAULT_USERNAME = ManagerTestFixtures.get("manager.test.datasource-lifecycle.default.username");
    private static final String DEFAULT_PASSWORD = ManagerTestFixtures.get("manager.test.datasource-lifecycle.default.password");
    private static final String PRESTO_NAME = ManagerTestFixtures.get("manager.test.datasource-lifecycle.presto.name");
    private static final String PRESTO_TYPE = ManagerTestFixtures.get("manager.test.datasource-lifecycle.presto.type");
    private static final String PRESTO_DRIVER = ManagerTestFixtures.get("manager.test.datasource-lifecycle.presto.driver-class");
    private static final String PRESTO_JDBC_URL = ManagerTestFixtures.get("manager.test.datasource-lifecycle.presto.jdbc-url");
    private static final String PRESTO_USERNAME = ManagerTestFixtures.get("manager.test.datasource-lifecycle.presto.username");
    private static final String PRESTO_PASSWORD = ManagerTestFixtures.get("manager.test.datasource-lifecycle.presto.password");
    private static final String PRESTO_PROMOTED_USERNAME = ManagerTestFixtures.get("manager.test.datasource-lifecycle.presto.promoted-username");

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
        String defaultJson = datasourceJson(
                DEFAULT_NAME,
                DEFAULT_TYPE,
                DEFAULT_DRIVER,
                DEFAULT_JDBC_URL,
                DEFAULT_USERNAME,
                DEFAULT_PASSWORD,
                4,
                1,
                10000,
                true);

        mockMvc.perform(post("/api/v1/query-datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(defaultJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("default"))
                .andExpect(jsonPath("$.isDefault").value(true));

        String prestoJson = datasourceJson(
                PRESTO_NAME,
                PRESTO_TYPE,
                PRESTO_DRIVER,
                PRESTO_JDBC_URL,
                PRESTO_USERNAME,
                PRESTO_PASSWORD,
                4,
                1,
                10000,
                false);

        mockMvc.perform(post("/api/v1/query-datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prestoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("presto_local"));

        mockMvc.perform(get("/api/v1/query-datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("default"))
                .andExpect(jsonPath("$[1].name").value("presto_local"));

        long prestoId = repository.findByName(PRESTO_NAME).orElseThrow(AssertionError::new).getId();
        mockMvc.perform(put("/api/v1/query-datasources/{id}", prestoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasourceJson(
                                PRESTO_NAME,
                                PRESTO_TYPE,
                                PRESTO_DRIVER,
                                PRESTO_JDBC_URL,
                                PRESTO_PROMOTED_USERNAME,
                                PRESTO_PASSWORD,
                                8,
                                2,
                                15000,
                                true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(PRESTO_PROMOTED_USERNAME))
                .andExpect(jsonPath("$.maxPoolSize").value(8))
                .andExpect(jsonPath("$.isDefault").value(true));

        assertEquals(Boolean.FALSE, repository.findByName(DEFAULT_NAME).orElseThrow(AssertionError::new).getIsDefault());
        assertEquals(Boolean.TRUE, repository.findByName(PRESTO_NAME).orElseThrow(AssertionError::new).getIsDefault());

        mockMvc.perform(delete("/api/v1/query-datasources/{id}", prestoId))
                .andExpect(status().isNoContent());

        assertEquals(1L, repository.count());
        assertTrue(repository.findByName(DEFAULT_NAME).orElseThrow(AssertionError::new).getIsDefault());
    }

    private String datasourceJson(String name,
                                  String type,
                                  String driverClass,
                                  String jdbcUrl,
                                  String username,
                                  String password,
                                  int maxPoolSize,
                                  int minIdle,
                                  int connectionTimeoutMs,
                                  boolean isDefault) {
        return "{"
                + "\"name\":\"" + name + "\","
                + "\"type\":\"" + type + "\","
                + "\"driverClass\":\"" + driverClass + "\","
                + "\"jdbcUrl\":\"" + jdbcUrl + "\","
                + "\"username\":\"" + username + "\","
                + "\"password\":\"" + password + "\","
                + "\"maxPoolSize\":" + maxPoolSize + ","
                + "\"minIdle\":" + minIdle + ","
                + "\"connectionTimeoutMs\":" + connectionTimeoutMs + ","
                + "\"isDefault\":" + isDefault
                + "}";
    }
}
