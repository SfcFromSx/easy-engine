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
import java.util.StringJoiner;

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
        List<StatementParameterDto> params = request == null ? null : request.getParams();
        String executionMode = resolveExecutionMode(params);

        if (request != null && request.hasUnsupportedProperties()) {
            return invalidRequestResponse(startedAt, null, SqlCommentParser.safeParse(originalSql), params, executionMode,
                    unsupportedRequestMessage(request));
        }

        CachePolicy cachePolicy = queryCacheService.getCachePolicy();
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.safeParse(originalSql);

        if (!StringUtils.hasText(parsed.cleanSql)) {
            return invalidRequestResponse(startedAt, null, parsed, params, executionMode,
                    "Query request must include SQL");
        }

        RoutedSql routed = sqlRouteService.routeAndRewrite(originalSql, parsed);

        if (!cachePolicy.isQuerySql(parsed.cleanSql)) {
            return invalidRequestResponse(startedAt, routed, parsed, params, executionMode,
                    "Only query SQL is supported by engine-query");
        }

        String paramFingerprint = PreparedParameterSupport.fingerprint(
                params,
                routed.executionSql,
                queryProperties.getCache().isPreparedSqlEnabled(),
                cachePolicy);
        boolean parameterCacheable = PreparedParameterSupport.shouldUseCache(
                routed.executionSql,
                params,
                queryProperties.getCache().isPreparedSqlEnabled(),
                cachePolicy);

        boolean skipLookup = parsed.metadata.noCache || parsed.metadata.cacheRefresh || cachePolicy.shouldBypassCacheBeforeLookup(parsed);
        if (!skipLookup && parameterCacheable) {
            SqlResponseStubDto cached = queryCacheService.tryGet(parsed, paramFingerprint, routed.datasourceName);
            if (cached != null) {
                cached.setStorageCacheUsed(true);
                cached.setDuration(elapsedMs(startedAt));
                traceReportingService.report(routed, parsed, paramFingerprint, params, executionMode, true, true,
                        cached.getDuration(), null);
                return cached;
            }
        }

        String kylinPreparedSql = null;
        if (shouldLiteralizeKylinPrepared(routed, params)) {
            try {
                kylinPreparedSql = literalizeKylinPreparedSql(routed.executionSql, params);
            } catch (IllegalArgumentException ex) {
                long durationMs = elapsedMs(startedAt);
                SqlResponseStubDto response = queryResultMapper.exceptionResponse(routed.datasourceName, durationMs, ex.getMessage());
                traceReportingService.report(routed, parsed, paramFingerprint, params, executionMode, false, false,
                        durationMs, response.getExceptionMessage());
                return response;
            }
        }

        try (Connection connection = managedDataSourceRegistry.getConnection(routed.datasourceName)) {
            SqlResponseStubDto response = executeAgainstDatasource(connection, routed, params, kylinPreparedSql, startedAt);
            if (!parsed.metadata.noCache && parameterCacheable) {
                queryCacheService.put(parsed, paramFingerprint, routed.datasourceName, response);
            }
            traceReportingService.report(routed, parsed, paramFingerprint, params, executionMode, true, false,
                    response.getDuration(), null);
            return response;
        } catch (Exception ex) {
            long durationMs = elapsedMs(startedAt);
            SqlResponseStubDto response = queryResultMapper.exceptionResponse(routed.datasourceName, durationMs, ex.getMessage());
            traceReportingService.report(routed, parsed, paramFingerprint, params, executionMode, false, false,
                    durationMs, ex.getMessage());
            return response;
        }
    }

    private SqlResponseStubDto invalidRequestResponse(long startedAt,
                                                      RoutedSql routed,
                                                      SqlCommentParser.ParsedSql parsed,
                                                      List<StatementParameterDto> params,
                                                      String executionMode,
                                                      String message) {
        long durationMs = elapsedMs(startedAt);
        String cube = routed == null ? managedDataSourceRegistry.getDefaultName() : routed.datasourceName;
        SqlResponseStubDto response = queryResultMapper.exceptionResponse(cube, durationMs, message);
        if (routed != null) {
            traceReportingService.report(routed, parsed, null, params, executionMode, false, false,
                    durationMs, response.getExceptionMessage());
        }
        return response;
    }

    private String unsupportedRequestMessage(PreparedQueryRequestDto request) {
        StringJoiner fields = new StringJoiner(", ");
        for (String field : request.getUnsupportedPropertyNames()) {
            fields.add(field);
        }
        return "Only query requests are supported by engine-query; unsupported fields: " + fields.toString();
    }

    private SqlResponseStubDto executeAgainstDatasource(Connection connection,
                                                        RoutedSql routed,
                                                        List<StatementParameterDto> params,
                                                        String kylinPreparedSql,
                                                        long startedAt) throws Exception {
        if (params == null || params.isEmpty()) {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(routed.executionSql)) {
                return queryResultMapper.toResponse(rs, routed.datasourceName, elapsedMs(startedAt));
            }
        }

        if (kylinPreparedSql != null) {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(kylinPreparedSql)) {
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

    private boolean shouldLiteralizeKylinPrepared(RoutedSql routed, List<StatementParameterDto> params) {
        return routed != null
                && params != null
                && !params.isEmpty()
                && "kylin".equalsIgnoreCase(routed.datasourceType);
    }

    private String literalizeKylinPreparedSql(String sql, List<StatementParameterDto> params) {
        if (sql == null) {
            throw new IllegalArgumentException("Prepared parameter count " + params.size()
                    + " does not match placeholder count 0 for Kylin SQL");
        }

        StringBuilder rendered = new StringBuilder(sql.length() + (params.size() * 16));
        int paramIndex = 0;
        int placeholderCount = 0;
        boolean mismatch = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                rendered.append(ch);
                if (ch == '\n' || ch == '\r') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                rendered.append(ch);
                if (ch == '*' && next == '/') {
                    rendered.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }
            if (inSingleQuote) {
                rendered.append(ch);
                if (ch == '\'') {
                    if (next == '\'') {
                        rendered.append(next);
                        i++;
                    } else {
                        inSingleQuote = false;
                    }
                }
                continue;
            }
            if (inDoubleQuote) {
                rendered.append(ch);
                if (ch == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }
            if (inBacktick) {
                rendered.append(ch);
                if (ch == '`') {
                    inBacktick = false;
                }
                continue;
            }

            if (ch == '-' && next == '-') {
                rendered.append(ch).append(next);
                i++;
                inLineComment = true;
                continue;
            }
            if (ch == '/' && next == '*') {
                rendered.append(ch).append(next);
                i++;
                inBlockComment = true;
                continue;
            }
            if (ch == '\'') {
                rendered.append(ch);
                inSingleQuote = true;
                continue;
            }
            if (ch == '"') {
                rendered.append(ch);
                inDoubleQuote = true;
                continue;
            }
            if (ch == '`') {
                rendered.append(ch);
                inBacktick = true;
                continue;
            }
            if (ch == '?') {
                placeholderCount++;
                if (paramIndex < params.size()) {
                    rendered.append(renderSqlLiteral(convertValue(params.get(paramIndex))));
                    paramIndex++;
                } else {
                    mismatch = true;
                    rendered.append(ch);
                }
                continue;
            }

            rendered.append(ch);
        }

        if (mismatch || paramIndex != params.size()) {
            throw new IllegalArgumentException("Prepared parameter count " + params.size()
                    + " does not match placeholder count " + placeholderCount + " for Kylin SQL");
        }

        return rendered.toString();
    }

    private String renderSqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? "TRUE" : "FALSE";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Date || value instanceof Time || value instanceof Timestamp || value instanceof String) {
            return "'" + escapeSqlLiteral(String.valueOf(value)) + "'";
        }
        return "'" + escapeSqlLiteral(String.valueOf(value)) + "'";
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
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
