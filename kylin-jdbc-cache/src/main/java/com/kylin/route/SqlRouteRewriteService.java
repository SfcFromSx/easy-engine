package com.kylin.route;

import com.kylin.SqlCommentParser;
import com.kylin.datasource.DataSourceConfig;
import com.kylin.datasource.DataSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 根据已解析的 SQL 指令完成路由分发与 SQL 方言适配。
 *
 * <p>注释指令的解析统一由 {@link SqlCommentParser} 完成，本类只消费解析结果。
 */
public class SqlRouteRewriteService {

    private static final Logger log = LoggerFactory.getLogger(SqlRouteRewriteService.class);

    private final DataSourceRegistry registry;
    private final boolean routingEnabled;
    private final Map<String, SqlAdapter> adapters = new ConcurrentHashMap<>();
    private final SqlAdapter defaultAdapter = new KylinSqlAdapter();

    public SqlRouteRewriteService(DataSourceRegistry registry, boolean routingEnabled) {
        this.registry = registry;
        this.routingEnabled = routingEnabled;
        adapters.put("kylin", new KylinSqlAdapter());
        adapters.put("presto", new PrestoSqlAdapter());
    }

    /**
     * 根据已解析的 SQL 进行路由，返回 {@link RoutedSql}。
     *
     * @param originalSql 用户原始 SQL（含注释指令）
     * @param parsed      已由 {@link SqlCommentParser} 解析完成的结果
     */
    public RoutedSql routeAndRewrite(String originalSql, SqlCommentParser.ParsedSql parsed) throws SQLException {
        String targetDsName;
        String defaultDsName = registry.getDefaultDataSourceName();
        
        if (routingEnabled && parsed.metadata.engine != null) {
            targetDsName = parsed.metadata.engine;
            // 如果 Hint 指定的数据源不存在，则按照用户要求自动降级回默认数据源
            if (registry.getConfig(targetDsName) == null) {
                log.warn("SQL Hint 指定的引擎 '{}' 未在配置中定义，自动降级至默认引擎: '{}'", targetDsName, defaultDsName);
                targetDsName = defaultDsName;
            }
        } else {
            targetDsName = defaultDsName;
        }

        DataSourceConfig dsConfig = registry.getConfig(targetDsName);
        if (dsConfig == null) {
            // 这属于极端配置错误（如默认数据源也没配），需要抛出异常
            throw new SQLException("无法加载目标数据源: " + targetDsName + " (默认数据源配置可能失效)");
        }

        String type = dsConfig.getType();
        SqlAdapter adapter = adapters.getOrDefault(type.toLowerCase(), defaultAdapter);
        String executionSql = adapter.rewrite(parsed.executionSql);

        return new RoutedSql(targetDsName, type, originalSql, executionSql);
    }
}
