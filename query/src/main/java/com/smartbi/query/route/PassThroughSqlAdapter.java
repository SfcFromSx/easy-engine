package com.smartbi.query.route;

public class PassThroughSqlAdapter implements SqlAdapter {

    @Override
    public String rewrite(String sql) {
        return sql;
    }
}
