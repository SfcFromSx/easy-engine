package com.kylin.cache.prepared;

import com.kylin.cache.CachePolicy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 管理 PreparedStatement 的参数缓存状态。
 *
 * <p>规则保持简单：
 * <ul>
 *   <li>SQL 不带 {@code ?} 时，不需要参数指纹，按普通查询缓存</li>
 *   <li>SQL 带 {@code ?} 且配置关闭时，直接透传，不走缓存</li>
 *   <li>SQL 带 {@code ?} 且配置开启时，只有参数仍可安全指纹化才走缓存</li>
 * </ul>
 */
public class PreparedStatementCacheSupport {

    private final CachePolicy cachePolicy;
    private final boolean parameterizedSql;
    private final boolean parameterCacheEnabled;
    private final List<String> paramList = new ArrayList<>();
    private boolean cacheable = true;

    public PreparedStatementCacheSupport(String sql, CachePolicy cachePolicy, boolean parameterCacheEnabled) {
        this.cachePolicy = cachePolicy;
        this.parameterizedSql = sql != null && sql.indexOf('?') >= 0;
        this.parameterCacheEnabled = parameterCacheEnabled;
    }

    /**
     * 当前 PreparedStatement 是否应该走缓存。
     */
    public boolean shouldUseCache() {
        return !parameterizedSql || (parameterCacheEnabled && cacheable);
    }

    /**
     * 返回当前参数指纹；只有参数化缓存启用且仍可缓存时才有值。
     */
    public String currentFingerprint() {
        if (!parameterizedSql || !parameterCacheEnabled || !cacheable) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramList.size(); i++) {
            String value = paramList.get(i);
            sb.append(i + 1).append('=');
            if (value == null) {
                sb.append("<UNSET>");
            } else {
                sb.append(value.length()).append(':').append(value);
            }
            sb.append(';');
        }
        return sb.toString();
    }

    /**
     * 记录普通参数值。
     */
    public void recordValue(int index, Object value) {
        if (!canTrackParameters()) {
            return;
        }
        ensureCapacity(index);
        paramList.set(index - 1, fingerprintValue(value));
    }

    /**
     * 记录 setObject 绑定的参数；复杂对象直接降级为不缓存。
     */
    public void recordObjectValue(int index, Object value) {
        if (!canTrackParameters()) {
            return;
        }
        if (!cachePolicy.isFingerprintableParameterValue(value)) {
            markUncacheable(index);
            return;
        }
        recordValue(index, value);
    }

    /**
     * 标记当前参数集不可缓存。
     */
    public void markUncacheable(int index) {
        if (!canTrackParameters()) {
            return;
        }
        ensureCapacity(index);
        paramList.set(index - 1, null);
        cacheable = false;
    }

    /**
     * 清空参数状态。
     */
    public void clear() {
        paramList.clear();
        cacheable = true;
    }

    private boolean canTrackParameters() {
        return parameterizedSql && parameterCacheEnabled;
    }

    private void ensureCapacity(int index) {
        while (paramList.size() < index) {
            paramList.add(null);
        }
    }

    private static String fingerprintValue(Object value) {
        if (value == null) {
            return "<NULL>";
        }
        if (value instanceof byte[]) {
            return "bytes:" + Base64.getEncoder().encodeToString((byte[]) value);
        }
        if (value instanceof BigDecimal) {
            return "decimal:" + ((BigDecimal) value).toPlainString();
        }
        return value.getClass().getName() + ":" + value;
    }
}
