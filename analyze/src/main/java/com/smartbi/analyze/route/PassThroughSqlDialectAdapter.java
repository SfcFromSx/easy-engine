package com.smartbi.analyze.route;

public class PassThroughSqlDialectAdapter implements SqlDialectAdapter {

    @Override
    public String rewrite(String sql) {
        return sql;
    }
}
