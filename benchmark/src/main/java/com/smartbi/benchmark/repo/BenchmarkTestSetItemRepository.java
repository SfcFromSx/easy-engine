package com.smartbi.benchmark.repo;

import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenchmarkTestSetItemRepository extends JpaRepository<BenchmarkTestSetItem, Long> {

    List<BenchmarkTestSetItem> findByTestSetIdOrderBySortOrderAsc(Long testSetId);

    long countByTestSetId(Long testSetId);
}
