package com.kylin.route;

/**
 * Kylin SQL 的直接传导适配器。
 */
public class KylinSqlAdapter implements SqlAdapter {
    
    @Override
    public String rewrite(String sql) {
        // TODO: 如果遇到 Kylin 特殊语法需要降级或者修正的补充在此处
        return sql;
    }
}
