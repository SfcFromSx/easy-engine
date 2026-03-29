package com.smartbi.benchmark.repo;

import com.smartbi.benchmark.domain.BenchmarkDataSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenchmarkDataSourceRepository extends JpaRepository<BenchmarkDataSource, Long> {
}
