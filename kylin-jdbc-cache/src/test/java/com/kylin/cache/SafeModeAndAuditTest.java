package com.kylin.cache;

import com.kylin.CachedKylinConnection;
import com.kylin.CachedKylinStatement;
import com.kylin.DriverConfig;
import com.kylin.record.SqlExecutionTrace;
import com.kylin.record.SqlExecutionTraceService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SafeModeAndAuditTest {

    private CachedKylinConnection mockKylinConnection(Connection connection, DriverConfig config) throws Exception {
        com.kylin.datasource.DataSourceRegistry registry = mock(com.kylin.datasource.DataSourceRegistry.class);
        when(registry.getConnection(anyString())).thenReturn(connection);
        when(registry.getDefaultDataSourceName()).thenReturn("test");
        when(registry.getConfig(anyString())).thenReturn(new com.kylin.datasource.DataSourceConfig("test", "kylin", "driver", "url", "", "", false, 0, 0, 0));
        config.setDataSourceRegistry(registry);
        return new CachedKylinConnection(config, mock(RedisCacheManager.class));
    }

    @Test
    public void testSafeModeBypassesVolatileSqlCaching() throws Exception {
        RedisCacheManager cache = mock(RedisCacheManager.class);
        Statement delegate = mock(Statement.class);
        Connection connection = mock(Connection.class);
        when(connection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(delegate);
        DriverConfig config = DriverConfig.parse(
                "jdbc:kylin-cached://localhost/proj?cache.safe_mode.enabled=true",
                null);

        CachedKylinStatement statement =
                new CachedKylinStatement(mockKylinConnection(connection, config), new CacheLogic(cache, config, new CachePolicy(config)), new CachePolicy(config), mock(SqlExecutionTraceService.class));
        ResultSet liveRs = scalarResultSet(java.sql.Types.TIMESTAMP, "TIMESTAMP", "2026-03-28 10:00:00");
        when(delegate.executeQuery("SELECT CURRENT_TIMESTAMP")).thenReturn(liveRs);

        ResultSet result = statement.executeQuery("SELECT CURRENT_TIMESTAMP");

        assertSame(liveRs, result);
        verify(cache, never()).get(anyString());
        verify(cache, never()).set(anyString(), any(byte[].class), anyInt());
        verify(delegate).executeQuery("SELECT CURRENT_TIMESTAMP");
    }

    @Test
    public void testSafeModeDisabledAllowsVolatileSqlCaching() throws Exception {
        RedisCacheManager cache = mock(RedisCacheManager.class);
        Statement delegate = mock(Statement.class);
        Connection connection = mock(Connection.class);
        when(connection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(delegate);
        DriverConfig config = DriverConfig.parse(
                "jdbc:kylin-cached://localhost/proj?cache.safe_mode.enabled=false",
                null);

        CachedKylinStatement statement =
                new CachedKylinStatement(mockKylinConnection(connection, config), new CacheLogic(cache, config, new CachePolicy(config)), new CachePolicy(config), mock(SqlExecutionTraceService.class));
        ResultSet liveRs = scalarResultSet(java.sql.Types.TIMESTAMP, "TIMESTAMP", "2026-03-28 10:00:00");
        when(cache.get(anyString())).thenReturn(null);
        when(delegate.executeQuery("SELECT CURRENT_TIMESTAMP")).thenReturn(liveRs);

        ResultSet result = statement.executeQuery("SELECT CURRENT_TIMESTAMP");

        assertNotSame(liveRs, result);
        verify(cache).get(anyString());
        verify(cache).set(anyString(), any(byte[].class), eq(300));
    }

    @Test
    public void testSafeModeBypassesUnsupportedResultSetTypes() throws Exception {
        RedisCacheManager cache = mock(RedisCacheManager.class);
        Statement delegate = mock(Statement.class);
        Connection connection = mock(Connection.class);
        when(connection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(delegate);
        DriverConfig config = DriverConfig.parse(
                "jdbc:kylin-cached://localhost/proj?cache.safe_mode.enabled=true",
                null);

        CachedKylinStatement statement =
                new CachedKylinStatement(mockKylinConnection(connection, config), new CacheLogic(cache, config, new CachePolicy(config)), new CachePolicy(config), mock(SqlExecutionTraceService.class));
        ResultSet liveRs = scalarResultSet(java.sql.Types.BINARY, "BINARY", "[1,2,3]");
        when(cache.get(anyString())).thenReturn(null);
        when(delegate.executeQuery("SELECT payload FROM fact")).thenReturn(liveRs);

        ResultSet result = statement.executeQuery("SELECT payload FROM fact");

        assertSame(liveRs, result);
        verify(cache).get(anyString());
        verify(cache, never()).set(anyString(), any(byte[].class), anyInt());
    }

    @Test
    public void testSqlTraceReportedOnCacheHit() throws Exception {
        RedisCacheManager cache = mock(RedisCacheManager.class);
        Connection delegateConn = mock(Connection.class);
        Statement businessStmt = mock(Statement.class);
        SqlExecutionTraceService traceService = mock(SqlExecutionTraceService.class);
        when(delegateConn.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(businessStmt);

        DriverConfig config = DriverConfig.parse(
                "jdbc:kylin-cached://localhost/proj",
                null);
        CachedKylinConnection connection = mockKylinConnectionWithTrace(delegateConn, config, traceService, cache);

        ResultSet seed = scalarResultSet(java.sql.Types.VARCHAR, "VARCHAR", "hit");
        byte[] cachedBytes = ResultSetSerializer.serialize(seed);
        when(cache.get(anyString())).thenReturn(cachedBytes);

        ResultSet result = connection.createStatement().executeQuery("SELECT 1");

        assertTrue(result.next());
        assertEquals("hit", result.getString(1));
        ArgumentCaptor<SqlExecutionTrace> traceCaptor = ArgumentCaptor.forClass(SqlExecutionTrace.class);
        verify(traceService).reportExecution(traceCaptor.capture());
        assertTrue(traceCaptor.getValue().success);
        assertEquals(Boolean.TRUE, traceCaptor.getValue().cacheHit);
        verify(delegateConn, times(1)).createStatement(anyInt(), anyInt(), anyInt());
    }

    private CachedKylinConnection mockKylinConnectionWithTrace(Connection connection, DriverConfig config, SqlExecutionTraceService traceService, RedisCacheManager cache) throws Exception {
        com.kylin.datasource.DataSourceRegistry registry = mock(com.kylin.datasource.DataSourceRegistry.class);
        when(registry.getConnection(anyString())).thenReturn(connection);
        when(registry.getDefaultDataSourceName()).thenReturn("test");
        when(registry.getConfig(anyString())).thenReturn(new com.kylin.datasource.DataSourceConfig("test", "kylin", "driver", "url", "", "", false, 0, 0, 0));
        config.setDataSourceRegistry(registry);
        return new CachedKylinConnection(config, cache, traceService);
    }

    @Test
    public void testSqlTraceReportedOnCacheMiss() throws Exception {
        RedisCacheManager cache = mock(RedisCacheManager.class);
        Connection delegateConn = mock(Connection.class);
        Statement businessStmt = mock(Statement.class);
        SqlExecutionTraceService traceService = mock(SqlExecutionTraceService.class);
        when(delegateConn.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(businessStmt);

        DriverConfig config = DriverConfig.parse(
                "jdbc:kylin-cached://localhost/proj",
                null);
        CachedKylinConnection connection = mockKylinConnectionWithTrace(delegateConn, config, traceService, cache);

        when(cache.get(anyString())).thenReturn(null);
        ResultSet liveRs = scalarResultSet(java.sql.Types.VARCHAR, "VARCHAR", "miss");
        when(businessStmt.executeQuery("SELECT 'B'")).thenReturn(liveRs);

        ResultSet result = connection.createStatement().executeQuery("SELECT 'B'");

        assertTrue(result.next());
        assertEquals("miss", result.getString(1));
        ArgumentCaptor<SqlExecutionTrace> traceCaptor = ArgumentCaptor.forClass(SqlExecutionTrace.class);
        verify(traceService).reportExecution(traceCaptor.capture());
        assertTrue(traceCaptor.getValue().success);
        assertEquals(Boolean.FALSE, traceCaptor.getValue().cacheHit);
        verify(businessStmt).executeQuery("SELECT 'B'");
    }

    @Test
    public void testSqlTraceReportedOnExecutionFailure() throws Exception {
        RedisCacheManager cache = mock(RedisCacheManager.class);
        Connection delegateConn = mock(Connection.class);
        Statement businessStmt = mock(Statement.class);
        SqlExecutionTraceService traceService = mock(SqlExecutionTraceService.class);
        when(delegateConn.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(businessStmt);

        DriverConfig config = DriverConfig.parse(
                "jdbc:kylin-cached://localhost/proj",
                null);
        CachedKylinConnection connection = mockKylinConnectionWithTrace(delegateConn, config, traceService, cache);

        when(cache.get(anyString())).thenReturn(null);
        when(businessStmt.executeQuery("SELECT 'C'")).thenThrow(new java.sql.SQLException("boom"));

        try {
            connection.createStatement().executeQuery("SELECT 'C'");
            fail("expected SQLException");
        } catch (java.sql.SQLException expected) {
            assertEquals("boom", expected.getMessage());
        }

        ArgumentCaptor<SqlExecutionTrace> traceCaptor = ArgumentCaptor.forClass(SqlExecutionTrace.class);
        verify(traceService).reportExecution(traceCaptor.capture());
        assertFalse(traceCaptor.getValue().success);
        assertEquals("boom", traceCaptor.getValue().errorMessage);
        verify(businessStmt).executeQuery("SELECT 'C'");
    }

    private ResultSet scalarResultSet(int sqlType, String typeName, Object value) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(meta);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnName(1)).thenReturn("col");
        when(meta.getColumnLabel(1)).thenReturn("col");
        when(meta.getColumnType(1)).thenReturn(sqlType);
        when(meta.getColumnTypeName(1)).thenReturn(typeName);
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn(value);
        return rs;
    }
}
