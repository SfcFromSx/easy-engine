package com.smartbi.engine.web;

import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.ParseStatus;
import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.repo.AccelerationTableRepository;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import com.smartbi.engine.web.dto.StatsSummaryDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

class StatsControllerTest {

    private final SqlExecutionRecordRepository recordRepository = Mockito.mock(SqlExecutionRecordRepository.class);
    private final SqlPatternStatsRepository patternStatsRepository = Mockito.mock(SqlPatternStatsRepository.class);
    private final AccelerationTableRepository accelerationTableRepository = Mockito.mock(AccelerationTableRepository.class);

    private final StatsController controller = new StatsController(
            recordRepository,
            patternStatsRepository,
            accelerationTableRepository
    );

    @Test
    void summaryIncludesAccelerationAndCacheMetrics() {
        SqlExecutionRecord lastTrace = new SqlExecutionRecord();
        Instant lastSeenAt = Instant.parse("2026-03-29T11:40:21Z");
        lastTrace.setReceivedAt(lastSeenAt);

        when(recordRepository.count()).thenReturn(323L);
        when(recordRepository.countByParseStatus(ParseStatus.OK)).thenReturn(318L);
        when(recordRepository.countByParseStatus(ParseStatus.ERROR)).thenReturn(2L);
        when(recordRepository.countByCacheHitTrue()).thenReturn(120L);
        when(recordRepository.findTopByOrderByReceivedAtDesc()).thenReturn(lastTrace);
        when(patternStatsRepository.count()).thenReturn(30L);
        when(accelerationTableRepository.countByStatus(AccelerationStatus.ACTIVE)).thenReturn(3L);
        when(accelerationTableRepository.countByStatus(AccelerationStatus.DRAFT)).thenReturn(1L);

        StatsSummaryDto dto = controller.summary();

        assertEquals(323L, dto.getTotalTraces());
        assertEquals(318L, dto.getParseOk());
        assertEquals(2L, dto.getParseError());
        assertEquals(30L, dto.getPatternCount());
        assertEquals(3L, dto.getActiveAccelerationCount());
        assertEquals(1L, dto.getDraftAccelerationCount());
        assertEquals(120L, dto.getCacheHitCount());
        assertSame(lastSeenAt, dto.getLastTraceAt());
    }
}
