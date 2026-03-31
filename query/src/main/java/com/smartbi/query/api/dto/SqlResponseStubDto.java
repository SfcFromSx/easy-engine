package com.smartbi.query.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility response envelope for the Kylin-shaped `/kylin/api/query` contract.
 * The `Stub` suffix is intentional and mirrors the historical JDBC-facing payload name.
 */
public class SqlResponseStubDto implements Serializable {

    private List<ColumnMetaStubDto> columnMetas = new ArrayList<ColumnMetaStubDto>();
    private List<String[]> results = new ArrayList<String[]>();
    private String cube;
    private int affectedRowCount;
    private boolean isException;
    private String exceptionMessage;
    private long duration;
    private boolean partial;
    private long totalScanCount;
    private boolean hitExceptionCache;
    private boolean storageCacheUsed;

    public List<ColumnMetaStubDto> getColumnMetas() {
        return columnMetas;
    }

    public void setColumnMetas(List<ColumnMetaStubDto> columnMetas) {
        this.columnMetas = columnMetas;
    }

    public List<String[]> getResults() {
        return results;
    }

    public void setResults(List<String[]> results) {
        this.results = results;
    }

    public String getCube() {
        return cube;
    }

    public void setCube(String cube) {
        this.cube = cube;
    }

    public int getAffectedRowCount() {
        return affectedRowCount;
    }

    public void setAffectedRowCount(int affectedRowCount) {
        this.affectedRowCount = affectedRowCount;
    }

    public boolean getIsException() {
        return isException;
    }

    public void setIsException(boolean exception) {
        isException = exception;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    public long getTotalScanCount() {
        return totalScanCount;
    }

    public void setTotalScanCount(long totalScanCount) {
        this.totalScanCount = totalScanCount;
    }

    public boolean isHitExceptionCache() {
        return hitExceptionCache;
    }

    public void setHitExceptionCache(boolean hitExceptionCache) {
        this.hitExceptionCache = hitExceptionCache;
    }

    public boolean isStorageCacheUsed() {
        return storageCacheUsed;
    }

    public void setStorageCacheUsed(boolean storageCacheUsed) {
        this.storageCacheUsed = storageCacheUsed;
    }

    /**
     * Compatibility metadata block that mirrors Kylin's column metadata payload shape.
     */
    public static class ColumnMetaStubDto implements Serializable {
        private boolean autoIncrement;
        private boolean caseSensitive;
        private boolean searchable;
        private boolean currency;
        private int isNullable;
        private boolean signed;
        private int displaySize;
        private String label;
        private String name;
        private String schemaName;
        private String catelogName;
        private String tableName;
        private int precision;
        private int scale;
        private int columnType;
        private String columnTypeName;
        private boolean readOnly;
        private boolean writable;
        private boolean definitelyWritable;

        public boolean isAutoIncrement() {
            return autoIncrement;
        }

        public void setAutoIncrement(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public boolean isSearchable() {
            return searchable;
        }

        public void setSearchable(boolean searchable) {
            this.searchable = searchable;
        }

        public boolean isCurrency() {
            return currency;
        }

        public void setCurrency(boolean currency) {
            this.currency = currency;
        }

        public int getIsNullable() {
            return isNullable;
        }

        public void setIsNullable(int isNullable) {
            this.isNullable = isNullable;
        }

        public boolean isSigned() {
            return signed;
        }

        public void setSigned(boolean signed) {
            this.signed = signed;
        }

        public int getDisplaySize() {
            return displaySize;
        }

        public void setDisplaySize(int displaySize) {
            this.displaySize = displaySize;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        public String getCatelogName() {
            return catelogName;
        }

        public void setCatelogName(String catelogName) {
            this.catelogName = catelogName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public int getPrecision() {
            return precision;
        }

        public void setPrecision(int precision) {
            this.precision = precision;
        }

        public int getScale() {
            return scale;
        }

        public void setScale(int scale) {
            this.scale = scale;
        }

        public int getColumnType() {
            return columnType;
        }

        public void setColumnType(int columnType) {
            this.columnType = columnType;
        }

        public String getColumnTypeName() {
            return columnTypeName;
        }

        public void setColumnTypeName(String columnTypeName) {
            this.columnTypeName = columnTypeName;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public boolean isWritable() {
            return writable;
        }

        public void setWritable(boolean writable) {
            this.writable = writable;
        }

        public boolean isDefinitelyWritable() {
            return definitelyWritable;
        }

        public void setDefinitelyWritable(boolean definitelyWritable) {
            this.definitelyWritable = definitelyWritable;
        }
    }
}
