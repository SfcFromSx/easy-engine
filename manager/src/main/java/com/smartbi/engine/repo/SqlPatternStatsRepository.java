package com.smartbi.engine.repo;

import com.smartbi.engine.domain.SqlPatternStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SqlPatternStatsRepository extends JpaRepository<SqlPatternStats, Long>, JpaSpecificationExecutor<SqlPatternStats> {

    Optional<SqlPatternStats> findBySqlFingerprint(String sqlFingerprint);

    List<SqlPatternStats> findTop50ByOrderByExecutionCountDesc();

    Page<SqlPatternStats> findAllByOrderByExecutionCountDesc(Pageable pageable);

    Page<SqlPatternStats> findAllBySqlFingerprintOrderByExecutionCountDesc(String sqlFingerprint, Pageable pageable);
}
