package com.smartbi.query.cache;

import com.smartbi.query.api.dto.StatementParameterDto;

import java.util.ArrayList;
import java.util.List;

public final class PreparedParameterSupport {

    private PreparedParameterSupport() {
    }

    public static boolean shouldUseCache(String sql,
                                         List<StatementParameterDto> params,
                                         boolean preparedSqlEnabled,
                                         CachePolicy cachePolicy) {
        boolean parameterizedSql = sql != null && sql.indexOf('?') >= 0;
        if (!parameterizedSql) {
            return true;
        }
        if (!preparedSqlEnabled) {
            return false;
        }
        if (params == null) {
            return true;
        }
        for (StatementParameterDto param : params) {
            if (param != null && !cachePolicy.isFingerprintableParameterType(param.getClassName())) {
                return false;
            }
        }
        return true;
    }

    public static String fingerprint(List<StatementParameterDto> params,
                                     String sql,
                                     boolean preparedSqlEnabled,
                                     CachePolicy cachePolicy) {
        boolean parameterizedSql = sql != null && sql.indexOf('?') >= 0;
        if (!parameterizedSql || !preparedSqlEnabled) {
            return null;
        }
        if (params == null || params.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<String>();
        for (int i = 0; i < params.size(); i++) {
            StatementParameterDto param = params.get(i);
            if (param == null) {
                parts.add((i + 1) + "=0:<NULL>");
                continue;
            }
            if (!cachePolicy.isFingerprintableParameterType(param.getClassName())) {
                return null;
            }
            String className = param.getClassName() == null ? "unknown" : param.getClassName();
            String value = param.getValue() == null ? "<NULL>" : param.getValue();
            String token = className + ":" + value;
            parts.add((i + 1) + "=" + token.length() + ":" + token);
        }
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part).append(';');
        }
        return builder.toString();
    }
}
