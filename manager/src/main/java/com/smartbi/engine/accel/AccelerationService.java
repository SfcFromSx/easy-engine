package com.smartbi.engine.accel;

import com.smartbi.engine.domain.AccelerationSource;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import com.smartbi.engine.domain.SqlPatternStats;
import com.smartbi.engine.repo.AccelerationTableRepository;
import com.smartbi.engine.repo.SqlPatternStatsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class AccelerationService {

    private final AccelerationTableRepository accelerationTableRepository;
    private final SqlPatternStatsRepository sqlPatternStatsRepository;
    private final JdbcTemplate jdbcTemplate;

    public AccelerationService(AccelerationTableRepository accelerationTableRepository,
                               SqlPatternStatsRepository sqlPatternStatsRepository,
                               JdbcTemplate jdbcTemplate) {
        this.accelerationTableRepository = accelerationTableRepository;
        this.sqlPatternStatsRepository = sqlPatternStatsRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<AccelerationTable> list(Pageable pageable) {
        return accelerationTableRepository.findAllByOrderByUpdatedAtDesc(pageable);
    }

    @Transactional
    public AccelerationTable createManual(String name,
                                          String schemaName,
                                          String ddlText,
                                          String refreshSql,
                                          String cronExpr) {
        String normalizedName = requireText(name, "name is required");
        String normalizedSchema = normalizeSchemaName(schemaName);
        validateDuplicate(normalizedName, normalizedSchema, null);

        AccelerationTable t = new AccelerationTable();
        t.setName(normalizedName);
        t.setSchemaName(normalizedSchema);
        t.setDdlText(requireText(ddlText, "ddlText is required"));
        t.setRefreshSql(trimToNull(refreshSql));
        t.setCronExpr(trimToNull(cronExpr));
        t.setStatus(AccelerationStatus.DRAFT);
        t.setSource(AccelerationSource.MANUAL);
        return saveTable(t);
    }

    @Transactional
    public AccelerationTable updateManual(long id,
                                          String name,
                                          String schemaName,
                                          String ddlText,
                                          String refreshSql,
                                          String cronExpr) {
        AccelerationTable table = accelerationTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("acceleration table not found"));
        String normalizedName = requireText(name, "name is required");
        String normalizedSchema = normalizeSchemaName(schemaName);
        validateDuplicate(normalizedName, normalizedSchema, id);

        table.setName(normalizedName);
        table.setSchemaName(normalizedSchema);
        table.setDdlText(requireText(ddlText, "ddlText is required"));
        table.setRefreshSql(trimToNull(refreshSql));
        table.setCronExpr(trimToNull(cronExpr));
        return saveTable(table);
    }

    @Transactional
    public void delete(long id) {
        AccelerationTable table = accelerationTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("acceleration table not found"));
        accelerationTableRepository.delete(table);
    }

    @Transactional
    public AccelerationTable createFromPattern(long patternStatsId, String tableName, String schemaName) {
        SqlPatternStats stats = sqlPatternStatsRepository.findById(patternStatsId)
                .orElseThrow(() -> new IllegalArgumentException("pattern not found"));
        if (!StringUtils.hasText(stats.getCleanSqlSample())) {
            throw new IllegalArgumentException("pattern has no sample SQL");
        }
        String safeName = sanitizeTableName(tableName);
        String schema = normalizeSchemaName(schemaName);
        validateDuplicate(safeName, schema, null);
        String ddl = "-- Recommended physical rollup table (daily batch managed)\n"
                + "CREATE TABLE IF NOT EXISTS \"" + schema + "\".\"" + safeName + "\" AS\n"
                + stats.getCleanSqlSample() + "\n"
                + "WITH NO DATA;";
        String refresh = "INSERT INTO \"" + schema + "\".\"" + safeName + "\"\n"
                + stats.getCleanSqlSample() + ";";

        AccelerationTable t = new AccelerationTable();
        t.setName(safeName);
        t.setSchemaName(schema);
        t.setDdlText(ddl);
        t.setRefreshSql(refresh);
        t.setCronExpr("0 30 2 * * ?");
        t.setStatus(AccelerationStatus.DRAFT);
        t.setSource(AccelerationSource.RECOMMENDED);
        t.setRecommendationNote("From pattern " + stats.getSqlFingerprint() + " count=" + stats.getExecutionCount());
        return saveTable(t);
    }

    private static String sanitizeTableName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("table name required");
        }
        String n = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (n.isEmpty()) {
            throw new IllegalArgumentException("invalid table name");
        }
        return n;
    }

    public Page<SqlPatternStats> topPatterns(Pageable pageable, String fingerprint) {
        if (StringUtils.hasText(fingerprint)) {
            return sqlPatternStatsRepository.findAllBySqlFingerprintOrderByExecutionCountDesc(fingerprint.trim(), pageable);
        }
        return sqlPatternStatsRepository.findAllByOrderByExecutionCountDesc(pageable);
    }

    @Transactional
    public AccelerationTable updateStatus(long id, AccelerationStatus status) {
        AccelerationTable t = accelerationTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found"));

        // If transitioning to ACTIVE, try to actually create the table in DB
        if (status == AccelerationStatus.ACTIVE && t.getStatus() != AccelerationStatus.ACTIVE) {
            if (StringUtils.hasText(t.getSchemaName()) && !"public".equalsIgnoreCase(t.getSchemaName())) {
                jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + t.getSchemaName() + "\"");
            }
            if (StringUtils.hasText(t.getDdlText())) {
                jdbcTemplate.execute(t.getDdlText());
            }
            if (StringUtils.hasText(t.getRefreshSql())) {
                jdbcTemplate.execute(t.getRefreshSql());
            }
        }

        t.setStatus(status);
        return saveTable(t);
    }

    private void validateDuplicate(String name, String schemaName, Long currentId) {
        boolean exists = currentId == null
                ? accelerationTableRepository.existsByNameIgnoreCaseAndSchemaNameIgnoreCase(name, schemaName)
                : accelerationTableRepository.existsByNameIgnoreCaseAndSchemaNameIgnoreCaseAndIdNot(name, schemaName, currentId);
        if (exists) {
            throw new IllegalArgumentException("acceleration table already exists for schema/name");
        }
    }

    private AccelerationTable saveTable(AccelerationTable table) {
        try {
            return accelerationTableRepository.save(table);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("acceleration table already exists for schema/name");
        }
    }

    private static String normalizeSchemaName(String schemaName) {
        return StringUtils.hasText(schemaName) ? schemaName.trim() : "public";
    }

    private static String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
