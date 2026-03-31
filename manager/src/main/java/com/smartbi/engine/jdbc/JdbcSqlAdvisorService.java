package com.smartbi.engine.jdbc;

import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.jdbc.dto.SqlRewriteRequest;
import com.smartbi.engine.jdbc.dto.SqlRewriteResponse;
import com.smartbi.engine.repo.AccelerationTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class JdbcSqlAdvisorService {

    private final AccelerationTableRepository accelerationTableRepository;

    public JdbcSqlAdvisorService(AccelerationTableRepository accelerationTableRepository) {
        this.accelerationTableRepository = accelerationTableRepository;
    }

    public SqlRewriteResponse adviseRewrite(SqlRewriteRequest req) {
        SqlRewriteResponse out = new SqlRewriteResponse();
        String base = StringUtils.hasText(req.getCleanSql()) ? req.getCleanSql() : req.getOriginalSql();
        if (!StringUtils.hasText(base)) {
            out.setExecutionSql("");
            out.setHintCommentBlock(null);
            out.setModified(false);
            out.setAdvisoryMessage("empty_sql");
            return out;
        }

        String query = base.trim();
        List<AccelerationTable> activeTables = accelerationTableRepository.findAll();
        for (AccelerationTable t : activeTables) {
            if (t.getStatus() == AccelerationStatus.ACTIVE && t.getRefreshSql() != null) {
                // If the refresh SQL contains our query, it's a candidate
                if (t.getRefreshSql().contains(query)) {
                    out.setModified(true);
                    // Suggest redirecting to the MySQL-backed acceleration table.
                    out.setHintCommentBlock("/* engine=mysql, cache-table=" + t.getSchemaName() + "." + t.getName() + " */");
                    out.setExecutionSql(out.getHintCommentBlock() + " " + query);
                    out.setAdvisoryMessage("accelerated_by_" + t.getName());
                    return out;
                }
            }
        }

        out.setExecutionSql(query);
        out.setHintCommentBlock(null);
        out.setModified(false);
        out.setAdvisoryMessage("passthrough");
        return out;
    }
}
