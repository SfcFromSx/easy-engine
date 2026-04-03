package com.smartbi.engine.jdbc;

import com.smartbi.analyze.route.AccelerationRule;
import com.smartbi.analyze.route.DatasourceDescriptor;
import com.smartbi.analyze.route.RoutingContext;
import com.smartbi.analyze.route.RoutingDecision;
import com.smartbi.analyze.route.SqlRoutingAnalyzer;
import com.smartbi.analyze.sql.ParsedSql;
import com.smartbi.analyze.sql.SqlCommentParser;
import com.smartbi.engine.datasource.QueryDatasourceConfig;
import com.smartbi.engine.datasource.QueryDatasourceConfigService;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.jdbc.dto.SqlRewriteRequest;
import com.smartbi.engine.jdbc.dto.SqlRewriteResponse;
import com.smartbi.engine.repo.AccelerationTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class JdbcSqlAdvisorService {

    private final AccelerationTableRepository accelerationTableRepository;
    private final QueryDatasourceConfigService queryDatasourceConfigService;
    private final EffectiveEngineResolver effectiveEngineResolver;
    private final SqlRoutingAnalyzer sqlRoutingAnalyzer = new SqlRoutingAnalyzer();

    @Autowired
    public JdbcSqlAdvisorService(AccelerationTableRepository accelerationTableRepository,
                                 QueryDatasourceConfigService queryDatasourceConfigService,
                                 EffectiveEngineResolver effectiveEngineResolver) {
        this.accelerationTableRepository = accelerationTableRepository;
        this.queryDatasourceConfigService = queryDatasourceConfigService;
        this.effectiveEngineResolver = effectiveEngineResolver;
    }

    JdbcSqlAdvisorService(AccelerationTableRepository accelerationTableRepository,
                          QueryDatasourceConfigService queryDatasourceConfigService) {
        this(accelerationTableRepository, queryDatasourceConfigService, new EffectiveEngineResolver(null));
    }

    public SqlRewriteResponse adviseRewrite(SqlRewriteRequest req) {
        SqlRewriteResponse out = new SqlRewriteResponse();
        String base = StringUtils.hasText(req.getOriginalSql()) ? req.getOriginalSql() : req.getCleanSql();
        if (!StringUtils.hasText(base)) {
            out.setExecutionSql("");
            out.setHintCommentBlock(null);
            out.setModified(false);
            out.setAdvisoryMessage("empty_sql");
            return out;
        }

        ParsedSql parsed = SqlCommentParser.safeParse(base);
        String effectiveEngine = effectiveEngineResolver.resolve(parsed);
        RoutingDecision decision = sqlRoutingAnalyzer.analyze(base, parsed, buildRoutingContext(), effectiveEngine);
        out.setExecutionSql(decision.getExecutionSql());
        out.setHintCommentBlock(extractLeadingCommentBlock(decision.getExecutionSql()));
        out.setModified(!base.trim().equals(decision.getExecutionSql()));
        if (StringUtils.hasText(decision.getAccelerationRuleName())) {
            out.setAdvisoryMessage("accelerated_by_" + decision.getAccelerationRuleName());
        } else if (out.isModified()) {
            out.setAdvisoryMessage("routed_to_" + decision.getDatasourceName());
        } else {
            out.setAdvisoryMessage("passthrough");
        }
        return out;
    }

    private RoutingContext buildRoutingContext() {
        List<QueryDatasourceConfig> configs = queryDatasourceConfigService.list();
        List<DatasourceDescriptor> datasources = new ArrayList<DatasourceDescriptor>(configs.size());
        String defaultName = null;
        for (QueryDatasourceConfig config : configs) {
            if (config == null) {
                continue;
            }
            boolean isDefault = Boolean.TRUE.equals(config.getIsDefault());
            datasources.add(new DatasourceDescriptor(config.getName(), config.getType(), isDefault));
            if (defaultName == null && isDefault) {
                defaultName = config.getName();
            }
        }
        if (!StringUtils.hasText(defaultName) && !datasources.isEmpty()) {
            defaultName = datasources.get(0).getName();
        }

        List<AccelerationRule> accelerationRules = new ArrayList<AccelerationRule>();
        for (AccelerationTable table : accelerationTableRepository.findAll()) {
            if (table != null && table.getStatus() == AccelerationStatus.ACTIVE) {
                accelerationRules.add(new AccelerationRule(
                        table.getName(),
                        table.getSchemaName(),
                        table.getName(),
                        table.getRefreshSql()
                ));
            }
        }
        return new RoutingContext(defaultName, datasources, accelerationRules);
    }

    private static String extractLeadingCommentBlock(String executionSql) {
        if (!StringUtils.hasText(executionSql)) {
            return null;
        }
        String trimmed = executionSql.trim();
        if (!trimmed.startsWith("/*")) {
            return null;
        }
        int end = trimmed.indexOf("*/");
        return end < 0 ? null : trimmed.substring(0, end + 2);
    }
}
