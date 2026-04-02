package com.smartbi.query.parsing;

public class SqlMetadata extends com.smartbi.analyze.sql.SqlMetadata {

    public SqlMetadata(Builder builder) {
        super(builder);
    }

    public SqlMetadata(com.smartbi.analyze.sql.SqlMetadata source) {
        this(copyOf(source));
    }

    private static Builder copyOf(com.smartbi.analyze.sql.SqlMetadata source) {
        Builder builder = new Builder();
        if (source == null) {
            return builder;
        }
        builder.noCache = source.noCache;
        builder.cacheTtl = source.cacheTtl;
        builder.cacheKey = source.cacheKey;
        builder.cacheRefresh = source.cacheRefresh;
        builder.cacheTable = source.cacheTable;
        builder.queryId = source.queryId;
        builder.rptId = source.rptId;
        builder.rptInstId = source.rptInstId;
        builder.rptOrg = source.rptOrg;
        builder.rptViewUser = source.rptViewUser;
        builder.rptViewRoleList = source.rptViewRoleList;
        builder.rptViewMode = source.rptViewMode;
        builder.sqlSendTime = source.sqlSendTime;
        builder.rptWidgetType = source.rptWidgetType;
        builder.rptWidgetName = source.rptWidgetName;
        builder.refDataset = source.refDataset;
        builder.dateCc = source.dateCc;
        builder.rptSearchMode = source.rptSearchMode;
        builder.extraMetadata.putAll(source.extraMetadata);
        return builder;
    }

    public static class Builder extends com.smartbi.analyze.sql.SqlMetadata.Builder {
        @Override
        public SqlMetadata build() {
            return new SqlMetadata(this);
        }
    }
}
