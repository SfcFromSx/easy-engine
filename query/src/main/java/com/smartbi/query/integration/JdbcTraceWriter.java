package com.smartbi.query.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.query.domain.ParseStatus;
import com.smartbi.query.domain.SqlExecutionRecord;
import com.smartbi.query.domain.SqlPatternStats;
import com.smartbi.query.repo.SqlExecutionRecordRepository;
import com.smartbi.query.repo.SqlPatternStatsRepository;
import com.smartbi.query.trace.FingerprintUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Component
public class JdbcTraceWriter implements TraceWriter {

    private final ObjectMapper objectMapper;
    private final SqlExecutionRecordRepository recordRepository;
    private final SqlPatternStatsRepository patternStatsRepository;

    public JdbcTraceWriter(ObjectMapper objectMapper,
                           SqlExecutionRecordRepository recordRepository,
                           SqlPatternStatsRepository patternStatsRepository) {
        this.objectMapper = objectMapper;
        this.recordRepository = recordRepository;
        this.patternStatsRepository = patternStatsRepository;
    }

    @Override
    @Transactional
    public void publish(String payload) throws RuntimeException {
        if (!StringUtils.hasText(payload)) {
            return;
        }

        SqlExecutionRecord row = new SqlExecutionRecord();
        row.setRawPayload(payload);
        row.setReceivedAt(Instant.now());

        try {
            JsonNode root = objectMapper.readTree(payload);
            fillRecord(root, row);
        } catch (Exception ex) {
            row.setParseStatus(ParseStatus.ERROR);
            row.setParseError(ex.getMessage());
            recordRepository.save(row);
            return;
        }

        String sqlForFingerprint = StringUtils.hasText(row.getCleanSql()) ? row.getCleanSql() : row.getOriginalSql();
        if (StringUtils.hasText(sqlForFingerprint)) {
            row.setSqlFingerprint(FingerprintUtil.sha256Hex(FingerprintUtil.normalizeForFingerprint(sqlForFingerprint)));
            row.setParseStatus(ParseStatus.OK);
        } else {
            row.setParseStatus(ParseStatus.SKIPPED);
        }

        recordRepository.save(row);
        upsertPatternStats(row, sqlForFingerprint);
    }

    private void fillRecord(JsonNode root, SqlExecutionRecord row) {
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
        row.setCacheKey(text(root, "cacheKey"));
        if (root.has("durationMs") && !root.get("durationMs").isNull()) {
            row.setDurationMs(root.get("durationMs").asLong());
        }
        row.setErrorMessage(text(root, "errorMessage"));
    }

    private void upsertPatternStats(SqlExecutionRecord row, String sampleSql) {
        if (!StringUtils.hasText(row.getSqlFingerprint())) {
            return;
        }

        Instant now = Instant.now();
        SqlPatternStats stats = patternStatsRepository.findBySqlFingerprint(row.getSqlFingerprint()).orElse(null);
        if (stats == null) {
            stats = new SqlPatternStats();
            stats.setSqlFingerprint(row.getSqlFingerprint());
            stats.setCleanSqlSample(truncate(sampleSql, 4000));
            stats.setExecutionCount(1);
            stats.setLastSeenAt(now);
            stats.setAvgDurationMs(row.getDurationMs() == null ? null : row.getDurationMs().doubleValue());
            stats.setSignatureJson(row.getSignatureJson());
            patternStatsRepository.save(stats);
            return;
        }

        long nextCount = stats.getExecutionCount() + 1;
        stats.setExecutionCount(nextCount);
        stats.setLastSeenAt(now);
        if (!StringUtils.hasText(stats.getCleanSqlSample()) && StringUtils.hasText(sampleSql)) {
            stats.setCleanSqlSample(truncate(sampleSql, 4000));
        }
        if (row.getDurationMs() != null) {
            double previousAverage = stats.getAvgDurationMs() == null ? 0D : stats.getAvgDurationMs();
            stats.setAvgDurationMs((previousAverage * (nextCount - 1) + row.getDurationMs()) / nextCount);
        }
        if (StringUtils.hasText(row.getSignatureJson())) {
            stats.setSignatureJson(row.getSignatureJson());
        }
        patternStatsRepository.save(stats);
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : node.asText(null);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
