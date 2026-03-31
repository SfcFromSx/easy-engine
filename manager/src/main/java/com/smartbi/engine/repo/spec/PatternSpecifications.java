package com.smartbi.engine.repo.spec;

import com.smartbi.engine.domain.SqlPatternStats;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import java.util.Locale;

public final class PatternSpecifications {

    private PatternSpecifications() {
    }

    public static Specification<SqlPatternStats> withFilters(String fingerprint,
                                                             String sqlKeyword,
                                                             Long minExecutionCount) {
        return Specification.where(hasFingerprint(fingerprint))
                .and(hasSqlKeyword(sqlKeyword))
                .and(hasMinExecutionCount(minExecutionCount));
    }

    private static Specification<SqlPatternStats> hasFingerprint(String fingerprint) {
        if (!StringUtils.hasText(fingerprint)) {
            return null;
        }
        String normalized = fingerprint.trim();
        return (root, query, builder) -> builder.equal(root.get("sqlFingerprint"), normalized);
    }

    private static Specification<SqlPatternStats> hasSqlKeyword(String sqlKeyword) {
        if (!StringUtils.hasText(sqlKeyword)) {
            return null;
        }
        String normalized = "%" + sqlKeyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.like(lower(builder, root, "cleanSqlSample"), normalized);
    }

    private static Specification<SqlPatternStats> hasMinExecutionCount(Long minExecutionCount) {
        if (minExecutionCount == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("executionCount"), minExecutionCount);
    }

    private static Expression<String> lower(CriteriaBuilder builder, Root<SqlPatternStats> root, String field) {
        return builder.lower(builder.coalesce(root.get(field), ""));
    }
}
