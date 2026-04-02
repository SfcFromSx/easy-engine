package com.smartbi.engine.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.engine.datasource.QueryDatasourceConfig;
import com.smartbi.engine.datasource.QueryDatasourceConfigService;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.repo.AccelerationTableRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QueryRoutingContextControllerTest {

    private final QueryDatasourceConfigService queryDatasourceConfigService = Mockito.mock(QueryDatasourceConfigService.class);
    private final AccelerationTableRepository accelerationTableRepository = Mockito.mock(AccelerationTableRepository.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new QueryRoutingContextController(queryDatasourceConfigService, accelerationTableRepository))
            .build();

    @Test
    void returnsDatasourcesAndActiveAccelerationRules() throws Exception {
        QueryDatasourceConfig datasource = new QueryDatasourceConfig();
        datasource.setName("default");
        datasource.setType("h2");
        datasource.setIsDefault(Boolean.TRUE);
        when(queryDatasourceConfigService.list()).thenReturn(Collections.singletonList(datasource));

        AccelerationTable active = new AccelerationTable();
        active.setName("mv_sales");
        active.setSchemaName("analytics");
        active.setRefreshSql("INSERT INTO analytics.mv_sales SELECT * FROM sales");
        active.setStatus(AccelerationStatus.ACTIVE);

        AccelerationTable draft = new AccelerationTable();
        draft.setName("mv_sales_draft");
        draft.setSchemaName("analytics");
        draft.setStatus(AccelerationStatus.DRAFT);
        when(accelerationTableRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(Arrays.asList(active, draft));

        mockMvc.perform(get("/api/v1/query-routing-context").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasources[0].name").value("default"))
                .andExpect(jsonPath("$.accelerationRules[0].name").value("mv_sales"))
                .andExpect(jsonPath("$.accelerationRules[0].schemaName").value("analytics"))
                .andExpect(jsonPath("$.accelerationRules[0].tableName").value("mv_sales"))
                .andExpect(jsonPath("$.accelerationRules.length()").value(1));
    }
}
