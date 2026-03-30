package com.smartbi.engine.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.engine.domain.ParseStatus;
import com.smartbi.engine.domain.SqlExecutionRecord;
import com.smartbi.engine.domain.SqlPatternStats;
import com.smartbi.engine.parse.ParseOutcome;
import com.smartbi.engine.parse.QuerySignature;
import com.smartbi.engine.parse.SqlParseService;
import com.smartbi.engine.repo.SqlExecutionRecordRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class TraceIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TraceIngestionService.class);

    private final ObjectMapper objectMapper;
    private final SqlParseService sqlParseService;
    private final SqlExecutionRecordRepository recordRepository;
    private final SqlPatternStatsRepository patternStatsRepository;

    public TraceIngestionService(ObjectMapper objectMapper,
                                 SqlParseService sqlParseService,
                                 SqlExecutionRecordRepository recordRepository,
                                 SqlPatternStatsRepository patternStatsRepository) {
        this.objectMapper = objectMapper;
        this.sqlParseService = sqlParseService;
        this.recordRepository = recordRepository;
        this.patternStatsRepository = patternStatsRepository;
    }

    @Transactional
    public void ingestBatch(java.util.List<String> jsons) {
        if (jsons == null || jsons.isEmpty()) return;
        for (String json : jsons) {
            ingestJson(json);
        }
    }

    @Transactional
    public void ingestJson(String json) {
        if (!StringUtils.hasText(json)) return;
        
        SqlExecutionRecord row = new SqlExecutionRecord();
        row.setRawPayload(json);
        row.setReceivedAt(Instant.now());
        try {
            JsonNode root = objectMapper.readTree(json);
            parseAndFill(root, row);
            
            String sqlForParse = StringUtils.hasText(row.getCleanSql()) ? row.getCleanSql() : row.getOriginalSql();
            if (StringUtils.hasText(sqlForParse)) {
                String fp = FingerprintUtil.sha256Hex(FingerprintUtil.normalizeForFingerprint(sqlForParse));
                row.setSqlFingerprint(fp);

                ParseOutcome outcome = sqlParseService.analyze(sqlForParse);
                row.setParseStatus(outcome.getStatus());
                if (outcome.getStatus() == ParseStatus.OK && outcome.getSignature() != null) {
                    row.setSignatureJson(objectMapper.writeValueAsString(outcome.getSignature()));
                } else {
                    row.setParseError(outcome.getErrorMessage());
                }

                recordRepository.save(row);
                upsertPatternStats(fp, sqlForParse, row.getDurationMs(), row.getSignatureJson());
            } else {
                row.setParseStatus(ParseStatus.SKIPPED);
                recordRepository.save(row);
            }
        } catch (Exception e) {
            log.warn("Trace ingestion failed: {}", e.getMessage());
            row.setParseStatus(ParseStatus.ERROR);
            row.setParseError(e.getMessage());
            recordRepository.save(row);
        }
    }

    private void parseAndFill(JsonNode root, SqlExecutionRecord row) {
        row.setDatasourceName(text(root, "datasourceName"));
        row.setDatasourceType(text(root, "datasourceType"));
        row.setOriginalSql(text(root, "originalSql"));
        row.setCleanSql(text(root, "cleanSql"));
        row.setParamFingerprint(text(root, "paramFingerprint"));
        row.setParameterPayload(text(root, "parameterPayload"));
        row.setExecutionMode(text(root, "executionMode"));
        if (root.has("success") && !root.get("success").isNull()) {
            row.setSuccess(root.get("success").asBoolean());
        }
        if (root.has("cacheHit") && !root.get("cacheHit").isNull()) {
            row.setCacheHit(root.get("cacheHit").asBoolean());
        }
        if (root.has("durationMs") && !root.get("durationMs").isNull()) {
            row.setDurationMs(root.get("durationMs").asLong());
        }
        row.setErrorMessage(text(root, "errorMessage"));
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        return n == null || n.isNull() ? null : n.asText(null);
    }

    private void upsertPatternStats(String fingerprint, String sampleSql, Long durationMs, String signatureJson) {
        if (fingerprint == null) {
            return;
        }
        SqlPatternStats stats = patternStatsRepository.findBySqlFingerprint(fingerprint).orElse(null);
        if (stats == null) {
            stats = new SqlPatternStats();
            stats.setSqlFingerprint(fingerprint);
            stats.setCleanSqlSample(truncate(sampleSql, 4000));
            stats.setExecutionCount(1);
            stats.setLastSeenAt(Instant.now());
            stats.setAvgDurationMs(durationMs == null ? null : durationMs.doubleValue());
            stats.setSignatureJson(signatureJson);
            patternStatsRepository.save(stats);
            return;
        }
        stats.setExecutionCount(stats.getExecutionCount() + 1);
        stats.setLastSeenAt(Instant.now());
        if (!StringUtils.hasText(stats.getCleanSqlSample()) && StringUtils.hasText(sampleSql)) {
            stats.setCleanSqlSample(truncate(sampleSql, 4000));
        }
        if (durationMs != null) {
            double prev = stats.getAvgDurationMs() == null ? 0D : stats.getAvgDurationMs();
            long n = stats.getExecutionCount();
            stats.setAvgDurationMs((prev * (n - 1) + durationMs) / n);
        }
        if (StringUtils.hasText(signatureJson)) {
            stats.setSignatureJson(signatureJson);
        }
        patternStatsRepository.save(stats);
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
