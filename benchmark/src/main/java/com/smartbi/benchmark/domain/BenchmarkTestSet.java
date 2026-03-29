package com.smartbi.benchmark.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "benchmark_test_set")
public class BenchmarkTestSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false, length = 512)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(name = "source_filename", length = 512)
    private String sourceFilename;

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }
}
