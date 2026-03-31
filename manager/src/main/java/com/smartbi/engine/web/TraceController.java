package com.smartbi.engine.web;

import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.repo.spec.TraceSpecifications;
import com.smartbi.engine.web.dto.TraceListItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/traces")
public class TraceController {

    private final SqlExecutionRecordRepository recordRepository;

    public TraceController(SqlExecutionRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @GetMapping
    public Page<TraceListItemDto> page(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String fingerprint,
                                       @RequestParam(required = false) String datasource,
                                       @RequestParam(required = false) String sourceFlag,
                                       @RequestParam(required = false) Boolean cacheHit,
                                       @RequestParam(required = false) String parseStatus,
                                       @RequestParam(required = false) String sqlKeyword) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<SqlExecutionRecord> traces = recordRepository.findAll(
                TraceSpecifications.withFilters(fingerprint, datasource, sourceFlag, cacheHit, parseStatus, sqlKeyword),
                request);
        return traces.map(this::toDto);
    }

    private TraceListItemDto toDto(SqlExecutionRecord row) {
        TraceListItemDto dto = new TraceListItemDto();
        dto.setId(row.getId());
        dto.setReceivedAt(row.getReceivedAt());
        dto.setDatasourceName(row.getDatasourceName());
        dto.setDatasourceType(row.getDatasourceType());
        dto.setDurationMs(row.getDurationMs());
        dto.setCacheHit(row.getCacheHit());
        dto.setParseStatus(row.getParseStatus());
        dto.setSqlFingerprint(row.getSqlFingerprint());
        dto.setOriginalSql(row.getOriginalSql());
        dto.setParameterPayload(row.getParameterPayload());
        dto.setExecutionMode(row.getExecutionMode());
        dto.setParseError(row.getParseError());
        dto.setSourceFlag(resolveSourceFlag(row));
        return dto;
    }

    private String resolveSourceFlag(SqlExecutionRecord row) {
        if (row.getRawPayload() == null || row.getRawPayload().trim().isEmpty()) {
            return "SEED";
        }
        if ("default".equalsIgnoreCase(row.getDatasourceName())) {
            return "SELF";
        }
        return "JDBC";
    }
}
