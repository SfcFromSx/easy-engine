package com.smartbi.query.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.api.dto.StatementParameterDto;
import com.smartbi.query.config.QueryProperties;
import com.smartbi.query.integration.TraceWriter;
import com.smartbi.query.parsing.SqlCommentParser;
import com.smartbi.query.route.RoutedSql;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceReportingServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // Covers TraceReportingService#report trace-disabled early-return branch.
    @Test
    void shouldSkipPublishingWhenTraceReportingIsDisabled() {
        QueryProperties properties = new QueryProperties();
        properties.getTrace().setEnabled(false);
        CollectingTraceWriter writer = new CollectingTraceWriter();
        TraceReportingService service = new TraceReportingService(properties, writer, new ObjectMapper());

        service.report(routedSql(), parsedSql(), null, null, "STATEMENT", true, false, 5L, null);

        assertTrue(writer.payloads.isEmpty());
    }

    // Covers TraceReportingService#report successful publish branch.
    @Test
    void shouldPublishSerializedTracePayloads() throws Exception {
        CollectingTraceWriter writer = new CollectingTraceWriter();
        TraceReportingService service = new TraceReportingService(new QueryProperties(), writer, new ObjectMapper());

        service.report(routedSql(), parsedSql(), "fp", null, "STATEMENT", true, false, 8L, null);

        assertEquals(1, writer.payloads.size());
        JsonNode payload = JSON.readTree(writer.payloads.get(0));
        assertEquals("default", payload.path("datasourceName").asText());
        assertEquals("STATEMENT", payload.path("executionMode").asText());
        assertTrue(payload.path("parameterPayload").isMissingNode() || payload.path("parameterPayload").isNull());
    }

    // Covers TraceReportingService#report publish-failure swallowing branch.
    @Test
    void shouldSwallowTraceWriterFailures() {
        TraceWriter writer = new TraceWriter() {
            @Override
            public void publish(String payload) {
                throw new RuntimeException("boom");
            }
        };
        TraceReportingService service = new TraceReportingService(new QueryProperties(), writer, new ObjectMapper());

        assertDoesNotThrow(() -> service.report(routedSql(), parsedSql(), "fp", null, "STATEMENT", true, false, 8L, null));
    }

    // Covers TraceReportingService#resolveParameterPayload happy-path for failed prepared execution.
    @Test
    void shouldIncludeReadableParameterPayloadOnlyForFailedPreparedExecutions() throws Exception {
        CollectingTraceWriter writer = new CollectingTraceWriter();
        TraceReportingService service = new TraceReportingService(new QueryProperties(), writer, new ObjectMapper());
        List<StatementParameterDto> params = Collections.singletonList(param("java.lang.Integer", "1"));

        service.report(routedSql(), parsedSql(), "fp", params, "PREPARED_STATEMENT", false, false, 9L, "boom");

        JsonNode payload = JSON.readTree(writer.payloads.get(0));
        assertEquals("[{\"position\":1,\"className\":\"java.lang.Integer\",\"value\":\"1\"}]",
                payload.path("parameterPayload").asText());
    }

    // Covers TraceReportingService#resolveParameterPayload null-return branches for success, statement, and empty params.
    @Test
    void shouldOmitParameterPayloadWhenItIsNotNeeded() throws Exception {
        CollectingTraceWriter writer = new CollectingTraceWriter();
        TraceReportingService service = new TraceReportingService(new QueryProperties(), writer, new ObjectMapper());

        service.report(routedSql(), parsedSql(), "fp", Collections.singletonList(param("java.lang.Integer", "1")),
                "PREPARED_STATEMENT", true, false, 7L, null);
        service.report(routedSql(), parsedSql(), "fp", Collections.singletonList(param("java.lang.Integer", "1")),
                "STATEMENT", false, false, 7L, "boom");
        service.report(routedSql(), parsedSql(), "fp", Collections.<StatementParameterDto>emptyList(),
                "PREPARED_STATEMENT", false, false, 7L, "boom");

        assertNull(JSON.readTree(writer.payloads.get(0)).path("parameterPayload").textValue());
        assertNull(JSON.readTree(writer.payloads.get(1)).path("parameterPayload").textValue());
        assertNull(JSON.readTree(writer.payloads.get(2)).path("parameterPayload").textValue());
    }

    // Covers TraceReportingService#resolveParameterPayload serialization-failure fallback branch.
    @Test
    void shouldDropParameterPayloadWhenSerializationFails() throws Exception {
        CollectingTraceWriter writer = new CollectingTraceWriter();
        TraceReportingService service = new TraceReportingService(new QueryProperties(), writer, new ListFailingObjectMapper());

        service.report(routedSql(), parsedSql(), "fp", Collections.singletonList(param("java.lang.Integer", "1")),
                "PREPARED_STATEMENT", false, false, 7L, "boom");

        JsonNode payload = JSON.readTree(writer.payloads.get(0));
        assertTrue(payload.path("parameterPayload").isMissingNode() || payload.path("parameterPayload").isNull());
    }

    private static RoutedSql routedSql() {
        return new RoutedSql("default", "h2", "SELECT * FROM SALES", "SELECT * FROM SALES");
    }

    private static SqlCommentParser.ParsedSql parsedSql() {
        return SqlCommentParser.parse("/* YH_QUERYID=q1 */ SELECT * FROM SALES");
    }

    private static StatementParameterDto param(String className, String value) {
        StatementParameterDto dto = new StatementParameterDto();
        dto.setClassName(className);
        dto.setValue(value);
        return dto;
    }

    private static class CollectingTraceWriter implements TraceWriter {
        private final List<String> payloads = new ArrayList<String>();

        @Override
        public void publish(String payload) {
            payloads.add(payload);
        }
    }

    private static class ListFailingObjectMapper extends ObjectMapper {
        @Override
        public String writeValueAsString(Object value) throws JsonProcessingException {
            if (value instanceof List) {
                throw new JsonProcessingException("boom") {
                };
            }
            return super.writeValueAsString(value);
        }
    }
}
