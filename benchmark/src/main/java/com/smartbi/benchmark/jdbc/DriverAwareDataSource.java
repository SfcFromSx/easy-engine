package com.smartbi.benchmark.jdbc;

import com.smartbi.benchmark.domain.BenchmarkDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class DriverAwareDataSource implements DataSource {

    private final JdbcDriverRegistry driverRegistry;
    private final BenchmarkDataSource definition;
    private volatile PrintWriter logWriter;
    private volatile int loginTimeout;

    public DriverAwareDataSource(JdbcDriverRegistry driverRegistry, BenchmarkDataSource definition) {
        this.driverRegistry = driverRegistry;
        this.definition = definition;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return driverRegistry.openConnection(definition);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return driverRegistry.openConnection(
                definition.getDriverClass(),
                definition.getJdbcUrl(),
                username,
                password
        );
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() {
        return loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("DriverAwareDataSource does not expose a parent logger");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("DriverAwareDataSource cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
