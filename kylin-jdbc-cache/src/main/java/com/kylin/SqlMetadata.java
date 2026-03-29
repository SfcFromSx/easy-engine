package com.kylin;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一的 SQL 元数据与指令对象。
 */
public class SqlMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- 核心控制指令 ---
    public final boolean noCache;
    public final Integer cacheTtl;
    public final String cacheKey;
    public final boolean cacheRefresh;
    public final String cacheTable;
    public final String engine;

    // --- 标准企业级 BI 元数据 (常用字段快照) ---
    public final String queryId;          // YH_QUERYID
    public final String rptId;            // YH_RPTID
    public final String rptInstId;        // YH_RPTINSTID
    public final String rptOrg;           // YH_RPTORG
    public final String rptViewUser;      // YH_RPTVIEWUSER
    public final String rptViewRoleList;  // YH_RPTVIEWROLELIST
    public final String rptViewMode;      // YH_RPTVIEWMODE
    public final String sqlSendTime;      // YH_SQLSENDTIME
    public final String rptWidgetType;    // YH_RPTWIDGETTYPE
    public final String rptWidgetName;    // YH_RPTWIDGETNAME
    public final String refDataset;       // YH_REFDATASET
    public final String dateCc;           // YH_DATE_CC
    public final String rptSearchMode;    // YH_RPTSEARCHMODE

    // --- 动态扩展元数据 (其他所有 YH_ 前缀字段) ---
    public final Map<String, String> extraMetadata;

    public SqlMetadata(Builder builder) {
        this.noCache = builder.noCache;
        this.cacheTtl = builder.cacheTtl;
        this.cacheKey = builder.cacheKey;
        this.cacheRefresh = builder.cacheRefresh;
        this.cacheTable = builder.cacheTable;
        this.engine = builder.engine;

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

        this.extraMetadata = Collections.unmodifiableMap(new HashMap<>(builder.extraMetadata));
    }

    public static class Builder {
        public boolean noCache = false;
        public Integer cacheTtl = null;
        public String cacheKey = null;
        public boolean cacheRefresh = false;
        public String cacheTable = null;
        public String engine = null;

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

        public Map<String, String> extraMetadata = new HashMap<>();

        public SqlMetadata build() {
            return new SqlMetadata(this);
        }
    }

    @Override
    public String toString() {
        return "SqlMetadata{" +
                "engine='" + engine + '\'' +
                ", queryId='" + queryId + '\'' +
                ", extras=" + extraMetadata.size() +
                '}';
    }
}
