package com.smartbi.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.integration.TraceQueuePublisher;
import com.smartbi.query.parsing.SqlCommentParser;
import com.smartbi.query.route.RoutedSql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
}
