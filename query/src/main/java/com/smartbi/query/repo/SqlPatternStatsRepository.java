package com.smartbi.query.repo;

import com.smartbi.query.domain.SqlPatternStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SqlPatternStatsRepository extends JpaRepository<SqlPatternStats, Long> {

    Optional<SqlPatternStats> findBySqlFingerprint(String sqlFingerprint);
}
