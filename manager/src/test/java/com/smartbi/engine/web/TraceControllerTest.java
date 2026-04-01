package com.smartbi.engine.web;

import com.smartbi.engine.domain.ParseStatus;
import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.web.dto.TraceListItemDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceControllerTest {

    private final SqlExecutionRecordRepository recordRepository = Mockito.mock(SqlExecutionRecordRepository.class);
    private final TraceController controller = new TraceController(recordRepository);

    @Test
    // Covers TraceController#page with fingerprint filtering and TraceController#resolveSourceFlag seed branch.
    void pageFiltersByFingerprintAndMapsToLightweightDto() {
        SqlExecutionRecord row = new SqlExecutionRecord();
        row.setReceivedAt(Instant.parse("2026-03-29T11:40:21Z"));
        row.setDatasourceName("learn_kylin");
        row.setDatasourceType("KYLIN");
        row.setDurationMs(120L);
        row.setCacheHit(Boolean.TRUE);
        row.setParseStatus(ParseStatus.OK);
        row.setSqlFingerprint("fingerprint-1");
        row.setOriginalSql("SELECT count(*) FROM KYLIN_SALES");
        row.setParameterPayload("[{\"position\":1,\"className\":\"java.lang.Long\",\"value\":\"42\"}]");
        row.setExecutionMode("PREPARED_STATEMENT");

        when(recordRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1));

        Page<TraceListItemDto> page = controller.page(0, 20, "fingerprint-1", null, null, null, null, null);

        verify(recordRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "receivedAt"))));
        assertEquals(1, page.getTotalElements());
        assertEquals("learn_kylin", page.getContent().get(0).getDatasourceName());
        assertEquals("SELECT count(*) FROM KYLIN_SALES", page.getContent().get(0).getOriginalSql());
        assertEquals("[{\"position\":1,\"className\":\"java.lang.Long\",\"value\":\"42\"}]",
                page.getContent().get(0).getParameterPayload());
        assertEquals("PREPARED_STATEMENT", page.getContent().get(0).getExecutionMode());
        assertEquals(ParseStatus.OK, page.getContent().get(0).getParseStatus());
        assertEquals("SEED", page.getContent().get(0).getSourceFlag());
    }

    @Test
    // Covers TraceController#page default listing and TraceController#resolveSourceFlag SELF/JDBC branches.
    void pageUsesDefaultListingWhenFingerprintIsBlank() {
        SqlExecutionRecord selfRow = new SqlExecutionRecord();
        ReflectionTestUtils.setField(selfRow, "id", 1L);
        selfRow.setReceivedAt(Instant.parse("2026-03-29T11:40:21Z"));
        selfRow.setDatasourceName("default");
        selfRow.setRawPayload("{\"sql\":\"select 1\"}");

        SqlExecutionRecord jdbcRow = new SqlExecutionRecord();
        ReflectionTestUtils.setField(jdbcRow, "id", 2L);
        jdbcRow.setReceivedAt(Instant.parse("2026-03-29T11:41:21Z"));
        jdbcRow.setDatasourceName("analytics");
        jdbcRow.setRawPayload("{\"sql\":\"select 2\"}");

        when(recordRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(selfRow, jdbcRow), PageRequest.of(0, 5), 2));

        Page<TraceListItemDto> page = controller.page(0, 5, "  ", null, null, null, null, null);

        verify(recordRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 5, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "receivedAt"))));
        assertEquals("SELF", page.getContent().get(0).getSourceFlag());
        assertEquals("JDBC", page.getContent().get(1).getSourceFlag());
    }
}
