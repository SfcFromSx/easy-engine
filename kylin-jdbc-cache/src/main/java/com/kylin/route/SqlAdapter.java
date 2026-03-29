package com.kylin.route;

/**
 * 针对特定方言对 SQL 发起语法层面的转换。
 */
public interface SqlAdapter {
    /**
     * 将标准化或 Kylin 语法的 SQL 转换成目标数据源所需的原生语法。
     */
    String rewrite(String sql);
}
