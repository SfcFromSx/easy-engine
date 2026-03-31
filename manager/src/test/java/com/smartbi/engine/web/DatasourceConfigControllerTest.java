package com.smartbi.engine.web;

import com.smartbi.engine.datasource.QueryDatasourceConfig;
import com.smartbi.engine.datasource.QueryDatasourceConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DatasourceConfigControllerTest {

    private final QueryDatasourceConfigService service = Mockito.mock(QueryDatasourceConfigService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DatasourceConfigController(service))
            .setControllerAdvice(new RestExceptionHandler())
            .build();

    @Test
    // Covers DatasourceConfigController#get success path.
    void getReturnsDatasourceWhenPresent() throws Exception {
        QueryDatasourceConfig config = new QueryDatasourceConfig();
        ReflectionTestUtils.setField(config, "id", 4L);
        config.setName("analytics");

        when(service.get(4L)).thenReturn(Optional.of(config));

        mockMvc.perform(get("/api/v1/query-datasources/4").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("analytics"));
    }

    @Test
    // Covers DatasourceConfigController#get 404 path.
    void getReturnsNotFoundWhenMissing() throws Exception {
        when(service.get(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/query-datasources/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
