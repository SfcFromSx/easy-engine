package com.smartbi.query.service;

import com.smartbi.query.api.dto.PreparedQueryRequestDto;
import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.api.dto.StatementParameterDto;
import com.smartbi.query.cache.CachePolicy;
import com.smartbi.query.cache.PreparedParameterSupport;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.datasource.ManagedDataSourceRegistry;
import com.smartbi.query.parsing.SqlCommentParser;
import com.smartbi.query.route.RoutedSql;
import com.smartbi.query.route.SqlRouteService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

@Service
public class QueryExecutionService {

    private final QueryCacheService queryCacheService;
    private final SqlRouteService sqlRouteService;
    private final ManagedDataSourceRegistry managedDataSourceRegistry;
    private final QueryResultMapper queryResultMapper;
    private final TraceReportingService traceReportingService;
    private final QueryProperties queryProperties;

    public QueryExecutionService(QueryCacheService queryCacheService,
                                 SqlRouteService sqlRouteService,
                                 ManagedDataSourceRegistry managedDataSourceRegistry,
                                 QueryResultMapper queryResultMapper,
                                 TraceReportingService traceReportingService,
                                 QueryProperties queryProperties) {
        this.queryCacheService = queryCacheService;
        this.sqlRouteService = sqlRouteService;
        this.managedDataSourceRegistry = managedDataSourceRegistry;
        this.queryResultMapper = queryResultMapper;
        this.traceReportingService = traceReportingService;
        this.queryProperties = queryProperties;
    }

    public SqlResponseStubDto execute(PreparedQueryRequestDto request) {
        long startedAt = System.nanoTime();
        String originalSql = request == null ? null : request.getSql();
        String executionMode = resolveExecutionMode(request == null ? null : request.getParams());
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.safeParse(originalSql);
        RoutedSql routed = sqlRouteService.routeAndRewrite(originalSql, parsed);
        CachePolicy cachePolicy = queryCacheService.getCachePolicy();
        String paramFingerprint = PreparedParameterSupport.fingerprint(
                request == null ? null : request.getParams(),
                routed.executionSql,
                queryProperties.getCache().isPreparedSqlEnabled(),
                cachePolicy);
        boolean parameterCacheable = PreparedParameterSupport.shouldUseCache(
                routed.executionSql,
                request == null ? null : request.getParams(),
                queryProperties.getCache().isPreparedSqlEnabled(),
                cachePolicy);

        if (!cachePolicy.isQuerySql(parsed.cleanSql)) {
            long durationMs = elapsedMs(startedAt);
            SqlResponseStubDto response = queryResultMapper.exceptionResponse(routed.datasourceName, durationMs,
                    "Only query SQL is supported by engine-query");
            traceReportingService.report(routed, parsed, paramFingerprint, executionMode, false, false, durationMs,
                    response.getExceptionMessage());
            return response;
        }

        boolean skipLookup = parsed.metadata.noCache || parsed.metadata.cacheRefresh || cachePolicy.shouldBypassCacheBeforeLookup(parsed);
        if (!skipLookup && parameterCacheable) {
            SqlResponseStubDto cached = queryCacheService.tryGet(parsed, paramFingerprint, routed.datasourceName);
            if (cached != null) {
                cached.setStorageCacheUsed(true);
                cached.setDuration(elapsedMs(startedAt));
                traceReportingService.report(routed, parsed, paramFingerprint, executionMode, true, true,
                        cached.getDuration(), null);
                return cached;
            }
        }

        try (Connection connection = managedDataSourceRegistry.getConnection(routed.datasourceName)) {
            SqlResponseStubDto response = executeAgainstDatasource(connection, routed, request == null ? null : request.getParams(), startedAt);
            if (!parsed.metadata.noCache && parameterCacheable) {
                queryCacheService.put(parsed, paramFingerprint, routed.datasourceName, response);
            }
            traceReportingService.report(routed, parsed, paramFingerprint, executionMode, true, false,
                    response.getDuration(), null);
            return response;
        } catch (Exception ex) {
            long durationMs = elapsedMs(startedAt);
            SqlResponseStubDto response = queryResultMapper.exceptionResponse(routed.datasourceName, durationMs, ex.getMessage());
            traceReportingService.report(routed, parsed, paramFingerprint, executionMode, false, false, durationMs,
                    ex.getMessage());
            return response;
        }
    }

    private SqlResponseStubDto executeAgainstDatasource(Connection connection,
                                                        RoutedSql routed,
                                                        List<StatementParameterDto> params,
                                                        long startedAt) throws Exception {
        if (params == null || params.isEmpty()) {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(routed.executionSql)) {
                return queryResultMapper.toResponse(rs, routed.datasourceName, elapsedMs(startedAt));
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(routed.executionSql)) {
            bindParameters(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return queryResultMapper.toResponse(rs, routed.datasourceName, elapsedMs(startedAt));
            }
        }
    }

    private void bindParameters(PreparedStatement statement, List<StatementParameterDto> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            StatementParameterDto param = params.get(i);
            Object value = convertValue(param);
            statement.setObject(i + 1, value);
        }
    }

    private Object convertValue(StatementParameterDto param) {
        if (param == null || param.getValue() == null) {
            return null;
        }
        String className = param.getClassName();
        String value = param.getValue();
        if (!StringUtils.hasText(className)) {
            return value;
        }
        if ("java.lang.String".equals(className)) {
            return value;
        }
        if ("java.lang.Integer".equals(className)) {
            return Integer.valueOf(value);
        }
        if ("java.lang.Long".equals(className)) {
            return Long.valueOf(value);
        }
        if ("java.lang.Short".equals(className)) {
            return Short.valueOf(value);
        }
        if ("java.lang.Double".equals(className)) {
            return Double.valueOf(value);
        }
        if ("java.lang.Float".equals(className)) {
            return Float.valueOf(value);
        }
        if ("java.math.BigDecimal".equals(className)) {
            return new BigDecimal(value);
        }
        if ("java.lang.Boolean".equals(className)) {
            return Boolean.valueOf(value);
        }
        if ("java.sql.Date".equals(className)) {
            return Date.valueOf(value);
        }
        if ("java.sql.Time".equals(className)) {
            return Time.valueOf(value);
        }
        if ("java.sql.Timestamp".equals(className)) {
            return Timestamp.valueOf(value);
        }
        return value;
    }

    private static long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private static String resolveExecutionMode(List<StatementParameterDto> params) {
        return params == null || params.isEmpty() ? "STATEMENT" : "PREPARED_STATEMENT";
    }
}
