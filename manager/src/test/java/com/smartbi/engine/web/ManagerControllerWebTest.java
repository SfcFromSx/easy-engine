package com.smartbi.engine.web;

import com.smartbi.engine.accel.AccelerationService;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.jdbc.JdbcSqlAdvisorService;
import com.smartbi.engine.jdbc.dto.SqlRewriteRequest;
import com.smartbi.engine.parse.ParseOutcome;
import com.smartbi.engine.parse.SqlParseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagerControllerWebTest {

    private final AccelerationService accelerationService = Mockito.mock(AccelerationService.class);
    private final SqlParseService sqlParseService = Mockito.mock(SqlParseService.class);
    private final JdbcSqlAdvisorService jdbcSqlAdvisorService = Mockito.mock(JdbcSqlAdvisorService.class);

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new AccelerationController(accelerationService),
                    new ParseController(sqlParseService),
                    new JdbcAdvisorController(jdbcSqlAdvisorService))
            .setControllerAdvice(new RestExceptionHandler())
            .build();

    @Test
    // Covers ParseController#preview.
    void parsePreviewReturnsAnalyzeOutcome() throws Exception {
        when(sqlParseService.analyze("SELECT 1")).thenReturn(ParseOutcome.skipped("empty_sql"));

        mockMvc.perform(post("/api/v1/parse/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"SELECT 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome.status").value("SKIPPED"))
                .andExpect(jsonPath("$.outcome.errorMessage").value("empty_sql"));
    }

    @Test
    // Covers JdbcAdvisorController#sqlRewrite.
    void jdbcRewriteDelegatesToAdvisorService() throws Exception {
        when(jdbcSqlAdvisorService.adviseRewrite(any(SqlRewriteRequest.class))).thenAnswer(invocation -> {
            SqlRewriteRequest request = invocation.getArgument(0);
            com.smartbi.engine.jdbc.dto.SqlRewriteResponse response = new com.smartbi.engine.jdbc.dto.SqlRewriteResponse();
            response.setExecutionSql(request.getOriginalSql());
            response.setAdvisoryMessage("passthrough");
            return response;
        });

        mockMvc.perform(post("/api/v1/jdbc/sql-rewrite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalSql\":\"SELECT 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionSql").value("SELECT 1"))
                .andExpect(jsonPath("$.advisoryMessage").value("passthrough"));
    }

    @Test
    // Covers AccelerationController#fromPattern validation and RestExceptionHandler#handleBadRequest.
    void fromPatternRejectsMissingPatternIdAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/acceleration-tables/from-pattern")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableName\":\"mv_sales\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("patternStatsId is required"));
    }

    @Test
    // Covers AccelerationController#status and AccelerationService#updateStatus delegation.
    void statusUpdatesAccelerationState() throws Exception {
        AccelerationTable table = new AccelerationTable();
        ReflectionTestUtils.setField(table, "id", 8L);
        table.setStatus(AccelerationStatus.ACTIVE);
        when(accelerationService.updateStatus(8L, AccelerationStatus.ACTIVE)).thenReturn(table);

        mockMvc.perform(patch("/api/v1/acceleration-tables/8/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(accelerationService).updateStatus(8L, AccelerationStatus.ACTIVE);
    }

    @Test
    // Covers AccelerationController#list.
    void listDelegatesToAccelerationService() throws Exception {
        when(accelerationService.list(any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/acceleration-tables"))
                .andExpect(status().isOk());

        verify(accelerationService).list(any(), any(), any(), any(), any());
    }
}
