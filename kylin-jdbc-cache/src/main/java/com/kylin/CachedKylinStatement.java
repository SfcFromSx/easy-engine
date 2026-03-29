package com.kylin;

import com.kylin.cache.CacheLogic;
import com.kylin.cache.CachePolicy;
import com.kylin.record.SqlExecutionTrace;
import com.kylin.record.SqlExecutionTraceService;
import com.kylin.route.RoutedSql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class CachedKylinStatement implements Statement {

    private static final Logger log = LoggerFactory.getLogger(CachedKylinStatement.class);

    protected final CachedKylinConnection connection;
    protected final CacheLogic cacheLogic;
    protected final CachePolicy cachePolicy;
    protected final SqlExecutionTraceService sqlExecutionTraceService;

    // Statement properties
    protected int maxFieldSize = 0;
    protected int maxRows = 0;
    protected boolean escapeProcessing = true;
    protected int queryTimeout = 0;
    protected String cursorName;
    protected int fetchDirection = ResultSet.FETCH_FORWARD;
    protected int fetchSize = 0;
    protected boolean poolable = false;
    protected boolean closeOnCompletion = false;

    // Creation parameters
    protected final int resultSetType;
    protected final int resultSetConcurrency;
    protected final int resultSetHoldability;

    protected ResultSet currentResultSet;

    // Active lazily provisioned targets
    protected Connection currentActiveConnection;
    protected Statement currentActiveStatement;

    public CachedKylinStatement(CachedKylinConnection connection, CacheLogic cacheLogic, CachePolicy cachePolicy,
            SqlExecutionTraceService sqlExecutionTraceService) {
        this(connection, cacheLogic, cachePolicy, sqlExecutionTraceService, ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    public CachedKylinStatement(CachedKylinConnection connection, CacheLogic cacheLogic, CachePolicy cachePolicy,
            SqlExecutionTraceService sqlExecutionTraceService, int resultSetType, int resultSetConcurrency) {
        this(connection, cacheLogic, cachePolicy, sqlExecutionTraceService, resultSetType, resultSetConcurrency,
                ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    public CachedKylinStatement(CachedKylinConnection connection, CacheLogic cacheLogic, CachePolicy cachePolicy,
            SqlExecutionTraceService sqlExecutionTraceService, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) {
        this.connection = connection;
        this.cacheLogic = cacheLogic;
        this.cachePolicy = cachePolicy;
        this.sqlExecutionTraceService = sqlExecutionTraceService;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
    }

    protected void cleanUpActiveTarget() {
        if (currentActiveStatement != null) {
            try {
                currentActiveStatement.close();
            } catch (SQLException ignored) {
            }
            currentActiveStatement = null;
        }
        if (currentActiveConnection != null) {
            try {
                currentActiveConnection.close();
            } catch (SQLException ignored) {
            }
            currentActiveConnection = null;
        }
    }

    protected void applyProperties(Statement stmt) throws SQLException {
        // Kylin Avatica Statement 未实现部分 JDBC 可选能力，逐项降级避免阻断查询。
        try {
            if (maxFieldSize > 0)
                stmt.setMaxFieldSize(maxFieldSize);
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        try {
            if (maxRows > 0)
                stmt.setMaxRows(maxRows);
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        try {
            stmt.setEscapeProcessing(escapeProcessing);
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        try {
            if (queryTimeout > 0)
                stmt.setQueryTimeout(queryTimeout);
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        try {
            if (cursorName != null)
                stmt.setCursorName(cursorName);
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        try {
            stmt.setFetchDirection(fetchDirection);
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        try {
            if (fetchSize > 0)
                stmt.setFetchSize(fetchSize);
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        try {
            stmt.setPoolable(poolable);
        } catch (SQLFeatureNotSupportedException ignored) {
        }
        try {
            if (closeOnCompletion)
                stmt.closeOnCompletion();
        } catch (SQLFeatureNotSupportedException ignored) {
        }
    }

    protected Statement createUnderlyingStatement() throws SQLException {
        return currentActiveConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        cleanUpActiveTarget();
        SqlCommentParser.ParsedSql parsed = SqlCommentParser.safeParse(sql);
        ensureQuerySql(parsed.cleanSql);

        RoutedSql routed = connection.getRouteService().routeAndRewrite(sql, parsed);
        currentActiveConnection = connection.getRegistry().getConnection(routed.datasourceName);
        currentActiveStatement = createUnderlyingStatement();
        applyProperties(currentActiveStatement);

        long startNanos = System.nanoTime();
        try {
            CacheLogic.CacheExecutionResult result = cacheLogic.executeWithCacheDetailed(
                    parsed, null,
                    () -> currentActiveStatement.executeQuery(routed.executionSql),
                    routed.datasourceName);
            reportExecution(routed, parsed, null, startNanos, result.cacheHit, null);
            return storeCurrentResult(result.resultSet);
        } catch (SQLException e) {
            reportExecution(routed, parsed, null, startNanos, false, e);
            throw e;
        }
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        executeQuery(sql);
        return true;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        throw unsupportedSelectOnly("executeUpdate");
    }

    @Override
    public void close() throws SQLException {
        currentResultSet = null;
        cleanUpActiveTarget();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return maxFieldSize;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        this.maxFieldSize = max;
        if (currentActiveStatement != null)
            currentActiveStatement.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return maxRows;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        this.maxRows = max;
        if (currentActiveStatement != null)
            currentActiveStatement.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        this.escapeProcessing = enable;
        if (currentActiveStatement != null)
            currentActiveStatement.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        this.queryTimeout = seconds;
        if (currentActiveStatement != null)
            currentActiveStatement.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        if (currentActiveStatement != null)
            currentActiveStatement.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return currentActiveStatement == null ? null : currentActiveStatement.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (currentActiveStatement != null)
            currentActiveStatement.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        this.cursorName = name;
        if (currentActiveStatement != null)
            currentActiveStatement.setCursorName(name);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return currentResultSet != null ? currentResultSet
                : (currentActiveStatement != null ? currentActiveStatement.getResultSet() : null);
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return currentResultSet != null ? -1
                : (currentActiveStatement != null ? currentActiveStatement.getUpdateCount() : -1);
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        clearCurrentResult();
        return currentActiveStatement != null && currentActiveStatement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        this.fetchDirection = direction;
        if (currentActiveStatement != null)
            currentActiveStatement.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return fetchDirection;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        this.fetchSize = rows;
        if (currentActiveStatement != null)
            currentActiveStatement.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return resultSetConcurrency;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return resultSetType;
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw unsupportedSelectOnly("addBatch");
    }

    @Override
    public void clearBatch() throws SQLException {
        if (currentActiveStatement != null)
            currentActiveStatement.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw unsupportedSelectOnly("executeBatch");
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        clearCurrentResult();
        return currentActiveStatement != null && currentActiveStatement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return currentActiveStatement != null ? currentActiveStatement.getGeneratedKeys() : null;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw unsupportedSelectOnly("executeUpdate");
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw unsupportedSelectOnly("executeUpdate");
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw unsupportedSelectOnly("executeUpdate");
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return execute(sql);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return execute(sql);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return resultSetHoldability;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return currentActiveStatement != null && currentActiveStatement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        this.poolable = poolable;
        if (currentActiveStatement != null)
            currentActiveStatement.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return poolable;
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        this.closeOnCompletion = true;
        if (currentActiveStatement != null)
            currentActiveStatement.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return closeOnCompletion;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this))
            return iface.cast(this);
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    protected void ensureQuerySql(String sql) throws SQLFeatureNotSupportedException {
        if (!cachePolicy.isQuerySql(sql)) {
            throw unsupportedSelectOnly("executeQuery");
        }
    }

    protected SQLFeatureNotSupportedException unsupportedSelectOnly(String method) {
        return new SQLFeatureNotSupportedException("Only SELECT-like queries are supported by " + method);
    }

    protected void reportExecution(RoutedSql routed,
            SqlCommentParser.ParsedSql parsed,
            String paramFingerprint,
            long startNanos,
            Boolean cacheHit,
            SQLException error) {
        long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        sqlExecutionTraceService.reportExecution(new SqlExecutionTrace(
                routed.datasourceName,
                routed.datasourceType,
                routed.originalSql,
                parsed == null ? null : parsed.cleanSql,
                paramFingerprint,
                error == null,
                cacheHit,
                durationMs,
                error == null ? null : error.getMessage(),
                parsed == null ? null : parsed.metadata));
    }

    protected ResultSet storeCurrentResult(ResultSet result) {
        currentResultSet = result;
        return result;
    }

    protected void clearCurrentResult() {
        currentResultSet = null;
    }
}
