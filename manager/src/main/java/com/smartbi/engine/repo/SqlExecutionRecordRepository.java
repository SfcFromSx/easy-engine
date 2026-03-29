package com.smartbi.engine.repo;

import com.smartbi.engine.domain.ParseStatus;
import com.smartbi.engine.domain.SqlExecutionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SqlExecutionRecordRepository extends JpaRepository<SqlExecutionRecord, Long> {

    Page<SqlExecutionRecord> findAllByOrderByReceivedAtDesc(Pageable pageable);

    Page<SqlExecutionRecord> findAllBySqlFingerprintOrderByReceivedAtDesc(String sqlFingerprint, Pageable pageable);

    long countByParseStatus(ParseStatus parseStatus);

    long countByCacheHitTrue();

    SqlExecutionRecord findTopByOrderByReceivedAtDesc();
}
