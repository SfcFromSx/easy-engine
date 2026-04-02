package com.smartbi.analyze.sql;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SqlMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    public final boolean noCache;
    public final Integer cacheTtl;
    public final String cacheKey;
    public final boolean cacheRefresh;
    public final String cacheTable;

    public final String queryId;
    public final String rptId;
    public final String rptInstId;
    public final String rptOrg;
    public final String rptViewUser;
    public final String rptViewRoleList;
    public final String rptViewMode;
    public final String sqlSendTime;
    public final String rptWidgetType;
    public final String rptWidgetName;
    public final String refDataset;
    public final String dateCc;
    public final String rptSearchMode;

    public final Map<String, String> extraMetadata;

    public SqlMetadata(Builder builder) {
        this.noCache = builder.noCache;
        this.cacheTtl = builder.cacheTtl;
        this.cacheKey = builder.cacheKey;
        this.cacheRefresh = builder.cacheRefresh;
        this.cacheTable = builder.cacheTable;
        this.queryId = builder.queryId;
        this.rptId = builder.rptId;
        this.rptInstId = builder.rptInstId;
        this.rptOrg = builder.rptOrg;
        this.rptViewUser = builder.rptViewUser;
        this.rptViewRoleList = builder.rptViewRoleList;
        this.rptViewMode = builder.rptViewMode;
        this.sqlSendTime = builder.sqlSendTime;
        this.rptWidgetType = builder.rptWidgetType;
        this.rptWidgetName = builder.rptWidgetName;
        this.refDataset = builder.refDataset;
        this.dateCc = builder.dateCc;
        this.rptSearchMode = builder.rptSearchMode;
        this.extraMetadata = Collections.unmodifiableMap(new HashMap<String, String>(builder.extraMetadata));
    }

    public static class Builder {
        public boolean noCache;
        public Integer cacheTtl;
        public String cacheKey;
        public boolean cacheRefresh;
        public String cacheTable;

        public String queryId;
        public String rptId;
        public String rptInstId;
        public String rptOrg;
        public String rptViewUser;
        public String rptViewRoleList;
        public String rptViewMode;
        public String sqlSendTime;
        public String rptWidgetType;
        public String rptWidgetName;
        public String refDataset;
        public String dateCc;
        public String rptSearchMode;

        public Map<String, String> extraMetadata = new HashMap<String, String>();

        public SqlMetadata build() {
            return new SqlMetadata(this);
        }
    }
}
