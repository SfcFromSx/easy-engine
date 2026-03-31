package com.smartbi.query.repo;

import com.smartbi.query.domain.SqlExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SqlExecutionRecordRepository extends JpaRepository<SqlExecutionRecord, Long> {
}
