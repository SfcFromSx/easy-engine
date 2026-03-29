package com.smartbi.benchmark.domain;

public enum BenchmarkStrategy {
    /** Pick SQL by template weight */
    RANDOM_WEIGHT,
    /** Round-robin over templates */
    ROUND_ROBIN,
    /** Append cache-busting style comment to increase backend traffic */
    CACHE_PENETRATION
}
