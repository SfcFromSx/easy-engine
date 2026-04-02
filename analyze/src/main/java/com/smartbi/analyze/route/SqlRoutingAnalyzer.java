package com.smartbi.analyze.route;

import com.smartbi.analyze.sql.ParsedSql;
import com.smartbi.analyze.sql.SqlCommentParser;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SqlRoutingAnalyzer {

    private final Map<String, SqlDialectAdapter> adapters = new HashMap<String, SqlDialectAdapter>();

    public SqlRoutingAnalyzer() {
        register("kylin", new PassThroughSqlDialectAdapter());
        register("presto", new PassThroughSqlDialectAdapter());
        register("trino", new PassThroughSqlDialectAdapter());
        register("default", new PassThroughSqlDialectAdapter());
    }

    public void register(String datasourceType, SqlDialectAdapter adapter) {
        if (datasourceType == null || adapter == null) {
            return;
        }
        adapters.put(datasourceType.toLowerCase(Locale.ROOT), adapter);
    }

    public RoutingDecision analyze(String originalSql, ParsedSql parsed, RoutingContext context) {
        String defaultName = context == null ? null : context.getDefaultDatasourceName();
        String requestedTarget = parsed == null || parsed.metadata == null
                ? null
                : parsed.metadata.extraMetadata.get("YH_TARGET_ENGINE");
        String targetName = hasText(requestedTarget) ? requestedTarget.trim() : defaultName;
        DatasourceDescriptor descriptor = context == null ? null : context.findDatasource(targetName);
        if (descriptor == null && context != null) {
            descriptor = context.findDatasource(defaultName);
        }
        if (descriptor == null && context != null && !context.getDatasources().isEmpty()) {
            descriptor = context.getDatasources().get(0);
        }

        String resolvedDatasourceName = descriptor == null ? targetName : descriptor.getName();
        if (!hasText(resolvedDatasourceName)) {
            resolvedDatasourceName = "default";
        }
        String resolvedDatasourceType = descriptor == null ? null : descriptor.getType();

        String sqlForMatch = parsed == null || !hasText(parsed.cleanSql) ? originalSql : parsed.cleanSql;
        AccelerationRule matchedRule = findMatchingRule(sqlForMatch, context);
        String cacheTable = matchedRule == null ? null : matchedRule.getQualifiedTableName();

        String baseExecutionSql = parsed == null || !hasText(parsed.executionSql) ? originalSql : parsed.executionSql;
        String normalizedExecutionSql = SqlCommentParser.removeMetadataKey(baseExecutionSql, "YH_TARGET_ENGINE");
        String rewrittenSql = buildLeadingComment(resolvedDatasourceName, cacheTable) + "\n"
                + (normalizedExecutionSql == null ? "" : normalizedExecutionSql.trim());

        SqlDialectAdapter adapter = resolveAdapter(resolvedDatasourceType);
        return new RoutingDecision(
                resolvedDatasourceName,
                resolvedDatasourceType,
                adapter.rewrite(rewrittenSql.trim()),
                cacheTable,
                matchedRule == null ? null : matchedRule.getName()
        );
    }

    private AccelerationRule findMatchingRule(String sql, RoutingContext context) {
        if (!hasText(sql) || context == null) {
            return null;
        }
        String query = sql.trim();
        for (AccelerationRule rule : context.getAccelerationRules()) {
            if (rule != null && hasText(rule.getRefreshSql()) && rule.getRefreshSql().contains(query)) {
                return rule;
            }
        }
        return null;
    }

    private SqlDialectAdapter resolveAdapter(String datasourceType) {
        if (!hasText(datasourceType)) {
            return adapters.get("default");
        }
        SqlDialectAdapter adapter = adapters.get(datasourceType.toLowerCase(Locale.ROOT));
        return adapter == null ? adapters.get("default") : adapter;
    }

    private static String buildLeadingComment(String datasourceName, String cacheTable) {
        StringBuilder builder = new StringBuilder("/* YH_TARGET_ENGINE=").append(datasourceName);
        if (hasText(cacheTable)) {
            builder.append(" cache-table=").append(cacheTable);
        }
        builder.append(" */");
        return builder.toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
