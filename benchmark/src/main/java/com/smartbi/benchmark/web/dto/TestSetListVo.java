package com.smartbi.benchmark.web.dto;

import java.time.Instant;

public class TestSetListVo {

    private final Long id;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final String sourceFilename;
    private final long itemCount;

    public TestSetListVo(Long id,
                         String name,
                         String description,
                         Instant createdAt,
                         String sourceFilename,
                         long itemCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.sourceFilename = sourceFilename;
        this.itemCount = itemCount;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public long getItemCount() {
        return itemCount;
    }

    public long getSqlCount() {
        return itemCount;
    }
}
