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
           "LOWER(t.sqlText) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SqlTemplate> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<SqlTemplate> findAllByOrderByIdAsc(Pageable pageable);

    List<SqlTemplate> findAllByOrderByIdAsc();
}
