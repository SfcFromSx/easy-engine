package com.smartbi.benchmark.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "benchmark_job")
public class BenchmarkJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "data_source_id")
    private Long dataSourceId;

    @Column(name = "concurrent_threads", nullable = false)
    private int concurrentThreads = 4;

    @Column(nullable = false)
    private int rounds = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private BenchmarkStrategy strategy = BenchmarkStrategy.RANDOM_WEIGHT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * 非空时压测仅使用该测试集中的 SQL；为空时使用全局 {@code benchmark_sql_template}。
     */
    @Column(name = "test_set_id")
    private Long testSetId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }

    public int getConcurrentThreads() { return concurrentThreads; }
    public void setConcurrentThreads(int concurrentThreads) { this.concurrentThreads = concurrentThreads; }

    public int getRounds() { return rounds; }
    public void setRounds(int rounds) { this.rounds = rounds; }

    public BenchmarkStrategy getStrategy() { return strategy; }
    public void setStrategy(BenchmarkStrategy strategy) { this.strategy = strategy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getTestSetId() { return testSetId; }
    public void setTestSetId(Long testSetId) { this.testSetId = testSetId; }
}
