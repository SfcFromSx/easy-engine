package com.smartbi.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.api.dto.PreparedQueryRequestDto;
import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.api.dto.StatementParameterDto;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.datasource.ManagedDataSourceRegistry;
import com.smartbi.query.integration.QueryCacheStore;
import com.smartbi.query.route.RoutedSql;
import com.smartbi.query.route.SqlRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryExecutionServiceTest {

    // Covers QueryExecutionService#execute statement happy path and QueryExecutionService#executeAgainstDatasource statement branch.
    @Test
    void shouldExecuteStatementQueriesAgainstDatasourceAndCacheTheResponse() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("SELECT NAME FROM SALES");
        RoutedSql routed = new RoutedSql("default", "h2", request.getSql(), request.getSql());
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        SqlResponseStubDto response = response("default", "alpha");
        response.setDuration(12L);

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);
        when(registry.getConnection("default")).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(request.getSql())).thenReturn(resultSet);
        when(mapper.toResponse(eq(resultSet), eq("default"), anyLong())).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);
        String expectedCacheKey = cacheService.buildKey(com.smartbi.query.parsing.SqlCommentParser.parse(request.getSql()), null, "default");

        assertEquals("default", actual.getCube());
        assertEquals("alpha", actual.getResults().get(0)[0]);
        verify(cacheService).put(eq(expectedCacheKey), any(), eq(response));
        verify(traceReportingService).report(eq(routed), any(), eq(null), eq(null), eq("STATEMENT"), eq(true), eq(expectedCacheKey), eq(false), eq(12L), eq(null));
    }

    // Covers QueryExecutionService#execute prepared happy path, QueryExecutionService#bindParameters, and QueryExecutionService#resolveExecutionMode prepared branch for non-Kylin datasources.
    @Test
    void shouldExecutePreparedQueriesAndBindConvertedParameters() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("SELECT NAME FROM SALES WHERE ID = ?", param("java.lang.Integer", "1"));
        RoutedSql routed = new RoutedSql("default", "h2", request.getSql(), request.getSql());
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        SqlResponseStubDto response = response("default", "alpha");
        response.setDuration(15L);

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);
        when(registry.getConnection("default")).thenReturn(connection);
        when(connection.prepareStatement(request.getSql())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(mapper.toResponse(eq(resultSet), eq("default"), anyLong())).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);
        String expectedCacheKey = cacheService.buildKey(com.smartbi.query.parsing.SqlCommentParser.parse(request.getSql()),
                "1=19:java.lang.Integer:1;", "default");

        assertEquals("alpha", actual.getResults().get(0)[0]);
        verify(statement).setObject(1, Integer.valueOf(1));
        verify(traceReportingService).report(eq(routed), any(), eq("1=19:java.lang.Integer:1;"), eq(request.getParams()),
                eq("PREPARED_STATEMENT"), eq(true), eq(expectedCacheKey), eq(false), eq(15L), eq(null));
    }

    // Covers QueryExecutionService#executeAgainstDatasource Kylin literalization branch, including numeric, quoted-string, date, and null parameter rendering.
    @Test
    void shouldLiteralizePreparedParametersForKylinDatasources() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request(
                "SELECT * FROM SALES WHERE ID > ? AND NAME = ? AND CREATED_AT > ? AND DELETED_AT = ?",
                param("java.lang.Integer", "1"),
                param("java.lang.String", "O'Brien"),
                param("java.sql.Date", "2026-03-31"),
                param("java.lang.String", null));
        RoutedSql routed = new RoutedSql("default", "kylin", request.getSql(), request.getSql());
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        SqlResponseStubDto response = response("default", "alpha");
        response.setDuration(14L);
        String expectedSql = "SELECT * FROM SALES WHERE ID > 1 AND NAME = 'O''Brien' AND CREATED_AT > '2026-03-31' AND DELETED_AT = NULL";

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);
        when(registry.getConnection("default")).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(expectedSql)).thenReturn(resultSet);
        when(mapper.toResponse(eq(resultSet), eq("default"), anyLong())).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);

        assertEquals("alpha", actual.getResults().get(0)[0]);
        verify(connection, never()).prepareStatement(any());
        verify(statement).executeQuery(expectedSql);
        verify(traceReportingService).report(eq(routed), any(), any(), eq(request.getParams()),
                eq("PREPARED_STATEMENT"), eq(true), any(), eq(false), eq(14L), eq(null));
    }

    // Covers QueryExecutionService#literalizeKylinPreparedSql placeholder-count mismatch branch before datasource execution.
    @Test
    void shouldRejectKylinPreparedQueriesWhenPlaceholderCountDoesNotMatch() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("SELECT NAME FROM SALES WHERE ID = ? AND NAME = ?", param("java.lang.Integer", "1"));
        RoutedSql routed = new RoutedSql("default", "kylin", request.getSql(), request.getSql());
        SqlResponseStubDto response = new SqlResponseStubDto();
        response.setIsException(true);
        String message = "Prepared parameter count 1 does not match placeholder count 2 for Kylin SQL";
        response.setExceptionMessage(message);

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);
        when(mapper.exceptionResponse(eq("default"), anyLong(), eq(message))).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);

        assertTrue(actual.getIsException());
        assertEquals(message, actual.getExceptionMessage());
        verify(registry, never()).getConnection(any());
        verify(traceReportingService).report(eq(routed), any(), any(), eq(request.getParams()),
                eq("PREPARED_STATEMENT"), eq(false), any(), eq(false), anyLong(), eq(message));
    }

    // Covers QueryExecutionService#convertValue unsupported-class fallback and PreparedParameterSupport cache-disable behavior.
    @Test
    void shouldFallbackToRawStringsAndDisablePreparedCachingForUnsupportedParameterTypes() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("SELECT NAME FROM SALES WHERE CREATED_AT = ?", param("java.util.Date", "2026-03-31T12:34:56Z"));
        RoutedSql routed = new RoutedSql("default", "h2", request.getSql(), request.getSql());
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        SqlResponseStubDto response = response("default", "alpha");
        response.setDuration(11L);

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);
        when(registry.getConnection("default")).thenReturn(connection);
        when(connection.prepareStatement(request.getSql())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(mapper.toResponse(eq(resultSet), eq("default"), anyLong())).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);

        assertEquals("alpha", actual.getResults().get(0)[0]);
        verify(statement).setObject(1, "2026-03-31T12:34:56Z");
        verify(cacheService, never()).tryGet(any(), any(), any());
        verify(cacheService, never()).put(any(), any(), any(), any());
        verify(traceReportingService).report(eq(routed), any(), eq(null), eq(request.getParams()),
                eq("PREPARED_STATEMENT"), eq(true), eq(null), eq(false), eq(11L), eq(null));
    }

    // Covers QueryExecutionService#execute non-query rejection branch.
    @Test
    void shouldRejectNonQuerySqlWithExceptionPayload() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("DELETE FROM SALES");
        RoutedSql routed = new RoutedSql("default", "h2", request.getSql(), request.getSql());
        SqlResponseStubDto response = new SqlResponseStubDto();
        response.setIsException(true);
        response.setExceptionMessage("Only query SQL is supported by engine-query");

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);
        when(mapper.exceptionResponse(eq("default"), anyLong(), eq("Only query SQL is supported by engine-query"))).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);

        assertTrue(actual.getIsException());
        verify(registry, never()).getConnection(any());
        verify(traceReportingService).report(eq(routed), any(), eq(null), eq(null), eq("STATEMENT"), eq(false), eq(null), eq(false), anyLong(),
                eq("Only query SQL is supported by engine-query"));
    }

    // Covers QueryExecutionService#execute blank-request rejection before routing or cache work.
    @Test
    void shouldRejectBlankQueryRequestsBeforeRouting() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("   ");
        SqlResponseStubDto response = new SqlResponseStubDto();
        String message = "Query request must include SQL";
        response.setIsException(true);
        response.setExceptionMessage(message);

        when(registry.getDefaultName()).thenReturn("default");
        when(mapper.exceptionResponse(eq("default"), anyLong(), eq(message))).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);

        assertTrue(actual.getIsException());
        assertEquals(message, actual.getExceptionMessage());
        verify(routeService, never()).routeAndRewrite(any(), any());
        verify(cacheService, never()).tryGet(any(), any(), any());
        verify(cacheService, never()).tryGet(any());
        verify(cacheService, never()).put(any(), any(), any(), any());
        verify(cacheService, never()).put(any(), any(), any());
        verify(traceReportingService, never()).report(any(), any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), anyLong(), any());
    }

    // Covers QueryExecutionService#execute null-request rejection before routing or cache work.
    @Test
    void shouldRejectNullQueryRequestsBeforeRouting() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        SqlResponseStubDto response = new SqlResponseStubDto();
        String message = "Query request must include SQL";
        response.setIsException(true);
        response.setExceptionMessage(message);

        when(registry.getDefaultName()).thenReturn("default");
        when(mapper.exceptionResponse(eq("default"), anyLong(), eq(message))).thenReturn(response);

        SqlResponseStubDto actual = service.execute(null);

        assertTrue(actual.getIsException());
        assertEquals(message, actual.getExceptionMessage());
        verify(routeService, never()).routeAndRewrite(any(), any());
        verify(cacheService, never()).tryGet(any(), any(), any());
        verify(cacheService, never()).tryGet(any());
        verify(cacheService, never()).put(any(), any(), any(), any());
        verify(cacheService, never()).put(any(), any(), any());
        verify(traceReportingService, never()).report(any(), any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), anyLong(), any());
    }

    // Covers QueryExecutionService#execute unsupported-request-shape rejection before routing or cache work.
    @Test
    void shouldRejectUnsupportedRequestShapesBeforeRouting() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("SELECT NAME FROM SALES");
        SqlResponseStubDto response = new SqlResponseStubDto();
        String message = "Only query requests are supported by engine-query; unsupported fields: prepareSql";
        response.setIsException(true);
        response.setExceptionMessage(message);
        request.captureUnsupportedProperty("prepareSql", Boolean.TRUE);

        when(registry.getDefaultName()).thenReturn("default");
        when(mapper.exceptionResponse(eq("default"), anyLong(), eq(message))).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);

        assertTrue(actual.getIsException());
        assertEquals(message, actual.getExceptionMessage());
        verify(routeService, never()).routeAndRewrite(any(), any());
        verify(cacheService, never()).tryGet(any(), any(), any());
        verify(cacheService, never()).tryGet(any());
        verify(cacheService, never()).put(any(), any(), any(), any());
        verify(cacheService, never()).put(any(), any(), any());
        verify(traceReportingService, never()).report(any(), any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), anyLong(), any());
    }

    // Covers QueryExecutionService#execute cache-hit branch.
    @Test
    void shouldReturnCachedResponsesWithoutOpeningDatasourceConnections() throws Exception {
        MapCacheStore store = new MapCacheStore();
        QueryProperties properties = new QueryProperties();
        QueryCacheService cacheService = spy(new QueryCacheService(store, properties, new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, properties);
        PreparedQueryRequestDto request = request("SELECT NAME FROM SALES");
        RoutedSql routed = new RoutedSql("default", "h2", request.getSql(), request.getSql());
        SqlResponseStubDto cached = response("default", "alpha");
        cacheService.put(com.smartbi.query.parsing.SqlCommentParser.parse(request.getSql()), null, "default", cached);

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);

        SqlResponseStubDto actual = service.execute(request);
        String expectedCacheKey = cacheService.buildKey(com.smartbi.query.parsing.SqlCommentParser.parse(request.getSql()), null, "default");

        assertTrue(actual.isStorageCacheUsed());
        verify(registry, never()).getConnection(any());
        verify(traceReportingService).report(eq(routed), any(), eq(null), eq(null), eq("STATEMENT"), eq(true), eq(expectedCacheKey), eq(true), anyLong(), eq(null));
    }

    // Covers QueryExecutionService#execute cache-bypass branches for no-cache and force-refresh metadata.
    @Test
    void shouldSkipCacheLookupWhenSqlForcesNoCacheOrRefresh() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("-- no-cache\n-- force-refresh\nSELECT NAME FROM SALES");
        RoutedSql routed = new RoutedSql("default", "h2", request.getSql(), "SELECT NAME FROM SALES");
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        SqlResponseStubDto response = response("default", "alpha");
        response.setDuration(9L);

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);
        when(registry.getConnection("default")).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT NAME FROM SALES")).thenReturn(resultSet);
        when(mapper.toResponse(eq(resultSet), eq("default"), anyLong())).thenReturn(response);

        service.execute(request);

        verify(cacheService, never()).tryGet(any(), any(), any());
        verify(cacheService, never()).tryGet(any());
        verify(traceReportingService).report(eq(routed), any(), eq(null), eq(null), eq("STATEMENT"), eq(true), eq(null), eq(false), eq(9L), eq(null));
    }

    // Covers QueryExecutionService#execute datasource-error branch.
    @Test
    void shouldReturnExceptionResponseWhenDatasourceExecutionFails() throws Exception {
        QueryCacheService cacheService = spy(new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()));
        SqlRouteService routeService = mock(SqlRouteService.class);
        ManagedDataSourceRegistry registry = mock(ManagedDataSourceRegistry.class);
        QueryResultMapper mapper = mock(QueryResultMapper.class);
        TraceReportingService traceReportingService = mock(TraceReportingService.class);
        QueryExecutionService service = new QueryExecutionService(cacheService, routeService, registry, mapper, traceReportingService, new QueryProperties());
        PreparedQueryRequestDto request = request("SELECT NAME FROM SALES");
        RoutedSql routed = new RoutedSql("default", "h2", request.getSql(), request.getSql());
        SqlResponseStubDto response = new SqlResponseStubDto();
        response.setIsException(true);
        response.setExceptionMessage("boom");

        when(routeService.routeAndRewrite(eq(request.getSql()), any())).thenReturn(routed);
        when(registry.getConnection("default")).thenThrow(new java.sql.SQLException("boom"));
        when(mapper.exceptionResponse(eq("default"), anyLong(), eq("boom"))).thenReturn(response);

        SqlResponseStubDto actual = service.execute(request);
        String expectedCacheKey = cacheService.buildKey(com.smartbi.query.parsing.SqlCommentParser.parse(request.getSql()), null, "default");

        assertTrue(actual.getIsException());
        verify(traceReportingService).report(eq(routed), any(), eq(null), eq(null), eq("STATEMENT"), eq(false), eq(expectedCacheKey), eq(false), anyLong(), eq("boom"));
    }

    // Covers QueryExecutionService#convertValue supported-type, null, and fallback branches.
    @Test
    void shouldConvertSupportedParameterTypesAndFallbackToRawStrings() {
        QueryExecutionService service = new QueryExecutionService(
                new QueryCacheService(new MapCacheStore(), new QueryProperties(), new ObjectMapper()),
                mock(SqlRouteService.class),
                mock(ManagedDataSourceRegistry.class),
                mock(QueryResultMapper.class),
                mock(TraceReportingService.class),
                new QueryProperties()
        );

        assertNull(ReflectionTestUtils.invokeMethod(service, "convertValue", new Object[]{null}));
        assertNull(ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.lang.Integer", null)));
        assertEquals("hello", ReflectionTestUtils.invokeMethod(service, "convertValue", param(null, "hello")));
        assertEquals(Integer.valueOf(1), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.lang.Integer", "1")));
        assertEquals(Long.valueOf(2L), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.lang.Long", "2")));
        assertEquals(Short.valueOf((short) 3), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.lang.Short", "3")));
        assertEquals(Double.valueOf(4.5D), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.lang.Double", "4.5")));
        assertEquals(Float.valueOf(6.5F), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.lang.Float", "6.5")));
        assertEquals(new BigDecimal("7.5"), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.math.BigDecimal", "7.5")));
        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.lang.Boolean", "true")));
        assertEquals(Date.valueOf("2026-03-31"), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.sql.Date", "2026-03-31")));
        assertEquals(Time.valueOf("12:34:56"), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.sql.Time", "12:34:56")));
        assertEquals(Timestamp.valueOf("2026-03-31 12:34:56"), ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.sql.Timestamp", "2026-03-31 12:34:56")));
        assertEquals("fallback", ReflectionTestUtils.invokeMethod(service, "convertValue", param("java.util.UUID", "fallback")));
    }

    // Covers QueryExecutionService#resolveExecutionMode statement and prepared branches.
    @Test
    void shouldResolveExecutionModeFromParameterPresence() throws Exception {
        java.lang.reflect.Method method = QueryExecutionService.class.getDeclaredMethod("resolveExecutionMode", List.class);
        method.setAccessible(true);

        assertEquals("STATEMENT", method.invoke(null, new Object[]{null}));
        assertEquals("STATEMENT", method.invoke(null, java.util.Collections.emptyList()));
        assertEquals("PREPARED_STATEMENT", method.invoke(null, java.util.Collections.singletonList(param("java.lang.Integer", "1"))));
    }

    private static PreparedQueryRequestDto request(String sql, StatementParameterDto... params) {
        PreparedQueryRequestDto request = new PreparedQueryRequestDto();
        request.setSql(sql);
        if (params != null && params.length > 0) {
            request.setParams(Arrays.asList(params));
        }
        return request;
    }

    private static StatementParameterDto param(String className, String value) {
        StatementParameterDto dto = new StatementParameterDto();
        dto.setClassName(className);
        dto.setValue(value);
        return dto;
    }

    private static SqlResponseStubDto response(String cube, String firstValue) {
        SqlResponseStubDto response = new SqlResponseStubDto();
        response.setCube(cube);
        response.setResults(java.util.Collections.singletonList(new String[]{firstValue}));
        return response;
    }

    private static class MapCacheStore implements QueryCacheStore {
        private final Map<String, String> values = new HashMap<String, String>();

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public void set(String key, String value, int ttlSeconds) {
            values.put(key, value);
        }
    }
}
