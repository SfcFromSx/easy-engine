package com.smartbi.engine.jdbc;

import com.smartbi.engine.datasource.QueryDatasourceConfig;
import com.smartbi.engine.datasource.QueryDatasourceConfigService;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.jdbc.dto.SqlRewriteRequest;
import com.smartbi.engine.jdbc.dto.SqlRewriteResponse;
import com.smartbi.engine.repo.AccelerationTableRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class JdbcSqlAdvisorServiceTest {

    private final AccelerationTableRepository repository = Mockito.mock(AccelerationTableRepository.class);
    private final QueryDatasourceConfigService queryDatasourceConfigService = Mockito.mock(QueryDatasourceConfigService.class);
    private final JdbcSqlAdvisorService service = new JdbcSqlAdvisorService(repository, queryDatasourceConfigService);

    @Test
    // Covers JdbcSqlAdvisorService#adviseRewrite passthrough branch.
    void passthroughCleanSql() {
        when(queryDatasourceConfigService.list()).thenReturn(Collections.singletonList(defaultDatasource()));
        SqlRewriteRequest req = new SqlRewriteRequest();
        req.setCleanSql("  SELECT 1  ");
        SqlRewriteResponse out = service.adviseRewrite(req);
        assertTrue(out.isModified());
        assertEquals("/* YH_TARGET_ENGINE=default */\nSELECT 1", out.getExecutionSql());
    }

    @Test
    // Covers JdbcSqlAdvisorService#adviseRewrite empty SQL branch.
    void emptySqlReturnsExplicitAdvisoryMessage() {
        SqlRewriteResponse out = service.adviseRewrite(new SqlRewriteRequest());

        assertFalse(out.isModified());
        assertEquals("", out.getExecutionSql());
        assertNull(out.getHintCommentBlock());
        assertEquals("empty_sql", out.getAdvisoryMessage());
    }

    @Test
    // Covers JdbcSqlAdvisorService#adviseRewrite acceleration branch.
    void activeAccelerationProducesRewriteHint() {
        AccelerationTable table = new AccelerationTable();
        table.setName("mv_sales");
        table.setSchemaName("analytics");
        table.setStatus(AccelerationStatus.ACTIVE);
        table.setRefreshSql("INSERT INTO analytics.mv_sales SELECT 1");
        when(repository.findAll()).thenReturn(Collections.singletonList(table));
        when(queryDatasourceConfigService.list()).thenReturn(Collections.singletonList(defaultDatasource()));

        SqlRewriteRequest request = new SqlRewriteRequest();
        request.setOriginalSql("SELECT 1");

        SqlRewriteResponse out = service.adviseRewrite(request);

        assertTrue(out.isModified());
        assertTrue(out.getHintCommentBlock().contains("cache-table=analytics.mv_sales"));
        assertEquals(out.getHintCommentBlock() + "\nSELECT 1", out.getExecutionSql());
        assertEquals("accelerated_by_mv_sales", out.getAdvisoryMessage());
    }

    private static QueryDatasourceConfig defaultDatasource() {
        QueryDatasourceConfig config = new QueryDatasourceConfig();
        config.setName("default");
        config.setType("h2");
        config.setIsDefault(Boolean.TRUE);
        return config;
    }
}
