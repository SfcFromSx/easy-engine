package com.smartbi.benchmark.repo;

import com.smartbi.benchmark.domain.SqlTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SqlTemplateRepository extends JpaRepository<SqlTemplate, Long> {

    @Query("SELECT t FROM SqlTemplate t WHERE " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.sqlText) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.executionMode, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.paramJson, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SqlTemplate> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<SqlTemplate> findByExecutionModeOrderByIdAsc(String executionMode, Pageable pageable);

    @Query("SELECT t FROM SqlTemplate t WHERE t.executionMode = :executionMode AND (" +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.sqlText) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.executionMode, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(t.paramJson, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SqlTemplate> searchByKeywordAndExecutionMode(@Param("keyword") String keyword,
                                                      @Param("executionMode") String executionMode,
                                                      Pageable pageable);

    Page<SqlTemplate> findAllByOrderByIdAsc(Pageable pageable);

    List<SqlTemplate> findAllByOrderByIdAsc();
}
