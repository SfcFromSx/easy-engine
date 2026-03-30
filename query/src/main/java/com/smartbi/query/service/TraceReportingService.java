package com.smartbi.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.api.dto.StatementParameterDto;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.integration.TraceQueuePublisher;
import com.smartbi.query.parsing.SqlCommentParser;
import com.smartbi.query.route.RoutedSql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TraceReportingService {

    private static final Logger log = LoggerFactory.getLogger(TraceReportingService.class);

    private final QueryProperties queryProperties;
    private final TraceQueuePublisher traceQueuePublisher;
    private final ObjectMapper objectMapper;

    public TraceReportingService(QueryProperties queryProperties,
                                 TraceQueuePublisher traceQueuePublisher,
                                 ObjectMapper objectMapper) {
        this.queryProperties = queryProperties;
        this.traceQueuePublisher = traceQueuePublisher;
        this.objectMapper = objectMapper;
    }

    public void report(RoutedSql routed,
                       SqlCommentParser.ParsedSql parsed,
                       String paramFingerprint,
                       List<StatementParameterDto> params,
                       String executionMode,
                       boolean success,
                       Boolean cacheHit,
                       long durationMs,
                       String errorMessage) {
        if (!queryProperties.getTrace().isEnabled()) {
            return;
        }
        TracePayload payload = new TracePayload(
                routed.datasourceName,
                routed.datasourceType,
                routed.originalSql,
                parsed.cleanSql,
                paramFingerprint,
                resolveParameterPayload(params, executionMode, success),
                executionMode,
                success,
                cacheHit,
                durationMs,
                errorMessage,
                parsed.metadata
        );
        try {
            String json = objectMapper.writeValueAsString(payload);
            traceQueuePublisher.publish(queryProperties.getTrace().getListKey(), json);
        } catch (Exception ex) {
            log.warn("Trace payload serialization failed: {}", ex.getMessage());
        }
    }

    private String resolveParameterPayload(List<StatementParameterDto> params, String executionMode, boolean success) {
        if (success || !"PREPARED_STATEMENT".equals(executionMode) || params == null || params.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>(params.size());
        for (int i = 0; i < params.size(); i++) {
            StatementParameterDto param = params.get(i);
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("position", i + 1);
            entry.put("className", param == null ? null : param.getClassName());
            entry.put("value", param == null ? null : param.getValue());
            payload.add(entry);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Trace parameter payload serialization failed: {}", ex.getMessage());
            return null;
        }
    }
}
