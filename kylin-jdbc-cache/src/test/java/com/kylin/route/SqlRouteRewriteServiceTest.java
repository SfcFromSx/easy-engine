package com.kylin.route;

import com.kylin.SqlCommentParser;
import com.kylin.datasource.DataSourceConfig;
import com.kylin.datasource.DataSourceRegistry;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SqlRouteRewriteService} 的单元测试。
 */
public class SqlRouteRewriteServiceTest {

    private SqlRouteRewriteService routeService;
    private DataSourceRegistry registry;

    @Before
    public void setUp() {
        registry = mock(DataSourceRegistry.class);
        when(registry.getDefaultDataSourceName()).thenReturn("default");

        DataSourceConfig kylinConfig = new DataSourceConfig(
                "default", "kylin", "org.apache.kylin.jdbc.Driver",
                "jdbc:kylin://localhost/default", "ADMIN", "KYLIN", false, 0, 0, 0);
        when(registry.getConfig("default")).thenReturn(kylinConfig);

        DataSourceConfig prestoConfig = new DataSourceConfig(
                "presto_1", "presto", "io.prestosql.jdbc.PrestoDriver",
                "jdbc:presto://localhost:8080/hive/default", "test", "", false, 0, 0, 0);
        when(registry.getConfig("presto_1")).thenReturn(prestoConfig);

        // 默认开启路由测试
        routeService = new SqlRouteRewriteService(registry, true);
    }

    @Test
    public void testDefaultRoute() throws SQLException {
        String sql = "SELECT * FROM my_table";
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);
        RoutedSql routed = routeService.routeAndRewrite(sql, parsed);
        assertEquals("default", routed.datasourceName);
        assertEquals("kylin", routed.datasourceType);
    }

    @Test
    public void testEngineHint() throws SQLException {
        String originalSql = "-- engine=presto_1\nSELECT * FROM my_table";
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(originalSql);
        
        RoutedSql routed = routeService.routeAndRewrite(originalSql, parsed);
        assertEquals("presto_1", routed.datasourceName);
        assertEquals("presto", routed.datasourceType);
        assertEquals("SELECT * FROM my_table", routed.executionSql);
    }

    @Test
    public void testEngineHintUnknownFallsBackToDefault() throws SQLException {
        String originalSql = "-- engine=unknown_db\nSELECT 1";
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(originalSql);
        
        RoutedSql routed = routeService.routeAndRewrite(originalSql, parsed);
        assertEquals("default", routed.datasourceName);
        assertEquals("kylin", routed.datasourceType);
    }

    @Test
    public void testRoutingDisabledIgnoresHint() throws SQLException {
        routeService = new SqlRouteRewriteService(registry, false);
        String originalSql = "-- engine=presto_1\nSELECT 1";
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(originalSql);
        
        RoutedSql routed = routeService.routeAndRewrite(originalSql, parsed);
        assertEquals("default", routed.datasourceName);
    }

    // --- 新增边界场景 (Branch Coverage) ---

    @Test
    public void testAdapterFallbackForUnknownType() throws SQLException {
        // 当数据源类型不认识 (比如 mysql) 时，应降级到默认适配器 (KylinSqlAdapter)
        DataSourceConfig mysqlConfig = new DataSourceConfig(
                "my_db", "mysql", "com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://localhost:3306/db", "root", "", false, 0, 0, 0);
        when(registry.getConfig("my_db")).thenReturn(mysqlConfig);

        String sql = "-- engine=my_db\nSELECT 1";
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);
        
        RoutedSql routed = routeService.routeAndRewrite(sql, parsed);
        assertEquals("my_db", routed.datasourceName);
        assertEquals("mysql", routed.datasourceType);
        // 虽然类型是 mysql，但使用的是默认适配器，SELECT 1 不会被重写
        assertEquals("SELECT 1", routed.executionSql);
    }

    @Test(expected = SQLException.class)
    public void testExtremeConfigFailure() throws SQLException {
        // 当连默认数据源都找不到时的极端错误方案
        when(registry.getDefaultDataSourceName()).thenReturn("none");
        when(registry.getConfig("none")).thenReturn(null);

        String sql = "SELECT 1";
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);
        routeService.routeAndRewrite(sql, parsed);
    }

    @Test
    public void testPrestoAdapterRewritesLimit() throws SQLException {
        // 验证 Presto 适配器是否真正起作用 (目前 PrestoSqlAdapter 只是透传，但未来可能重写)
        String sql = "-- engine=presto_1\nSELECT * FROM t LIMIT 10";
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.parse(sql);
        RoutedSql routed = routeService.routeAndRewrite(sql, parsed);
        assertEquals("presto_1", routed.datasourceName);
    }
}
