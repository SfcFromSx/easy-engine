package com.smartbi.engine.jdbc;

import com.smartbi.engine.jdbc.dto.SqlRewriteRequest;
import com.smartbi.engine.jdbc.dto.SqlRewriteResponse;
import com.smartbi.engine.repo.AccelerationTableRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class JdbcSqlAdvisorServiceTest {

    private final AccelerationTableRepository repository = Mockito.mock(AccelerationTableRepository.class);
    private final JdbcSqlAdvisorService service = new JdbcSqlAdvisorService(repository);

    @Test
    void passthroughCleanSql() {
        SqlRewriteRequest req = new SqlRewriteRequest();
        req.setCleanSql("  SELECT 1  ");
        SqlRewriteResponse out = service.adviseRewrite(req);
        assertFalse(out.isModified());
        assertEquals("SELECT 1", out.getExecutionSql());
    }
}
