package com.smartbi.engine.repo.spec;

import com.smartbi.engine.domain.AccelerationSource;
import com.smartbi.engine.domain.AccelerationStatus;
import com.smartbi.engine.domain.AccelerationTable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Locale;

public final class AccelerationSpecifications {

    private AccelerationSpecifications() {
    }

    public static Specification<AccelerationTable> withFilters(String keyword,
                                                               String status,
                                                               String schemaName,
                                                               String source) {
        return Specification.where(hasKeyword(keyword))
                .and(hasStatus(status))
                .and(hasSchemaName(schemaName))
                .and(hasSource(source));
    }

    private static Specification<AccelerationTable> hasKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalized = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(lower(builder, root, "name"), normalized),
                builder.like(lower(builder, root, "refreshSql"), normalized),
                builder.like(lower(builder, root, "ddlText"), normalized)
        );
    }

    private static Specification<AccelerationTable> hasStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        AccelerationStatus normalized = AccelerationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        return (root, query, builder) -> builder.equal(root.get("status"), normalized);
    }

    private static Specification<AccelerationTable> hasSchemaName(String schemaName) {
        if (!StringUtils.hasText(schemaName)) {
            return null;
        }
        String normalized = "%" + schemaName.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.like(lower(builder, root, "schemaName"), normalized);
    }

    private static Specification<AccelerationTable> hasSource(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        AccelerationSource normalized = AccelerationSource.valueOf(source.trim().toUpperCase(Locale.ROOT));
        return (root, query, builder) -> builder.equal(root.get("source"), normalized);
    }

    private static Expression<String> lower(CriteriaBuilder builder, Root<AccelerationTable> root, String field) {
        return builder.lower(builder.coalesce(root.get(field), ""));
    }
}
