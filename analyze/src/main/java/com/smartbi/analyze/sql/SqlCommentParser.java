package com.smartbi.analyze.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlCommentParser {

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "('[^']*')|(--[^\\r\\n]*)|(/\\*.*?\\*/)", Pattern.DOTALL);
    private static final Pattern PAIR_PATTERN = Pattern.compile("([\\w-]+)\\s*[=:]\\s*([^\\s,;\\r\\n]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FLAG_PATTERN = Pattern.compile(
            "\\b(no-cache|cache-refresh|force-refresh)\\b",
            Pattern.CASE_INSENSITIVE);

    private SqlCommentParser() {
    }

    private enum HintType {
        NONE, DRIVER_HINT, METADATA
    }

    private static class MatchInfo {
        final int start;
        final int end;
        final boolean driverHint;
        final boolean metadata;

        MatchInfo(int start, int end, boolean driverHint, boolean metadata) {
            this.start = start;
            this.end = end;
            this.driverHint = driverHint;
            this.metadata = metadata;
        }
    }

    public static ParsedSql parse(String sql) {
        if (sql == null || sql.isEmpty()) {
            return new ParsedSql(sql, sql, new SqlMetadata.Builder().build());
        }

        SqlMetadata.Builder builder = new SqlMetadata.Builder();
        List<MatchInfo> matches = new ArrayList<MatchInfo>();
        Matcher matcher = COMMENT_PATTERN.matcher(sql);
        while (matcher.find()) {
            String match = matcher.group();
            if (match.startsWith("'")) {
                continue;
            }
            String body = extractCommentBody(match);
            boolean hasDriverHint = false;
            boolean hasMetadata = false;

            Matcher pairMatcher = PAIR_PATTERN.matcher(body);
            while (pairMatcher.find()) {
                HintType type = populateField(builder, pairMatcher.group(1), pairMatcher.group(2));
                if (type == HintType.DRIVER_HINT) {
                    hasDriverHint = true;
                }
                if (type == HintType.METADATA) {
                    hasMetadata = true;
                }
            }

            Matcher flagMatcher = FLAG_PATTERN.matcher(body);
            while (flagMatcher.find()) {
                String flag = flagMatcher.group(1).toLowerCase(Locale.ROOT);
                if ("no-cache".equals(flag)) {
                    builder.noCache = true;
                    hasDriverHint = true;
                }
                if ("cache-refresh".equals(flag) || "force-refresh".equals(flag)) {
                    builder.cacheRefresh = true;
                    hasDriverHint = true;
                }
            }

            if (hasDriverHint || hasMetadata) {
                matches.add(new MatchInfo(matcher.start(), matcher.end(), hasDriverHint, hasMetadata));
            }
        }

        String cleanSql = buildVersion(sql, matches, true);
        String executionSql = buildVersion(sql, matches, false);
        return new ParsedSql(cleanSql, executionSql, builder.build());
    }

    public static ParsedSql safeParse(String sql) {
        try {
            return parse(sql);
        } catch (Exception ex) {
            return new ParsedSql(sql, sql, new SqlMetadata.Builder().build());
        }
    }

    public static String removeMetadataKey(String sql, String key) {
        if (!hasText(sql) || !hasText(key)) {
            return sql;
        }
        String normalizedKey = key.trim();
        Matcher matcher = COMMENT_PATTERN.matcher(sql);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            if (match.startsWith("'")) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(match));
                continue;
            }
            String updated = removeMetadataKeyFromComment(match, normalizedKey);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(updated));
        }
        matcher.appendTail(buffer);
        return finalizeSql(buffer.toString());
    }

    private static String removeMetadataKeyFromComment(String comment, String key) {
        String body = extractCommentBody(comment);
        String updatedBody = body.replaceAll("(?i)(^|[\\s,;])" + Pattern.quote(key) + "\\s*[=:]\\s*[^\\s,;\\r\\n]+", "$1");
        updatedBody = updatedBody.replaceAll("\\s{2,}", " ")
                .replaceAll("^[\\s,;]+", "")
                .replaceAll("[\\s,;]+$", "")
                .trim();
        if (updatedBody.isEmpty()) {
            return "";
        }
        if (comment.startsWith("--")) {
            return "-- " + updatedBody;
        }
        return "/* " + updatedBody + " */";
    }

    private static String buildVersion(String original, List<MatchInfo> matches, boolean stripAll) {
        StringBuilder builder = new StringBuilder();
        int lastPos = 0;
        for (MatchInfo match : matches) {
            builder.append(original, lastPos, match.start);
            boolean shouldStrip = stripAll || (match.driverHint && !match.metadata);
            if (!shouldStrip) {
                builder.append(original, match.start, match.end);
            }
            lastPos = match.end;
        }
        builder.append(original.substring(lastPos));
        return finalizeSql(builder.toString());
    }

    private static String finalizeSql(String sql) {
        return sql.replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("^\\s+", "")
                .replaceAll("\\s+$", "");
    }

    private static String extractCommentBody(String comment) {
        return comment.startsWith("--")
                ? comment.substring(2).trim()
                : comment.substring(2, comment.length() - 2).trim();
    }

    private static HintType populateField(SqlMetadata.Builder builder, String key, String value) {
        String normalized = key.toUpperCase(Locale.ROOT).replace("-", "_");
        switch (normalized) {
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
                if (normalized.startsWith("YH_")) {
                    builder.extraMetadata.put(normalized, value);
                    return HintType.METADATA;
                }
                return HintType.NONE;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
