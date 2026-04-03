package com.smartbi.query.trino;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.api.dto.PreparedQueryRequestDto;
import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.api.dto.StatementParameterDto;
import com.smartbi.query.service.QueryExecutionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TrinoStatementService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HEADER_PREPARED_STATEMENT = "X-Trino-Prepared-Statement";
    private static final String HEADER_ADDED_PREPARE = "X-Trino-Added-Prepare";
    private static final String HEADER_DEALLOCATED_PREPARE = "X-Trino-Deallocated-Prepare";
    private static final Pattern PREPARE_PATTERN = Pattern.compile(
            "(?is)^PREPARE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+FROM\\s+(.+?)\\s*;?\\s*$");
    private static final Pattern DEALLOCATE_PATTERN = Pattern.compile(
            "(?is)^DEALLOCATE\\s+PREPARE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;?\\s*$");
    private static final Pattern EXECUTE_PATTERN = Pattern.compile(
            "(?is)^EXECUTE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(?:USING\\s+(.+?))?\\s*;?\\s*$");
    private static final Pattern EXECUTE_IMMEDIATE_PATTERN = Pattern.compile(
            "(?is)^EXECUTE\\s+IMMEDIATE\\s+'((?:''|[^'])*)'\\s*(?:USING\\s+(.+?))?\\s*;?\\s*$");
    private static final Pattern CAST_NULL_PATTERN = Pattern.compile(
            "(?is)^CAST\\(NULL\\s+AS\\s+(.+?)\\)$");
    private static final Pattern TYPED_LITERAL_PATTERN = Pattern.compile(
            "(?is)^([A-Za-z ]+?)(?:\\([^)]*\\))?\\s+'((?:''|[^'])*)'$");
    private static final Pattern INTEGER_LITERAL_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern DECIMAL_LITERAL_PATTERN = Pattern.compile("^-?\\d+\\.\\d+$");

    private final QueryExecutionService queryExecutionService;

    public TrinoStatementService(QueryExecutionService queryExecutionService) {
        this.queryExecutionService = queryExecutionService;
    }

    public ResponseEntity<String> execute(String sql, HttpServletRequest request) {
        String queryId = newQueryId();
        try {
            if (!StringUtils.hasText(sql)) {
                return ResponseEntity.ok(toJson(errorResponse(queryId, request, "SQL statement body is required")));
            }

            String normalizedSql = trimTrailingSemicolon(sql);
            Matcher prepareMatcher = PREPARE_PATTERN.matcher(normalizedSql);
            if (prepareMatcher.matches()) {
                return prepareResponse(queryId, request, prepareMatcher.group(1), prepareMatcher.group(2));
            }

            Matcher deallocateMatcher = DEALLOCATE_PATTERN.matcher(normalizedSql);
            if (deallocateMatcher.matches()) {
                return deallocateResponse(queryId, request, deallocateMatcher.group(1));
            }

            Matcher executeImmediateMatcher = EXECUTE_IMMEDIATE_PATTERN.matcher(normalizedSql);
            if (executeImmediateMatcher.matches()) {
                String statementSql = unescapeSingleQuoted(executeImmediateMatcher.group(1));
                List<StatementParameterDto> params = parseUsingClause(executeImmediateMatcher.group(2));
                return ResponseEntity.ok(toJson(executeQuery(queryId, request, statementSql, params)));
            }

            Matcher executeMatcher = EXECUTE_PATTERN.matcher(normalizedSql);
            if (executeMatcher.matches()) {
                String statementName = executeMatcher.group(1);
                Map<String, String> preparedStatements = preparedStatements(request);
                String statementSql = preparedStatements.get(statementName);
                if (!StringUtils.hasText(statementSql)) {
                    return ResponseEntity.ok(toJson(errorResponse(queryId, request,
                            "Prepared statement is missing for EXECUTE " + statementName)));
                }
                List<StatementParameterDto> params = parseUsingClause(executeMatcher.group(2));
                return ResponseEntity.ok(toJson(executeQuery(queryId, request, statementSql, params)));
            }

            return ResponseEntity.ok(toJson(executeQuery(queryId, request, normalizedSql, Collections.emptyList())));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(toJson(errorResponse(queryId, request, ex.getMessage())));
        } catch (Exception ex) {
            return ResponseEntity.ok(toJson(errorResponse(queryId, request, ex.getMessage())));
        }
    }

    private ResponseEntity<String> prepareResponse(String queryId,
                                                   HttpServletRequest request,
                                                   String statementName,
                                                   String statementSql) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_ADDED_PREPARE, encodePreparedStatement(statementName, trimTrailingSemicolon(statementSql)));
        return ResponseEntity.ok().headers(headers).body(toJson(updateResponse(queryId, request, "PREPARE", 0L)));
    }

    private ResponseEntity<String> deallocateResponse(String queryId,
                                                      HttpServletRequest request,
                                                      String statementName) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_DEALLOCATED_PREPARE, urlEncode(statementName));
        return ResponseEntity.ok().headers(headers)
                .body(toJson(updateResponse(queryId, request, "DEALLOCATE PREPARE", 0L)));
    }

    private Map<String, Object> executeQuery(String queryId,
                                             HttpServletRequest request,
                                             String statementSql,
                                             List<StatementParameterDto> params) {
        PreparedQueryRequestDto queryRequest = new PreparedQueryRequestDto();
        queryRequest.setSql(statementSql);
        if (params != null && !params.isEmpty()) {
            queryRequest.setParams(params);
        }

        SqlResponseStubDto response = queryExecutionService.execute(queryRequest);
        if (response == null || response.getIsException()) {
            String message = response == null ? "Query execution returned no response" : response.getExceptionMessage();
            return errorResponse(queryId, request, message);
        }

        Map<String, Object> out = baseResponse(queryId, request);
        out.put("columns", toColumns(response.getColumnMetas()));
        out.put("data", toRows(response.getResults()));
        out.put("stats", finishedStats("FINISHED", response.getDuration(), response.getTotalScanCount()));
        return out;
    }

    private Map<String, Object> updateResponse(String queryId,
                                               HttpServletRequest request,
                                               String updateType,
                                               Long updateCount) {
        Map<String, Object> out = baseResponse(queryId, request);
        out.put("columns", Collections.singletonList(syntheticUpdateColumn()));
        out.put("data", Collections.emptyList());
        out.put("stats", finishedStats("FINISHED", 0L, 0L));
        out.put("updateType", updateType);
        out.put("updateCount", updateCount);
        return out;
    }

    private Map<String, Object> errorResponse(String queryId,
                                              HttpServletRequest request,
                                              String message) {
        String safeMessage = StringUtils.hasText(message) ? message : "Query execution failed";
        Map<String, Object> out = baseResponse(queryId, request);
        out.put("stats", finishedStats("FAILED", 0L, 0L));

        Map<String, Object> failureInfo = new LinkedHashMap<String, Object>();
        failureInfo.put("type", "java.sql.SQLException");
        failureInfo.put("message", safeMessage);
        failureInfo.put("cause", null);
        failureInfo.put("suppressed", Collections.emptyList());
        failureInfo.put("stack", Collections.emptyList());
        failureInfo.put("errorInfo", null);
        failureInfo.put("errorLocation", null);

        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("message", safeMessage);
        error.put("sqlState", "HY000");
        error.put("errorCode", Integer.valueOf(1));
        error.put("errorName", "GENERIC_INTERNAL_ERROR");
        error.put("errorType", "USER_ERROR");
        error.put("errorLocation", null);
        error.put("failureInfo", failureInfo);
        out.put("error", error);
        return out;
    }

    private Map<String, Object> baseResponse(String queryId, HttpServletRequest request) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", queryId);
        out.put("infoUri", statementUri(request, queryId));
        out.put("partialCancelUri", null);
        out.put("nextUri", null);
        out.put("columns", null);
        out.put("data", null);
        out.put("stats", null);
        out.put("error", null);
        out.put("warnings", Collections.emptyList());
        out.put("updateType", null);
        out.put("updateCount", null);
        return out;
    }

    private List<Map<String, Object>> toColumns(List<SqlResponseStubDto.ColumnMetaStubDto> metas) {
        if (metas == null || metas.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> columns = new ArrayList<Map<String, Object>>();
        for (SqlResponseStubDto.ColumnMetaStubDto meta : metas) {
            String trinoType = toTrinoType(meta.getColumnTypeName());
            Map<String, Object> typeSignature = new LinkedHashMap<String, Object>();
            typeSignature.put("rawType", trinoType);
            if ("varchar".equals(trinoType)) {
                Map<String, Object> argument = new LinkedHashMap<String, Object>();
                argument.put("kind", "LONG");
                argument.put("value", Long.valueOf(2147483647L));
                typeSignature.put("arguments", Collections.singletonList(argument));
            } else {
                typeSignature.put("arguments", Collections.emptyList());
            }

            Map<String, Object> column = new LinkedHashMap<String, Object>();
            column.put("name", StringUtils.hasText(meta.getLabel()) ? meta.getLabel() : meta.getName());
            column.put("type", trinoType);
            column.put("typeSignature", typeSignature);
            columns.add(column);
        }
        return columns;
    }

    private Map<String, Object> syntheticUpdateColumn() {
        Map<String, Object> typeSignature = new LinkedHashMap<String, Object>();
        typeSignature.put("rawType", "boolean");
        typeSignature.put("arguments", Collections.emptyList());

        Map<String, Object> column = new LinkedHashMap<String, Object>();
        column.put("name", "result");
        column.put("type", "boolean");
        column.put("typeSignature", typeSignature);
        return column;
    }

    private List<List<Object>> toRows(List<String[]> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<Object>> rows = new ArrayList<List<Object>>();
        for (String[] row : results) {
            List<Object> values = new ArrayList<Object>();
            if (row != null) {
                Collections.addAll(values, (Object[]) row);
            }
            rows.add(values);
        }
        return rows;
    }

    private Map<String, Object> finishedStats(String state, long elapsedMs, long processedRows) {
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("state", state);
        stats.put("queued", Boolean.FALSE);
        stats.put("scheduled", Boolean.TRUE);
        stats.put("progressPercentage", Double.valueOf("FAILED".equals(state) ? 0D : 100D));
        stats.put("runningPercentage", Double.valueOf(0D));
        stats.put("nodes", Integer.valueOf(1));
        stats.put("totalSplits", Integer.valueOf(1));
        stats.put("queuedSplits", Integer.valueOf(0));
        stats.put("runningSplits", Integer.valueOf(0));
        stats.put("completedSplits", Integer.valueOf("FAILED".equals(state) ? 0 : 1));
        stats.put("planningTimeMillis", Long.valueOf(0L));
        stats.put("analysisTimeMillis", Long.valueOf(0L));
        stats.put("cpuTimeMillis", Long.valueOf(elapsedMs));
        stats.put("wallTimeMillis", Long.valueOf(elapsedMs));
        stats.put("queuedTimeMillis", Long.valueOf(0L));
        stats.put("elapsedTimeMillis", Long.valueOf(elapsedMs));
        stats.put("finishingTimeMillis", Long.valueOf(0L));
        stats.put("physicalInputTimeMillis", Long.valueOf(0L));
        stats.put("processedRows", Long.valueOf(processedRows));
        stats.put("processedBytes", Long.valueOf(0L));
        stats.put("physicalInputBytes", Long.valueOf(0L));
        stats.put("physicalWrittenBytes", Long.valueOf(0L));
        stats.put("internalNetworkInputBytes", Long.valueOf(0L));
        stats.put("peakMemoryBytes", Long.valueOf(0L));
        stats.put("spilledBytes", Long.valueOf(0L));
        stats.put("rootStage", null);
        return stats;
    }

    private String toTrinoType(String jdbcTypeName) {
        String normalized = jdbcTypeName == null ? "" : jdbcTypeName.trim().toUpperCase(Locale.ROOT);
        if ("VARCHAR".equals(normalized) || "CHARACTER VARYING".equals(normalized) || "CHAR".equals(normalized)) {
            return "varchar";
        }
        if ("INTEGER".equals(normalized) || "INT".equals(normalized)) {
            return "integer";
        }
        if ("BIGINT".equals(normalized)) {
            return "bigint";
        }
        if ("SMALLINT".equals(normalized)) {
            return "smallint";
        }
        if ("DOUBLE".equals(normalized) || "DOUBLE PRECISION".equals(normalized)) {
            return "double";
        }
        if ("REAL".equals(normalized) || "FLOAT".equals(normalized)) {
            return "real";
        }
        if ("BOOLEAN".equals(normalized) || "BIT".equals(normalized)) {
            return "boolean";
        }
        if ("DATE".equals(normalized)) {
            return "date";
        }
        if ("TIME".equals(normalized)) {
            return "time";
        }
        if ("TIMESTAMP".equals(normalized)) {
            return "timestamp";
        }
        if ("DECIMAL".equals(normalized) || "NUMERIC".equals(normalized)) {
            return "decimal";
        }
        return "varchar";
    }

    private List<StatementParameterDto> parseUsingClause(String usingClause) {
        if (!StringUtils.hasText(usingClause)) {
            return Collections.emptyList();
        }
        List<String> literals = splitTopLevel(usingClause);
        List<StatementParameterDto> params = new ArrayList<StatementParameterDto>();
        for (String literal : literals) {
            params.add(parseLiteral(literal));
        }
        return params;
    }

    private List<String> splitTopLevel(String source) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        int parenDepth = 0;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '\'') {
                current.append(ch);
                if (inQuote && i + 1 < source.length() && source.charAt(i + 1) == '\'') {
                    current.append(source.charAt(i + 1));
                    i++;
                } else {
                    inQuote = !inQuote;
                }
                continue;
            }
            if (!inQuote) {
                if (ch == '(') {
                    parenDepth++;
                } else if (ch == ')' && parenDepth > 0) {
                    parenDepth--;
                } else if (ch == ',' && parenDepth == 0) {
                    tokens.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            tokens.add(current.toString().trim());
        }
        return tokens;
    }

    private StatementParameterDto parseLiteral(String literal) {
        String trimmed = literal == null ? "" : literal.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("EXECUTE USING contains an empty literal");
        }

        Matcher castNullMatcher = CAST_NULL_PATTERN.matcher(trimmed);
        if (castNullMatcher.matches()) {
            return parameter(javaClassForType(castNullMatcher.group(1)), null);
        }
        if ("NULL".equalsIgnoreCase(trimmed)) {
            return parameter(null, null);
        }
        if ("TRUE".equalsIgnoreCase(trimmed) || "FALSE".equalsIgnoreCase(trimmed)) {
            return parameter("java.lang.Boolean", trimmed.toLowerCase(Locale.ROOT));
        }

        Matcher typedLiteralMatcher = TYPED_LITERAL_PATTERN.matcher(trimmed);
        if (typedLiteralMatcher.matches()) {
            String normalizedType = normalizeType(typedLiteralMatcher.group(1));
            String value = unescapeSingleQuoted(typedLiteralMatcher.group(2));
            return parameter(javaClassForType(normalizedType), value);
        }

        if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
            return parameter("java.lang.String", unescapeSingleQuoted(trimmed.substring(1, trimmed.length() - 1)));
        }
        if (INTEGER_LITERAL_PATTERN.matcher(trimmed).matches()) {
            return parameter("java.lang.Integer", trimmed);
        }
        if (DECIMAL_LITERAL_PATTERN.matcher(trimmed).matches()) {
            return parameter("java.lang.Double", trimmed);
        }

        throw new IllegalArgumentException("Unsupported EXECUTE USING literal: " + trimmed);
    }

    private StatementParameterDto parameter(String className, String value) {
        StatementParameterDto dto = new StatementParameterDto();
        dto.setClassName(className);
        dto.setValue(value);
        return dto;
    }

    private String javaClassForType(String rawType) {
        String normalizedType = normalizeType(rawType);
        if ("INTEGER".equals(normalizedType)) {
            return "java.lang.Integer";
        }
        if ("BIGINT".equals(normalizedType)) {
            return "java.lang.Long";
        }
        if ("SMALLINT".equals(normalizedType) || "TINYINT".equals(normalizedType)) {
            return "java.lang.Short";
        }
        if ("REAL".equals(normalizedType) || "FLOAT".equals(normalizedType)) {
            return "java.lang.Float";
        }
        if ("DOUBLE".equals(normalizedType) || "DOUBLE PRECISION".equals(normalizedType)) {
            return "java.lang.Double";
        }
        if ("DECIMAL".equals(normalizedType) || "NUMERIC".equals(normalizedType)) {
            return "java.math.BigDecimal";
        }
        if ("BOOLEAN".equals(normalizedType)) {
            return "java.lang.Boolean";
        }
        if ("DATE".equals(normalizedType)) {
            return "java.sql.Date";
        }
        if ("TIME".equals(normalizedType)) {
            return "java.sql.Time";
        }
        if ("TIMESTAMP".equals(normalizedType)) {
            return "java.sql.Timestamp";
        }
        return "java.lang.String";
    }

    private String normalizeType(String rawType) {
        return rawType == null ? "" : rawType.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private Map<String, String> preparedStatements(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }
        Enumeration<String> headers = request.getHeaders(HEADER_PREPARED_STATEMENT);
        if (headers == null) {
            return Collections.emptyMap();
        }
        Map<String, String> statements = new LinkedHashMap<String, String>();
        while (headers.hasMoreElements()) {
            String value = headers.nextElement();
            int separator = value.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String name = urlDecode(value.substring(0, separator));
            String sql = urlDecode(value.substring(separator + 1));
            statements.put(name, sql);
        }
        return statements;
    }

    private String encodePreparedStatement(String name, String sql) {
        return urlEncode(name) + "=" + urlEncode(sql);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String unescapeSingleQuoted(String value) {
        return value == null ? null : value.replace("''", "'");
    }

    private String trimTrailingSemicolon(String sql) {
        if (sql == null) {
            return null;
        }
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            return trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private URI statementUri(HttpServletRequest request, String queryId) {
        if (request == null) {
            return null;
        }
        return URI.create("http://127.0.0.1:" + request.getLocalPort() + "/v1/statement/" + queryId);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return JSON.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize Trino response", ex);
        }
    }

    private String newQueryId() {
        return "engine_trino_" + UUID.randomUUID().toString().replace("-", "");
    }
}
