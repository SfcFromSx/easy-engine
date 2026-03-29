package com.kylin.cache;

import com.kylin.CachedKylinPreparedStatement;
import com.kylin.CachedKylinStatement;
import com.kylin.DriverConfig;
import com.kylin.SqlCommentParser;
import com.kylin.record.SqlExecutionTrace;
import com.kylin.record.SqlExecutionTraceService;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 针对代码审阅结论的回归测试。
 *
 * 这些测试覆盖此前审阅中发现并已确认的问题点，确保修复后行为保持正确。
 */
public class ReviewVerificationTest {

    @After
    public void tearDown() throws Exception {
        clearRedisCacheManagerInstances();
    }

    private com.kylin.CachedKylinConnection mockKylinConnection(Connection connection, Statement delegate) throws Exception {
        com.kylin.datasource.DataSourceRegistry registry = mock(com.kylin.datasource.DataSourceRegistry.class);
        when(registry.getConnection(anyString())).thenReturn(connection);
        when(registry.getDefaultDataSourceName()).thenReturn("test");
        com.kylin.route.SqlRouteRewriteService routeService = mock(com.kylin.route.SqlRouteRewriteService.class);
        when(routeService.routeAndRewrite(anyString(), any())).thenAnswer(inv -> new com.kylin.route.RoutedSql("test", "kylin", inv.getArgument(0), inv.getArgument(0)));
        com.kylin.CachedKylinConnection kylinConn = mock(com.kylin.CachedKylinConnection.class);
        when(kylinConn.getRegistry()).thenReturn(registry);
        when(kylinConn.getRouteService()).thenReturn(routeService);
        if (delegate instanceof PreparedStatement) {
            when(connection.prepareStatement(anyString())).thenReturn((PreparedStatement) delegate);
            when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn((PreparedStatement) delegate);
            when(connection.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).thenReturn((PreparedStatement) delegate);
        } else {
            when(connection.createStatement()).thenReturn(delegate);
            when(connection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(delegate);
            when(connection.createStatement(anyInt(), anyInt())).thenReturn(delegate);
        }
        return kylinConn;
    }

    /**
     * 场景：BI 或 JDBC 框架不是调用 executeQuery(sql)，而是统一调用 execute(sql) 来执行 SELECT。
     * 验证点：SELECT 通过 execute(sql) 时也应走缓存逻辑，并且 getResultSet() 能取回缓存结果。
     */
    @Test
    public void testStatementExecuteSelectUsesCacheLogic() throws Exception {
        Statement delegate = mock(Statement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = mock(CachePolicy.class);
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        CachedKylinStatement statement =
                new CachedKylinStatement(mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService);
        ResultSet cached = mock(ResultSet.class);

        when(cachePolicy.isQuerySql(anyString())).thenReturn(true);
        when(cacheLogic.executeWithCacheDetailed(any(), isNull(), any(), anyString()))
                .thenReturn(new CacheLogic.CacheExecutionResult(cached, false, null));

        assertTrue(statement.execute("SELECT 1"));
        assertSame(cached, statement.getResultSet());

        ArgumentCaptor<SqlExecutionTrace> traceCaptor = ArgumentCaptor.forClass(SqlExecutionTrace.class);
        verify(sqlExecutionTraceService).reportExecution(traceCaptor.capture());
        assertEquals("SELECT 1", traceCaptor.getValue().originalSql);
        assertTrue(traceCaptor.getValue().success);
        verify(cacheLogic).executeWithCacheDetailed(any(), isNull(), any(), anyString());
        verify(delegate, never()).execute("SELECT 1");
    }

    /**
     * 场景：调用方使用 PreparedStatement.execute() 执行查询，而不是 executeQuery()。
     * 验证点：PreparedStatement.execute() 对查询也应走缓存逻辑，并能通过 getResultSet() 取回缓存结果。
     */
    @Test
    public void testPreparedStatementExecuteSelectUsesCacheLogic() throws Exception {
        PreparedStatement delegate = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = mock(CachePolicy.class);
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        ResultSet cached = mock(ResultSet.class);
        String sql = "SELECT * FROM fact WHERE id = ?";
        CachedKylinPreparedStatement ps =
                new CachedKylinPreparedStatement(
                        mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService,
                        true, sql, SqlCommentParser.parse(sql), -1, -1, -1, -1, null, null);

        when(cachePolicy.isQuerySql(anyString())).thenReturn(true);
        when(cacheLogic.executeWithCacheDetailed(any(), anyString(), any(), anyString()))
                .thenReturn(new CacheLogic.CacheExecutionResult(cached, false, null));

        assertTrue(ps.execute());
        assertSame(cached, ps.getResultSet());

        ArgumentCaptor<SqlExecutionTrace> traceCaptor = ArgumentCaptor.forClass(SqlExecutionTrace.class);
        verify(sqlExecutionTraceService).reportExecution(traceCaptor.capture());
        assertEquals(sql, traceCaptor.getValue().originalSql);
        assertEquals(Boolean.FALSE, traceCaptor.getValue().cacheHit);
        verify(cacheLogic).executeWithCacheDetailed(any(), eq(""), any(), anyString());
        verify(delegate, never()).execute();
    }

    /**
     * 场景：两个查询的 SQL 相同，但通过 setBytes 绑定了不同的 byte[] 参数。
     * 验证点：缓存键指纹应该能区分不同的二进制值，避免把不同参数误命中到同一份缓存。
     */
    @Test
    public void testSetBytesBuildsDifferentFingerprintsForDifferentValues() throws Exception {
        PreparedStatement delegate = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = mock(CachePolicy.class);
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        ResultSet mockResult = mock(ResultSet.class);
        when(cacheLogic.executeWithCacheDetailed(any(), anyString(), any(), anyString()))
                .thenReturn(new CacheLogic.CacheExecutionResult(mockResult, false, null));
        when(cachePolicy.isQuerySql(anyString())).thenReturn(true);
        String sql = "SELECT * FROM fact WHERE payload = ?";

        CachedKylinPreparedStatement ps =
                new CachedKylinPreparedStatement(
                        mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService,
                        true, sql, SqlCommentParser.parse(sql), -1, -1, -1, -1, null, null);

        ps.setBytes(1, new byte[]{1});
        ps.executeQuery();
        ps.clearParameters();

        ps.setBytes(1, new byte[]{2});
        ps.executeQuery();

        ArgumentCaptor<String> fingerprints = ArgumentCaptor.forClass(String.class);
        verify(cacheLogic, times(2)).executeWithCacheDetailed(any(), fingerprints.capture(), any(), anyString());

        List<String> values = fingerprints.getAllValues();
        assertNotEquals(values.get(0), values.get(1));
    }

    /**
     * 场景：查询参数通过 setBinaryStream 绑定，不是普通标量参数。
     * 验证点：对于无法稳定构建指纹的流式参数，驱动应安全降级为透传执行，而不是继续带着错误指纹走缓存。
     */
    @Test
    public void testBinaryStreamFallsBackToDirectExecution() throws Exception {
        PreparedStatement delegate = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = mock(CachePolicy.class);
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        ResultSet firstResult = mock(ResultSet.class);
        ResultSet secondResult = mock(ResultSet.class);
        when(delegate.executeQuery()).thenReturn(firstResult, secondResult);
        when(cachePolicy.isQuerySql(anyString())).thenReturn(true);
        String sql = "SELECT * FROM fact WHERE payload = ?";

        CachedKylinPreparedStatement ps =
                new CachedKylinPreparedStatement(
                        mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService,
                        true, sql, SqlCommentParser.parse(sql), -1, -1, -1, -1, null, null);

        ps.setBinaryStream(1, new ByteArrayInputStream(new byte[]{1, 2, 3}));
        assertSame(firstResult, ps.executeQuery());
        ps.clearParameters();

        ps.setBinaryStream(1, new ByteArrayInputStream(new byte[]{4, 5, 6}));
        assertSame(secondResult, ps.executeQuery());

        verify(delegate, times(2)).executeQuery();
        verify(sqlExecutionTraceService, times(2)).reportExecution(any(SqlExecutionTrace.class));
        verify(cacheLogic, never()).executeWithCacheDetailed(any(), anyString(), any(), anyString());
    }

    /**
     * 场景：调用方通过 setObject 传入非标量复杂对象，字符串表示不一定稳定。
     * 验证点：这类参数应直接降级为透传执行，避免继续参与缓存键导致错命中。
     */
    @Test
    public void testUnsupportedSetObjectFallsBackToDirectExecution() throws Exception {
        PreparedStatement delegate = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = new CachePolicy(DriverConfig.parse("jdbc:kylin-cached://localhost/proj", null));
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        ResultSet liveResult = mock(ResultSet.class);
        String sql = "SELECT * FROM fact WHERE payload = ?";

        when(delegate.executeQuery()).thenReturn(liveResult);

        CachedKylinPreparedStatement ps =
                new CachedKylinPreparedStatement(
                        mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService,
                        true, sql, SqlCommentParser.parse(sql), -1, -1, -1, -1, null, null);

        ps.setObject(1, Collections.singletonMap("k", "v"));

        assertSame(liveResult, ps.executeQuery());
        ArgumentCaptor<SqlExecutionTrace> traceCaptor = ArgumentCaptor.forClass(SqlExecutionTrace.class);
        verify(sqlExecutionTraceService).reportExecution(traceCaptor.capture());
        assertFalse(traceCaptor.getValue().cacheHit);
        verify(delegate).executeQuery();
        verify(cacheLogic, never()).executeWithCacheDetailed(any(), anyString(), any(), anyString());
    }

    /**
     * 场景：配置上明确关闭了带参数 SQL 的缓存能力。
     * 验证点：即使是普通标量参数，PreparedStatement 也应统一透传执行，不再进入缓存链路。
     */
    @Test
    public void testPreparedStatementCacheCanBeDisabled() throws Exception {
        PreparedStatement delegate = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = new CachePolicy(DriverConfig.parse("jdbc:kylin-cached://localhost/proj", null));
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        ResultSet liveResult = mock(ResultSet.class);
        String sql = "SELECT * FROM fact WHERE id = ?";

        when(delegate.executeQuery()).thenReturn(liveResult);

        CachedKylinPreparedStatement ps =
                new CachedKylinPreparedStatement(
                        mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService,
                        false, sql, SqlCommentParser.parse(sql), -1, -1, -1, -1, null, null);

        ps.setInt(1, 42);

        assertSame(liveResult, ps.executeQuery());
        verify(delegate).executeQuery();
        verify(cacheLogic, never()).executeWithCacheDetailed(any(), anyString(), any(), anyString());
    }

    /**
     * 场景：大屏 BI 只支持查询语句，不支持更新型 Statement 方法。
     * 验证点：executeUpdate(sql) 应直接抛出不支持异常，而不是尝试透传到底层驱动。
     */
    @Test
    public void testStatementExecuteUpdateIsRejected() throws Exception {
        Statement delegate = mock(Statement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = mock(CachePolicy.class);
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        CachedKylinStatement statement =
                new CachedKylinStatement(mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService);

        try {
            statement.executeUpdate("UPDATE fact SET c = 1");
            fail("expected SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException expected) {
            assertTrue(expected.getMessage().contains("Only SELECT-like queries are supported"));
        }

        verify(delegate, never()).executeUpdate(anyString());
    }

    /**
     * 场景：调用方用通用 execute(sql) 传入非查询 SQL。
     * 验证点：应直接拒绝，而不是把 UPDATE/DELETE 继续透传。
     */
    @Test
    public void testStatementExecuteNonQueryIsRejected() throws Exception {
        Statement delegate = mock(Statement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = mock(CachePolicy.class);
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        CachedKylinStatement statement =
                new CachedKylinStatement(mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService);

        when(cachePolicy.isQuerySql(anyString())).thenReturn(false);

        try {
            statement.execute("DELETE FROM fact");
            fail("expected SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException expected) {
            assertTrue(expected.getMessage().contains("Only SELECT-like queries are supported"));
        }

        verify(delegate, never()).execute(anyString());
    }

    /**
     * 场景：PreparedStatement 本身如果不是查询 SQL，也不应继续执行。
     * 验证点：execute()/executeUpdate()/addBatch() 等入口都应直接抛出不支持异常。
     */
    @Test
    public void testPreparedStatementNonQueryIsRejected() throws Exception {
        PreparedStatement delegate = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        CacheLogic cacheLogic = mock(CacheLogic.class);
        CachePolicy cachePolicy = mock(CachePolicy.class);
        SqlExecutionTraceService sqlExecutionTraceService = mock(SqlExecutionTraceService.class);
        String sql = "UPDATE fact SET c = ?";
        CachedKylinPreparedStatement ps =
                new CachedKylinPreparedStatement(
                        mockKylinConnection(connection, delegate), cacheLogic, cachePolicy, sqlExecutionTraceService,
                        true, sql, SqlCommentParser.parse(sql), -1, -1, -1, -1, null, null);

        when(cachePolicy.isQuerySql(anyString())).thenReturn(false);

        try {
            ps.execute();
            fail("expected SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException expected) {
            assertTrue(expected.getMessage().contains("Only SELECT-like queries are supported"));
        }

        try {
            ps.executeUpdate();
            fail("expected SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException expected) {
            assertTrue(expected.getMessage().contains("Only SELECT-like queries are supported"));
        }

        try {
            ps.addBatch();
            fail("expected SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException expected) {
            assertTrue(expected.getMessage().contains("Only SELECT-like queries are supported"));
        }

        verify(delegate, never()).execute();
        verify(delegate, never()).executeUpdate();
        verify(delegate, never()).addBatch();
    }

    /**
     * 场景：结果集中存在高精度 DECIMAL/NUMERIC，例如金额、汇总值或财务指标。
     * 验证点：经过 ResultSetSerializer 序列化再反序列化后，BigDecimal 是否还能保持原始精度。
     */
    @Test
    public void testDecimalPrecisionIsPreservedAfterCacheRoundTrip() throws Exception {
        BigDecimal original = new BigDecimal("12345678901234567890.123456789");
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnName(1)).thenReturn("amount");
        when(meta.getColumnLabel(1)).thenReturn("amount");
        when(meta.getColumnType(1)).thenReturn(Types.DECIMAL);
        when(meta.getColumnTypeName(1)).thenReturn("DECIMAL");
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn(original);

        byte[] bytes = ResultSetSerializer.serialize(rs);
        CachedResultSet cached = ResultSetSerializer.deserialize(bytes);

        assertTrue(cached.next());
        assertEquals(0, original.compareTo(cached.getBigDecimal(1)));
    }

    /**
     * 场景：调用方把缓存结果集当作可滚动 ResultSet 来使用，执行 absolute(0) 回到 beforeFirst。
     * 验证点：absolute(0) 应把游标放到 beforeFirst，而不是 afterLast。
     */
    @Test
    public void testAbsoluteZeroMovesCursorToBeforeFirst() throws Exception {
        CachedResultSet rs = new CachedResultSet(
                new String[]{"c"},
                new int[]{Types.INTEGER},
                new String[]{"INTEGER"},
                new String[]{"c"},
                Collections.<Object[]>singletonList(new Object[]{1}));

        assertFalse(rs.absolute(0));
        assertTrue(rs.isBeforeFirst());
        assertFalse(rs.isAfterLast());
        assertEquals(0, rs.getRow());
    }

    /**
     * 场景：查询结果为空，但上层框架仍然会调用 first()/isFirst()/getRow() 这类导航方法。
     * 验证点：空结果集上的游标状态应保持一致，不能出现“不在任何行上却显示第一行”的状态。
     */
    @Test
    public void testEmptyResultSetFirstKeepsCursorStateConsistent() throws Exception {
        CachedResultSet rs = new CachedResultSet(
                new String[]{"c"},
                new int[]{Types.INTEGER},
                new String[]{"INTEGER"},
                new String[]{"c"},
                Collections.<Object[]>emptyList());

        assertFalse(rs.first());
        assertFalse(rs.isFirst());
        assertEquals(0, rs.getRow());
    }

    /**
     * 场景：同一个 Redis 地址被不同连接配置复用，但这些连接对缓存大小、超时等参数要求不同。
     * 验证点：不同连接配置应拿到不同的 RedisCacheManager，避免后续连接默默复用首个实例的配置。
     */
    @Test
    public void testRedisCacheManagerSeparatesDifferentConfigsForSameEndpoint() throws Exception {
        DriverConfig firstConfig = DriverConfig.parse(
                "jdbc:kylin-cached://localhost/proj?redis.host=review-verification&cache.max_size_bytes=128&redis.timeout_ms=500",
                null);
        DriverConfig secondConfig = DriverConfig.parse(
                "jdbc:kylin-cached://localhost/proj?redis.host=review-verification&cache.max_size_bytes=4096&redis.timeout_ms=2000",
                null);

        RedisCacheManager first = RedisCacheManager.getInstance(firstConfig);
        RedisCacheManager second = RedisCacheManager.getInstance(secondConfig);

        assertNotSame(first, second);
        assertEquals(4096L, readLongField(second, "maxCacheSizeBytes"));
    }

    @SuppressWarnings("unchecked")
    private static void clearRedisCacheManagerInstances() throws Exception {
        Field instancesField = RedisCacheManager.class.getDeclaredField("INSTANCES");
        instancesField.setAccessible(true);
        Map<String, RedisCacheManager> instances =
                (ConcurrentHashMap<String, RedisCacheManager>) instancesField.get(null);
        for (RedisCacheManager manager : instances.values()) {
            manager.close();
        }
        instances.clear();
    }

    private static long readLongField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getLong(target);
    }
}
