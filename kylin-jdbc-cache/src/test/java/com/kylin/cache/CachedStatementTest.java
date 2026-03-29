package com.kylin.cache;

import com.kylin.CachedKylinStatement;
import com.kylin.DriverConfig;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * {@link CachedKylinStatement} 的集成式测试。
 * 使用 Mockito 模拟底层驱动和 Redis，以验证缓存逻辑。
 */
public class CachedStatementTest {

    private Statement mockDelegate;
    private RedisCacheManager mockCache;
    private CachedKylinStatement statement;
    private DriverConfig config;

    private com.kylin.CachedKylinConnection mockKylinConnection(Connection connection, Statement delegate) throws Exception {
        com.kylin.datasource.DataSourceRegistry registry = mock(com.kylin.datasource.DataSourceRegistry.class);
        when(registry.getConnection(anyString())).thenReturn(connection);
        when(registry.getDefaultDataSourceName()).thenReturn("test");
        com.kylin.route.SqlRouteRewriteService routeService = mock(com.kylin.route.SqlRouteRewriteService.class);
        when(routeService.routeAndRewrite(anyString(), any())).thenAnswer(inv -> {
            String original = inv.getArgument(0);
            return new com.kylin.route.RoutedSql("test", "kylin", original, com.kylin.SqlCommentParser.parse(original).cleanSql);
        });
        com.kylin.CachedKylinConnection kylinConn = mock(com.kylin.CachedKylinConnection.class);
        when(kylinConn.getRegistry()).thenReturn(registry);
        when(kylinConn.getRouteService()).thenReturn(routeService);
        when(connection.createStatement()).thenReturn(delegate);
        when(connection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(delegate);
        when(connection.createStatement(anyInt(), anyInt())).thenReturn(delegate);
        return kylinConn;
    }

    @Before
    public void setUp() throws Exception {
        mockDelegate = mock(Statement.class);
        mockCache = mock(RedisCacheManager.class);
        Connection mockConn = mock(Connection.class);

        // 默认配置
        config = DriverConfig.parse("jdbc:kylin-cached://localhost/proj", null);
        CacheLogic logic = new CacheLogic(mockCache, config, new CachePolicy(config));

        statement = new CachedKylinStatement(mockKylinConnection(mockConn, mockDelegate), logic, new CachePolicy(config), mock(com.kylin.record.SqlExecutionTraceService.class));
    }

    @Test
    public void testCacheMiss_ExecutesAndStores() throws Exception {
        String sql = "SELECT * FROM fact_sales";
        // 1. 模拟 Redis 返回 null (未命中)
        when(mockCache.get(anyString())).thenReturn(null);

        // 2. 模拟 Kylin 返回一个有数据的 ResultSet
        ResultSet delegateRs = onRowResultSet("id", "123");
        when(mockDelegate.executeQuery(sql)).thenReturn(delegateRs);

        // 3. 执行
        ResultSet result = statement.executeQuery(sql);

        // 4. 验证
        // 应调用一次 Kylin
        verify(mockDelegate).executeQuery(sql);
        // 应调用一次 Redis set
        verify(mockCache).set(anyString(), any(byte[].class), eq(300));

        assertTrue(result.next());
        assertEquals("123", result.getString(1));
    }

    @Test
    public void testCacheHit_ReturnsCachedImmediately() throws Exception {
        String sql = "SELECT 1";
        // 模拟先前序列化的结果（使用真实的序列化器以保持一致性）
        ResultSet initialRs = onRowResultSet("val", "hit");
        byte[] cachedData = ResultSetSerializer.serialize(initialRs);

        // 模拟 Redis 命中
        when(mockCache.get(anyString())).thenReturn(cachedData);

        // 执行
        ResultSet result = statement.executeQuery(sql);

        // 验证
        // 不应调用 Kylin
        verify(mockDelegate, never()).executeQuery(anyString());
        assertTrue(result.next());
        assertEquals("hit", result.getString(1));
    }

    @Test
    public void testNoCacheHint_BypassesRedis() throws Exception {
        String sql = "-- no-cache\nSELECT * FROM large_table";
        String cleanSql = "SELECT * FROM large_table";

        ResultSet delegateRs = onRowResultSet("status", "ok");
        when(mockDelegate.executeQuery(cleanSql)).thenReturn(delegateRs);

        ResultSet result = statement.executeQuery(sql);

        // 验证
        verify(mockCache, never()).get(anyString());
        verify(mockCache, never()).set(anyString(), any(), anyInt());
        verify(mockDelegate).executeQuery(cleanSql);
        assertTrue(result.next());
        assertEquals("ok", result.getString(1));
    }

    @Test
    public void testCacheTtlOverride() throws Exception {
        String sql = "-- cache-ttl:60\nSELECT 1";
        when(mockCache.get(anyString())).thenReturn(null);
        ResultSet delegateRs = onRowResultSet("a", "b");
        when(mockDelegate.executeQuery(anyString())).thenReturn(delegateRs);

        statement.executeQuery(sql);

        // 验证 set 调用使用的是 60 秒而不是默认的 300 秒
        verify(mockCache).set(anyString(), any(), eq(60));
    }

    @Test
    public void testCacheRefresh_skipReadButWritesFreshResult() throws Exception {
        String sql = "-- cache-refresh\nSELECT count(*) FROM fact_revenue";
        String cleanSql = "SELECT count(*) FROM fact_revenue";

        ResultSet delegateRs = onRowResultSet("count(*)", "9999");
        when(mockDelegate.executeQuery(cleanSql)).thenReturn(delegateRs);

        ResultSet result = statement.executeQuery(sql);
        // GET 不应被调用（强制刷新跳过读取）
        verify(mockCache, never()).get(anyString());
        // SET 应被调用（存储新结果）
        verify(mockCache).set(anyString(), any(byte[].class), eq(300));
        assertTrue(result.next());
        assertEquals("9999", result.getString(1));
    }

    // -------------------------------------------------------------------------
    // 助手方法
    // -------------------------------------------------------------------------

    private ResultSet onRowResultSet(String col, String val) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        java.sql.ResultSetMetaData meta = mock(java.sql.ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnName(1)).thenReturn(col);
        when(meta.getColumnLabel(1)).thenReturn(col);
        when(meta.getColumnType(1)).thenReturn(java.sql.Types.VARCHAR);

        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn(val);
        when(rs.getString(1)).thenReturn(val);
        return rs;
    }
}
