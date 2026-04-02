package com.smartbi.benchmark.repo;

import com.smartbi.benchmark.domain.SqlTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SqlTemplateRepository extends JpaRepository<SqlTemplate, Long> {

    @Query("SELECT t FROM SqlTemplate t WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.sqlText) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.executionMode, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.paramJson, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.sourceFilename, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:executionMode IS NULL OR t.executionMode = :executionMode) AND " +
           "(:sourceFilename IS NULL OR LOWER(COALESCE(t.sourceFilename, '')) LIKE LOWER(CONCAT('%', :sourceFilename, '%'))) AND " +
           "(:uploadedAfter IS NULL OR t.uploadedAt >= :uploadedAfter) AND " +
           "(:uploadedBefore IS NULL OR t.uploadedAt <= :uploadedBefore)")
    Page<SqlTemplate> searchLibrary(@Param("keyword") String keyword,
                                    @Param("executionMode") String executionMode,
                                    @Param("sourceFilename") String sourceFilename,
                                    @Param("uploadedAfter") Instant uploadedAfter,
                                    @Param("uploadedBefore") Instant uploadedBefore,
                                    Pageable pageable);

    List<SqlTemplate> findAllByOrderByIdAsc();
}
