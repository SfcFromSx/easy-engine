package com.smartbi.benchmark.domain;

public enum SqlExecutionMode {
    STATEMENT,
    PREPARED_STATEMENT;

    public static SqlExecutionMode from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return STATEMENT;
        }
        return SqlExecutionMode.valueOf(value.trim().toUpperCase());
    }
}
