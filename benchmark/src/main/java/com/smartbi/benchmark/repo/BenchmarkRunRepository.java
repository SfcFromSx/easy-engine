package com.smartbi.benchmark.repo;

import com.smartbi.benchmark.domain.BenchmarkRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BenchmarkRunRepository extends JpaRepository<BenchmarkRun, Long> {

    @Transactional
    @Modifying
    @Query("update BenchmarkRun r set r.currentProgress = ?2 where r.id = ?1")
    void updateProgress(Long id, Integer progress);

    Page<BenchmarkRun> findByJobIdOrderByStartedAtDesc(Long jobId, Pageable pageable);

    Optional<BenchmarkRun> findFirstByJobIdAndStartedAtLessThanOrderByStartedAtDesc(Long jobId, Instant startedAt);

    Optional<BenchmarkRun> findFirstByJobIdAndStatusAndIdNotOrderByStartedAtDesc(Long jobId, com.smartbi.benchmark.domain.RunStatus status, Long id);

    List<BenchmarkRun> findByStatus(com.smartbi.benchmark.domain.RunStatus status);

    List<BenchmarkRun> findByStatusAndStartedAtBefore(com.smartbi.benchmark.domain.RunStatus status, Instant startedAt);

    boolean existsByJobId(Long jobId);
}
