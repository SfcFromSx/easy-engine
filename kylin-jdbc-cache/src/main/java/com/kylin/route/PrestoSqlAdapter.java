package com.kylin.route;

/**
 * Presto SQL 目标方言转换器。
 */
public class PrestoSqlAdapter implements SqlAdapter {

    @Override
    public String rewrite(String sql) {
        // TODO: 实现诸如将被 Kylin 识别的聚合函数翻译成为 Presto 特供函数的逻辑
        return sql;
    }
}
