package com.kylin.datasource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 负责提供真实的数据库连接。
 */
public interface ConnectionProvider extends AutoCloseable {
    
    /**
     * 获取数据库连接。可能来自连接池，也可能是新建的单连接。
     */
    Connection getConnection() throws SQLException;

    /**
     * 关闭资源（例如销毁连接池）。
     */
    @Override
    void close() throws Exception;
}
