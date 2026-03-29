package com.smartbi.query.route;

import com.smartbi.query.datasource.DataSourceDefinition;
import com.smartbi.query.datasource.ManagedDataSourceRegistry;
import com.smartbi.query.parsing.SqlCommentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class SqlRouteService {

    private static final Logger log = LoggerFactory.getLogger(SqlRouteService.class);

    private final ManagedDataSourceRegistry registry;
    private final Map<String, SqlAdapter> adapters = new HashMap<String, SqlAdapter>();

    public SqlRouteService(ManagedDataSourceRegistry registry) {
        this.registry = registry;
        adapters.put("kylin", new PassThroughSqlAdapter());
        adapters.put("presto", new PassThroughSqlAdapter());
        adapters.put("default", new PassThroughSqlAdapter());
    }

    public RoutedSql routeAndRewrite(String originalSql, SqlCommentParser.ParsedSql parsed) {
        String defaultName = registry.getDefaultName();
        String targetName = resolveTargetName(parsed, defaultName);

        DataSourceDefinition definition = registry.getDefinition(targetName);
        if (definition == null) {
            log.warn("Unknown datasource [{}], fallback to [{}]", targetName, defaultName);
            definition = registry.getDefinition(defaultName);
            targetName = defaultName;
        } else if (!definition.getName().equals(targetName) && !defaultName.equals(targetName)) {
            log.warn("Datasource [{}] not configured, fallback to [{}]", targetName, definition.getName());
            targetName = definition.getName();
        }

        String type = definition == null ? "unknown" : definition.getType();
        SqlAdapter adapter = adapters.get(type == null ? "default" : type.toLowerCase(Locale.ROOT));
        if (adapter == null) {
            adapter = new PassThroughSqlAdapter();
        }

        String executionSql = parsed == null ? originalSql : parsed.executionSql;
        return new RoutedSql(targetName, type, originalSql, adapter.rewrite(executionSql));
    }

    private static String resolveTargetName(SqlCommentParser.ParsedSql parsed, String defaultName) {
        if (parsed == null || parsed.metadata == null) {
            return defaultName;
        }
        String metadataTarget = parsed.metadata.extraMetadata.get("YH_TARGET_ENGINE");
        if (metadataTarget != null && !metadataTarget.trim().isEmpty()) {
            return metadataTarget.trim();
        }
        if (parsed.metadata.engine != null && !parsed.metadata.engine.trim().isEmpty()) {
            return parsed.metadata.engine.trim();
        }
        return defaultName;
    }
}
