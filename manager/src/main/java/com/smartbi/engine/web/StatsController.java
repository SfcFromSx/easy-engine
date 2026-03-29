package com.smartbi.engine.web;

import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.ParseStatus;
import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.repo.AccelerationTableRepository;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import com.smartbi.engine.web.dto.StatsSummaryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final SqlExecutionRecordRepository recordRepository;
    private final SqlPatternStatsRepository patternStatsRepository;
    private final AccelerationTableRepository accelerationTableRepository;

    public StatsController(SqlExecutionRecordRepository recordRepository,
                           SqlPatternStatsRepository patternStatsRepository,
                           AccelerationTableRepository accelerationTableRepository) {
        this.recordRepository = recordRepository;
        this.patternStatsRepository = patternStatsRepository;
        this.accelerationTableRepository = accelerationTableRepository;
    }

    @GetMapping("/summary")
    public StatsSummaryDto summary() {
        StatsSummaryDto dto = new StatsSummaryDto();
        dto.setTotalTraces(recordRepository.count());
        dto.setParseOk(recordRepository.countByParseStatus(ParseStatus.OK));
        dto.setParseError(recordRepository.countByParseStatus(ParseStatus.ERROR));
        dto.setPatternCount(patternStatsRepository.count());
        dto.setActiveAccelerationCount(accelerationTableRepository.countByStatus(AccelerationStatus.ACTIVE));
        dto.setDraftAccelerationCount(accelerationTableRepository.countByStatus(AccelerationStatus.DRAFT));
        dto.setCacheHitCount(recordRepository.countByCacheHitTrue());
        SqlExecutionRecord lastTrace = recordRepository.findTopByOrderByReceivedAtDesc();
        dto.setLastTraceAt(lastTrace == null ? null : lastTrace.getReceivedAt());
        return dto;
    }
}
