package com.smartbi.benchmark.repo;

import com.smartbi.benchmark.domain.BenchmarkTestSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenchmarkTestSetRepository extends JpaRepository<BenchmarkTestSet, Long> {

    List<BenchmarkTestSet> findAllByOrderByCreatedAtDesc();
}
