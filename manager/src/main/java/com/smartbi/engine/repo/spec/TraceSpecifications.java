package com.smartbi.engine.repo.spec;

import com.smartbi.engine.domain.ParseStatus;
import com.smartbi.engine.domain.SqlExecutionRecord;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Locale;

public final class TraceSpecifications {

    private TraceSpecifications() {
    }

    public static Specification<SqlExecutionRecord> withFilters(String fingerprint,
                                                                String datasource,
                                                                String sourceFlag,
                                                                Boolean cacheHit,
                                                                String parseStatus,
                                                                String sqlKeyword) {
        return Specification.where(hasFingerprint(fingerprint))
                .and(hasDatasource(datasource))
                .and(hasSourceFlag(sourceFlag))
                .and(hasCacheHit(cacheHit))
                .and(hasParseStatus(parseStatus))
                .and(hasSqlKeyword(sqlKeyword));
    }

    private static Specification<SqlExecutionRecord> hasFingerprint(String fingerprint) {
        if (!StringUtils.hasText(fingerprint)) {
            return null;
        }
        String normalized = fingerprint.trim();
        return (root, query, builder) -> builder.equal(root.get("sqlFingerprint"), normalized);
    }

    private static Specification<SqlExecutionRecord> hasDatasource(String datasource) {
        if (!StringUtils.hasText(datasource)) {
            return null;
        }
        String normalized = likeValue(datasource);
        return (root, query, builder) -> builder.like(lower(builder, root, "datasourceName"), normalized);
    }

    private static Specification<SqlExecutionRecord> hasSourceFlag(String sourceFlag) {
        if (!StringUtils.hasText(sourceFlag)) {
            return null;
        }
        String normalized = sourceFlag.trim().toUpperCase(Locale.ROOT);
        return (root, query, builder) -> {
            Predicate rawPayloadBlank = isBlank(root, builder, "rawPayload");
            Predicate rawPayloadPresent = isNotBlank(root, builder, "rawPayload");
            Predicate defaultDatasource = builder.equal(lower(builder, root, "datasourceName"), "default");
            switch (normalized) {
                case "SEED":
                    return rawPayloadBlank;
                case "SELF":
                    return builder.and(rawPayloadPresent, defaultDatasource);
                case "JDBC":
                    return builder.and(rawPayloadPresent, builder.not(defaultDatasource));
                default:
                    throw new IllegalArgumentException("unknown sourceFlag: " + normalized);
            }
        };
    }

    private static Specification<SqlExecutionRecord> hasCacheHit(Boolean cacheHit) {
        if (cacheHit == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("cacheHit"), cacheHit);
    }

    private static Specification<SqlExecutionRecord> hasParseStatus(String parseStatus) {
        if (!StringUtils.hasText(parseStatus)) {
            return null;
        }
        ParseStatus normalized = ParseStatus.valueOf(parseStatus.trim().toUpperCase(Locale.ROOT));
        return (root, query, builder) -> builder.equal(root.get("parseStatus"), normalized);
    }

    private static Specification<SqlExecutionRecord> hasSqlKeyword(String sqlKeyword) {
        if (!StringUtils.hasText(sqlKeyword)) {
            return null;
        }
        String normalized = likeValue(sqlKeyword);
        return (root, query, builder) -> builder.like(lower(builder, root, "originalSql"), normalized);
    }

    private static String likeValue(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static Expression<String> lower(CriteriaBuilder builder, Root<SqlExecutionRecord> root, String field) {
        return builder.lower(builder.coalesce(root.get(field), ""));
    }

    private static Predicate isBlank(Root<SqlExecutionRecord> root, CriteriaBuilder builder, String field) {
        return builder.equal(builder.trim(builder.coalesce(root.get(field), "")), "");
    }

    private static Predicate isNotBlank(Root<SqlExecutionRecord> root, CriteriaBuilder builder, String field) {
        return builder.notEqual(builder.trim(builder.coalesce(root.get(field), "")), "");
    }
}
