package com.smartbi.benchmark.repo;

import com.smartbi.benchmark.domain.BenchmarkJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenchmarkJobRepository extends JpaRepository<BenchmarkJob, Long> {

    boolean existsByDataSourceId(Long dataSourceId);
}
