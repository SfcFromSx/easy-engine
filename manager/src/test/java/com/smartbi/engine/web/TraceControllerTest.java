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

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceControllerTest {

    private final SqlExecutionRecordRepository recordRepository = Mockito.mock(SqlExecutionRecordRepository.class);
    private final TraceController controller = new TraceController(recordRepository);

    @Test
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
        row.setExecutionMode("PREPARED_STATEMENT");

        when(recordRepository.findAllBySqlFingerprintOrderByReceivedAtDesc(Mockito.eq("fingerprint-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1));

        Page<TraceListItemDto> page = controller.page(0, 20, "fingerprint-1");

        verify(recordRepository).findAllBySqlFingerprintOrderByReceivedAtDesc(Mockito.eq("fingerprint-1"), any(PageRequest.class));
        assertEquals(1, page.getTotalElements());
        assertEquals("learn_kylin", page.getContent().get(0).getDatasourceName());
        assertEquals("SELECT count(*) FROM KYLIN_SALES", page.getContent().get(0).getOriginalSql());
        assertEquals("PREPARED_STATEMENT", page.getContent().get(0).getExecutionMode());
        assertEquals(ParseStatus.OK, page.getContent().get(0).getParseStatus());
        assertEquals("SEED", page.getContent().get(0).getSourceFlag());
    }
}
