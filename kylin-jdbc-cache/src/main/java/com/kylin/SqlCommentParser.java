package com.kylin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一解析 SQL 中嵌入的所有注释指令（缓存提示、引擎路由以及企业元数据）。
 */
public class SqlCommentParser {

    private static final Logger logger = LoggerFactory.getLogger(SqlCommentParser.class);

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "('[^']*')|(--[^\\r\\n]*)|(/\\*.*?\\*/)", Pattern.DOTALL);

    private static final Pattern PAIR_PATTERN = Pattern.compile("([\\w-]+)\\s*[=:]\\s*([^\\s,;\\r\\n]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FLAG_PATTERN = Pattern.compile(
            "\\b(no-cache|cache-refresh|force-refresh)\\b",
            Pattern.CASE_INSENSITIVE);

    private enum HintType {
        NONE, DRIVER_HINT, METADATA
    }

    private static class MatchInfo {
        final int start;
        final int end;
        final boolean isDriverHint;
        final boolean isMetadata;

        MatchInfo(int start, int end, boolean isDriverHint, boolean isMetadata) {
            this.start = start;
            this.end = end;
            this.isDriverHint = isDriverHint;
            this.isMetadata = isMetadata;
        }
    }

    public static ParsedSql parse(String sql) {
        if (sql == null || sql.isEmpty()) {
            return new ParsedSql(sql, sql, new SqlMetadata.Builder().build());
        }

        SqlMetadata.Builder metaBuilder = new SqlMetadata.Builder();
        List<MatchInfo> matches = new ArrayList<>();
        Matcher m = COMMENT_PATTERN.matcher(sql);

        while (m.find()) {
            String match = m.group();
            if (match.startsWith("'")) {
                continue; // Skip string literals
            }

            String body = match.startsWith("--")
                    ? match.substring(2).trim()
                    : match.substring(2, match.length() - 2).trim();

            boolean hasDriverHint = false;
            boolean hasMetadata = false;

            Matcher pm = PAIR_PATTERN.matcher(body);
            while (pm.find()) {
                HintType type = populateField(metaBuilder, pm.group(1), pm.group(2));
                if (type == HintType.DRIVER_HINT)
                    hasDriverHint = true;
                if (type == HintType.METADATA)
                    hasMetadata = true;
            }

            Matcher fm = FLAG_PATTERN.matcher(body);
            while (fm.find()) {
                String flag = fm.group(1).toLowerCase();
                if (flag.equals("no-cache")) {
                    metaBuilder.noCache = true;
                    hasDriverHint = true;
                }
                if (flag.equals("cache-refresh") || flag.equals("force-refresh")) {
                    metaBuilder.cacheRefresh = true;
                    hasDriverHint = true;
                }
            }

            if (hasDriverHint || hasMetadata) {
                matches.add(new MatchInfo(m.start(), m.end(), hasDriverHint, hasMetadata));
            }
        }

        String cleanSql = buildVersion(sql, matches, true); // 剥离所有
        String executionSql = buildVersion(sql, matches, false); // 仅剥离驱动指令

        return new ParsedSql(cleanSql, executionSql, metaBuilder.build());
    }

    private static String buildVersion(String original, List<MatchInfo> matches, boolean stripAll) {
        StringBuilder sb = new StringBuilder();
        int lastPos = 0;
        for (MatchInfo match : matches) {
            sb.append(original, lastPos, match.start);

            boolean shouldStrip;
            if (stripAll) {
                shouldStrip = true; // 缓存版全部剥离
            } else {
                // 执行版逻辑：只有“纯驱动指令”被剥离；含有元数据的注释予以保留
                shouldStrip = match.isDriverHint && !match.isMetadata;
            }

            if (!shouldStrip) {
                sb.append(original, match.start, match.end);
            }
            lastPos = match.end;
        }
        sb.append(original.substring(lastPos));
        return finalizeSql(sb.toString());
    }

    private static String finalizeSql(String sql) {
        return sql.replaceAll("[ \\t]+\\n", "\\n")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private static HintType populateField(SqlMetadata.Builder builder, String key, String value) {
        String k = key.toUpperCase().replace("-", "_");
        switch (k) {
            case "NO_CACHE":
                builder.noCache = Boolean.parseBoolean(value);
                return HintType.DRIVER_HINT;
            case "CACHE_TTL":
                builder.cacheTtl = Integer.parseInt(value);
                return HintType.DRIVER_HINT;
            case "CACHE_KEY":
                builder.cacheKey = value;
                return HintType.DRIVER_HINT;
            case "CACHE_REFRESH":
                builder.cacheRefresh = Boolean.parseBoolean(value);
                return HintType.DRIVER_HINT;
            case "CACHE_TABLE":
                builder.cacheTable = value;
                return HintType.DRIVER_HINT;
            case "ENGINE":
                builder.engine = value;
                return HintType.DRIVER_HINT;

            case "YH_QUERYID":
                builder.queryId = value;
                return HintType.METADATA;
            case "YH_RPTID":
                builder.rptId = value;
                return HintType.METADATA;
            case "YH_RPTINSTID":
                builder.rptInstId = value;
                return HintType.METADATA;
            case "YH_RPTORG":
                builder.rptOrg = value;
                return HintType.METADATA;
            case "YH_RPTVIEWUSER":
                builder.rptViewUser = value;
                return HintType.METADATA;
            case "YH_RPTVIEWROLELIST":
                builder.rptViewRoleList = value;
                return HintType.METADATA;
            case "YH_RPTVIEWMODE":
                builder.rptViewMode = value;
                return HintType.METADATA;
            case "YH_SQLSENDTIME":
                builder.sqlSendTime = value;
                return HintType.METADATA;
            case "YH_RPTWIDGETTYPE":
                builder.rptWidgetType = value;
                return HintType.METADATA;
            case "YH_RPTWIDGETNAME":
                builder.rptWidgetName = value;
                return HintType.METADATA;
            case "YH_REFDATASET":
                builder.refDataset = value;
                return HintType.METADATA;
            case "YH_DATE_CC":
                builder.dateCc = value;
                return HintType.METADATA;
            case "YH_RPTSEARCHMODE":
                builder.rptSearchMode = value;
                return HintType.METADATA;

            default:
                if (k.startsWith("YH_")) {
                    builder.extraMetadata.put(k, value);
                    return HintType.METADATA;
                }
                return HintType.NONE;
        }
    }

    public static ParsedSql safeParse(String sql) {
        try {
            return parse(sql);
        } catch (Exception e) {
            logger.error("SQL 解析异常: {}", e.getMessage());
            return new ParsedSql(sql, sql, new SqlMetadata.Builder().build());
        }
    }

    public static final class ParsedSql {
        public final String cleanSql;
        public final String executionSql;
        public final SqlMetadata metadata;

        public ParsedSql(String cleanSql, String executionSql, SqlMetadata metadata) {
            this.cleanSql = cleanSql;
            this.executionSql = executionSql;
            this.metadata = metadata;
        }
    }
}
