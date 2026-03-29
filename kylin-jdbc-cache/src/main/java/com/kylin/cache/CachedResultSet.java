package com.kylin.cache;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * 一个由对象数组列表支持的内存中 {@link ResultSet} 实现。
 * 列元数据（名称、JDBC 类型、类型名称、标签）与行数据一起存储。
 *
 * <p>当下层缓存层在 Redis 中找到查询结果时，将返回此类的实例。
 * 仅完整实现了通用的 JDBC 客户端和框架所使用的 ResultSet 方法子集；
 * 所有其他方法均抛出 {@link SQLFeatureNotSupportedException}。
 */
public class CachedResultSet implements ResultSet {

    private final String[] columnNames;
    private final int[] columnTypes;
    private final String[] columnTypeNames;
    private final String[] columnLabels;
    private final List<Object[]> rows;

    private int cursor = -1;        // 初始位置在第一行之前
    private boolean closed = false;
    private boolean wasNull = false;

    public CachedResultSet(String[] columnNames, int[] columnTypes,
                           String[] columnTypeNames, String[] columnLabels,
                           List<Object[]> rows) {
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
        this.columnTypeNames = columnTypeNames;
        this.columnLabels = columnLabels;
        this.rows = rows;
    }

    // -------------------------------------------------------------------------
    // 导航
    // -------------------------------------------------------------------------

    @Override public boolean next() throws SQLException {
        checkClosed();
        cursor++;
        return cursor < rows.size();
    }

    @Override public void close() { closed = true; }
    @Override public boolean isClosed() { return closed; }
    @Override public boolean isBeforeFirst() { return !rows.isEmpty() && cursor < 0; }
    @Override public boolean isAfterLast() { return !rows.isEmpty() && cursor >= rows.size(); }
    @Override public boolean isFirst() { return !rows.isEmpty() && cursor == 0; }
    @Override public boolean isLast() { return !rows.isEmpty() && cursor == rows.size() - 1; }
    @Override public void beforeFirst() { cursor = -1; }
    @Override public void afterLast() { cursor = rows.isEmpty() ? -1 : rows.size(); }
    @Override public boolean first() {
        if (rows.isEmpty()) {
            cursor = -1;
            return false;
        }
        cursor = 0;
        return true;
    }
    @Override public boolean last() {
        if (rows.isEmpty()) {
            cursor = -1;
            return false;
        }
        cursor = rows.size() - 1;
        return true;
    }
    @Override public int getRow() { return cursor >= 0 && cursor < rows.size() ? cursor + 1 : 0; }

    @Override public boolean absolute(int row) throws SQLException {
        checkClosed();
        if (row == 0) {
            beforeFirst();
            return false;
        }

        int target = row > 0 ? row - 1 : rows.size() + row;
        if (target < 0) {
            beforeFirst();
            return false;
        }
        if (target >= rows.size()) {
            afterLast();
            return false;
        }
        cursor = target;
        return true;
    }
    @Override public boolean relative(int rows) throws SQLException {
        checkClosed();
        int target = cursor + rows;
        if (target < 0) {
            beforeFirst();
            return false;
        }
        if (target >= this.rows.size()) {
            afterLast();
            return false;
        }
        cursor = target;
        return true;
    }
    @Override public boolean previous() throws SQLException {
        checkClosed();
        if (rows.isEmpty()) {
            cursor = -1;
            return false;
        }
        if (cursor > rows.size()) {
            cursor = rows.size();
        }
        if (cursor == rows.size()) {
            cursor = rows.size() - 1;
            return true;
        }
        if (cursor <= 0) {
            beforeFirst();
            return false;
        }
        cursor--;
        return true;
    }

    // -------------------------------------------------------------------------
    // wasNull
    // -------------------------------------------------------------------------

    @Override public boolean wasNull() { return wasNull; }

    // -------------------------------------------------------------------------
    // 列访问器
    // -------------------------------------------------------------------------

    private Object getValue(int columnIndex) throws SQLException {
        checkClosed();
        if (cursor < 0 || cursor >= rows.size()) throw new SQLException("无效的游标位置");
        if (columnIndex < 1 || columnIndex > columnNames.length)
            throw new SQLException("列索引超出范围: " + columnIndex);
        Object val = rows.get(cursor)[columnIndex - 1];
        wasNull = (val == null);
        return val;
    }

    @Override public int findColumn(String columnLabel) throws SQLException {
        for (int i = 0; i < columnLabels.length; i++) {
            if (columnLabels[i].equalsIgnoreCase(columnLabel)) return i + 1;
        }
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].equalsIgnoreCase(columnLabel)) return i + 1;
        }
        throw new SQLException("未找到列: " + columnLabel);
    }


    @Override public String getString(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex); return v == null ? null : v.toString();
    }
    @Override public String getString(String columnLabel) throws SQLException {
        return getString(findColumn(columnLabel));
    }

    @Override public boolean getBoolean(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }
    @Override public boolean getBoolean(String c) throws SQLException { return getBoolean(findColumn(c)); }

    @Override public byte getByte(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex); return v == null ? 0 : ((Number) v).byteValue();
    }
    @Override public byte getByte(String c) throws SQLException { return getByte(findColumn(c)); }

    @Override public short getShort(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex); return v == null ? 0 : ((Number) v).shortValue();
    }
    @Override public short getShort(String c) throws SQLException { return getShort(findColumn(c)); }

    @Override public int getInt(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }
    @Override public int getInt(String c) throws SQLException { return getInt(findColumn(c)); }

    @Override public long getLong(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }
    @Override public long getLong(String c) throws SQLException { return getLong(findColumn(c)); }

    @Override public float getFloat(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return 0f;
        if (v instanceof Number) return ((Number) v).floatValue();
        return Float.parseFloat(v.toString());
    }
    @Override public float getFloat(String c) throws SQLException { return getFloat(findColumn(c)); }

    @Override public double getDouble(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return 0d;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(v.toString());
    }
    @Override public double getDouble(String c) throws SQLException { return getDouble(findColumn(c)); }

    @Override public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        return new BigDecimal(v.toString());
    }
    @Override public BigDecimal getBigDecimal(String c) throws SQLException { return getBigDecimal(findColumn(c)); }
    @Override public BigDecimal getBigDecimal(int i, int scale) throws SQLException {
        BigDecimal bd = getBigDecimal(i); return bd == null ? null : bd.setScale(scale, BigDecimal.ROUND_HALF_UP);
    }
    @Override public BigDecimal getBigDecimal(String c, int scale) throws SQLException { return getBigDecimal(findColumn(c), scale); }

    @Override public Object getObject(int columnIndex) throws SQLException { return getValue(columnIndex); }
    @Override public Object getObject(String c) throws SQLException { return getObject(findColumn(c)); }
    @Override public Object getObject(int i, Map<String, Class<?>> map) throws SQLException { return getObject(i); }
    @Override public Object getObject(String c, Map<String, Class<?>> map) throws SQLException { return getObject(c); }
    @Override public <T> T getObject(int i, Class<T> type) throws SQLException {
        Object v = getObject(i); return type.isInstance(v) ? type.cast(v) : null;
    }
    @Override public <T> T getObject(String c, Class<T> type) throws SQLException { return getObject(findColumn(c), type); }

    @Override public Date getDate(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return null;
        if (v instanceof Date) return (Date) v;
        return Date.valueOf(v.toString());
    }
    @Override public Date getDate(String c) throws SQLException { return getDate(findColumn(c)); }
    @Override public Date getDate(int i, Calendar cal) throws SQLException { return getDate(i); }
    @Override public Date getDate(String c, Calendar cal) throws SQLException { return getDate(c); }

    @Override public Time getTime(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return null;
        if (v instanceof Time) return (Time) v;
        return Time.valueOf(v.toString());
    }
    @Override public Time getTime(String c) throws SQLException { return getTime(findColumn(c)); }
    @Override public Time getTime(int i, Calendar cal) throws SQLException { return getTime(i); }
    @Override public Time getTime(String c, Calendar cal) throws SQLException { return getTime(c); }

    @Override public Timestamp getTimestamp(int columnIndex) throws SQLException {
        Object v = getValue(columnIndex);
        if (v == null) return null;
        if (v instanceof Timestamp) return (Timestamp) v;
        return Timestamp.valueOf(v.toString());
    }
    @Override public Timestamp getTimestamp(String c) throws SQLException { return getTimestamp(findColumn(c)); }
    @Override public Timestamp getTimestamp(int i, Calendar cal) throws SQLException { return getTimestamp(i); }
    @Override public Timestamp getTimestamp(String c, Calendar cal) throws SQLException { return getTimestamp(c); }

    // -------------------------------------------------------------------------
    // 元数据
    // -------------------------------------------------------------------------

    @Override public ResultSetMetaData getMetaData() {
        return new ResultSetMetaData() {
            @Override public int getColumnCount() { return columnNames.length; }
            @Override public String getColumnName(int i) { return columnNames[i - 1]; }
            @Override public String getColumnLabel(int i) { return columnLabels[i - 1]; }
            @Override public int getColumnType(int i) { return columnTypes[i - 1]; }
            @Override public String getColumnTypeName(int i) { return columnTypeNames[i - 1]; }
            @Override public String getTableName(int i) { return ""; }
            @Override public String getSchemaName(int i) { return ""; }
            @Override public String getCatalogName(int i) { return ""; }
            @Override public int getPrecision(int i) { return 0; }
            @Override public int getScale(int i) { return 0; }
            @Override public int isNullable(int i) { return ResultSetMetaData.columnNullableUnknown; }
            @Override public boolean isAutoIncrement(int i) { return false; }
            @Override public boolean isCaseSensitive(int i) { return false; }
            @Override public boolean isSearchable(int i) { return false; }
            @Override public boolean isCurrency(int i) { return false; }
            @Override public boolean isSigned(int i) { return true; }
            @Override public int getColumnDisplaySize(int i) { return 0; }
            @Override public boolean isReadOnly(int i) { return true; }
            @Override public boolean isWritable(int i) { return false; }
            @Override public boolean isDefinitelyWritable(int i) { return false; }
            @Override public String getColumnClassName(int i) { return Object.class.getName(); }
            @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLFeatureNotSupportedException(); }
            @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        };
    }

    // -------------------------------------------------------------------------
    // 不支持的变更 / 流式列
    // -------------------------------------------------------------------------

    private void checkClosed() throws SQLException {
        if (closed) throw new SQLException("ResultSet 已关闭");
    }

    @Override public Statement getStatement() { return null; }
    @Override public int getType() { return TYPE_SCROLL_INSENSITIVE; }
    @Override public int getConcurrency() { return CONCUR_READ_ONLY; }
    @Override public int getFetchDirection() { return FETCH_FORWARD; }
    @Override public void setFetchDirection(int d) { }
    @Override public int getFetchSize() { return rows.size(); }
    @Override public void setFetchSize(int r) { }
    @Override public int getHoldability() { return CLOSE_CURSORS_AT_COMMIT; }

    private <T> T unsupported() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("CachedResultSet 不支持此操作");
    }

    @Override public InputStream getAsciiStream(int i) throws SQLException { return unsupported(); }
    @Override public InputStream getAsciiStream(String c) throws SQLException { return unsupported(); }
    @Override public InputStream getUnicodeStream(int i) throws SQLException { return unsupported(); }
    @Override public InputStream getUnicodeStream(String c) throws SQLException { return unsupported(); }
    @Override public InputStream getBinaryStream(int i) throws SQLException { return unsupported(); }
    @Override public InputStream getBinaryStream(String c) throws SQLException { return unsupported(); }
    @Override public Reader getCharacterStream(int i) throws SQLException { return unsupported(); }
    @Override public Reader getCharacterStream(String c) throws SQLException { return unsupported(); }
    @Override public Reader getNCharacterStream(int i) throws SQLException { return unsupported(); }
    @Override public Reader getNCharacterStream(String c) throws SQLException { return unsupported(); }
    @Override public byte[] getBytes(int i) throws SQLException { return unsupported(); }
    @Override public byte[] getBytes(String c) throws SQLException { return unsupported(); }
    @Override public Blob getBlob(int i) throws SQLException { return unsupported(); }
    @Override public Blob getBlob(String c) throws SQLException { return unsupported(); }
    @Override public Clob getClob(int i) throws SQLException { return unsupported(); }
    @Override public Clob getClob(String c) throws SQLException { return unsupported(); }
    @Override public NClob getNClob(int i) throws SQLException { return unsupported(); }
    @Override public NClob getNClob(String c) throws SQLException { return unsupported(); }
    @Override public Array getArray(int i) throws SQLException { return unsupported(); }
    @Override public Array getArray(String c) throws SQLException { return unsupported(); }
    @Override public Ref getRef(int i) throws SQLException { return unsupported(); }
    @Override public Ref getRef(String c) throws SQLException { return unsupported(); }
    @Override public SQLXML getSQLXML(int i) throws SQLException { return unsupported(); }
    @Override public SQLXML getSQLXML(String c) throws SQLException { return unsupported(); }
    @Override public URL getURL(int i) throws SQLException { return unsupported(); }
    @Override public URL getURL(String c) throws SQLException { return unsupported(); }
    @Override public String getNString(int i) throws SQLException { return getString(i); }
    @Override public String getNString(String c) throws SQLException { return getString(c); }
    @Override public RowId getRowId(int i) throws SQLException { return unsupported(); }
    @Override public RowId getRowId(String c) throws SQLException { return unsupported(); }
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() { }
    @Override public String getCursorName() throws SQLException { return unsupported(); }

    // 变更操作 — 只读结果集
    @Override public boolean rowUpdated() { return false; }
    @Override public boolean rowInserted() { return false; }
    @Override public boolean rowDeleted() { return false; }
    @Override public void insertRow() throws SQLException { unsupported(); }
    @Override public void updateRow() throws SQLException { unsupported(); }
    @Override public void deleteRow() throws SQLException { unsupported(); }
    @Override public void refreshRow() throws SQLException { unsupported(); }
    @Override public void cancelRowUpdates() throws SQLException { unsupported(); }
    @Override public void moveToInsertRow() throws SQLException { unsupported(); }
    @Override public void moveToCurrentRow() throws SQLException { unsupported(); }
    @Override public void updateNull(int i) throws SQLException { unsupported(); }
    @Override public void updateNull(String c) throws SQLException { unsupported(); }
    @Override public void updateBoolean(int i, boolean x) throws SQLException { unsupported(); }
    @Override public void updateBoolean(String c, boolean x) throws SQLException { unsupported(); }
    @Override public void updateByte(int i, byte x) throws SQLException { unsupported(); }
    @Override public void updateByte(String c, byte x) throws SQLException { unsupported(); }
    @Override public void updateShort(int i, short x) throws SQLException { unsupported(); }
    @Override public void updateShort(String c, short x) throws SQLException { unsupported(); }
    @Override public void updateInt(int i, int x) throws SQLException { unsupported(); }
    @Override public void updateInt(String c, int x) throws SQLException { unsupported(); }
    @Override public void updateLong(int i, long x) throws SQLException { unsupported(); }
    @Override public void updateLong(String c, long x) throws SQLException { unsupported(); }
    @Override public void updateFloat(int i, float x) throws SQLException { unsupported(); }
    @Override public void updateFloat(String c, float x) throws SQLException { unsupported(); }
    @Override public void updateDouble(int i, double x) throws SQLException { unsupported(); }
    @Override public void updateDouble(String c, double x) throws SQLException { unsupported(); }
    @Override public void updateBigDecimal(int i, BigDecimal x) throws SQLException { unsupported(); }
    @Override public void updateBigDecimal(String c, BigDecimal x) throws SQLException { unsupported(); }
    @Override public void updateString(int i, String x) throws SQLException { unsupported(); }
    @Override public void updateString(String c, String x) throws SQLException { unsupported(); }
    @Override public void updateBytes(int i, byte[] x) throws SQLException { unsupported(); }
    @Override public void updateBytes(String c, byte[] x) throws SQLException { unsupported(); }
    @Override public void updateDate(int i, Date x) throws SQLException { unsupported(); }
    @Override public void updateDate(String c, Date x) throws SQLException { unsupported(); }
    @Override public void updateTime(int i, Time x) throws SQLException { unsupported(); }
    @Override public void updateTime(String c, Time x) throws SQLException { unsupported(); }
    @Override public void updateTimestamp(int i, Timestamp x) throws SQLException { unsupported(); }
    @Override public void updateTimestamp(String c, Timestamp x) throws SQLException { unsupported(); }
    @Override public void updateAsciiStream(int i, InputStream x, int len) throws SQLException { unsupported(); }
    @Override public void updateAsciiStream(String c, InputStream x, int len) throws SQLException { unsupported(); }
    @Override public void updateAsciiStream(int i, InputStream x, long len) throws SQLException { unsupported(); }
    @Override public void updateAsciiStream(String c, InputStream x, long len) throws SQLException { unsupported(); }
    @Override public void updateAsciiStream(int i, InputStream x) throws SQLException { unsupported(); }
    @Override public void updateAsciiStream(String c, InputStream x) throws SQLException { unsupported(); }
    @Override public void updateBinaryStream(int i, InputStream x, int len) throws SQLException { unsupported(); }
    @Override public void updateBinaryStream(String c, InputStream x, int len) throws SQLException { unsupported(); }
    @Override public void updateBinaryStream(int i, InputStream x, long len) throws SQLException { unsupported(); }
    @Override public void updateBinaryStream(String c, InputStream x, long len) throws SQLException { unsupported(); }
    @Override public void updateBinaryStream(int i, InputStream x) throws SQLException { unsupported(); }
    @Override public void updateBinaryStream(String c, InputStream x) throws SQLException { unsupported(); }
    @Override public void updateCharacterStream(int i, Reader x, int len) throws SQLException { unsupported(); }
    @Override public void updateCharacterStream(String c, Reader x, int len) throws SQLException { unsupported(); }
    @Override public void updateCharacterStream(int i, Reader x, long len) throws SQLException { unsupported(); }
    @Override public void updateCharacterStream(String c, Reader x, long len) throws SQLException { unsupported(); }
    @Override public void updateCharacterStream(int i, Reader x) throws SQLException { unsupported(); }
    @Override public void updateCharacterStream(String c, Reader x) throws SQLException { unsupported(); }
    @Override public void updateNCharacterStream(int i, Reader x, long len) throws SQLException { unsupported(); }
    @Override public void updateNCharacterStream(String c, Reader x, long len) throws SQLException { unsupported(); }
    @Override public void updateNCharacterStream(int i, Reader x) throws SQLException { unsupported(); }
    @Override public void updateNCharacterStream(String c, Reader x) throws SQLException { unsupported(); }
    @Override public void updateObject(int i, Object x, int s) throws SQLException { unsupported(); }
    @Override public void updateObject(int i, Object x) throws SQLException { unsupported(); }
    @Override public void updateObject(String c, Object x, int s) throws SQLException { unsupported(); }
    @Override public void updateObject(String c, Object x) throws SQLException { unsupported(); }
    @Override public void updateRef(int i, Ref x) throws SQLException { unsupported(); }
    @Override public void updateRef(String c, Ref x) throws SQLException { unsupported(); }
    @Override public void updateBlob(int i, Blob x) throws SQLException { unsupported(); }
    @Override public void updateBlob(String c, Blob x) throws SQLException { unsupported(); }
    @Override public void updateBlob(int i, InputStream x, long len) throws SQLException { unsupported(); }
    @Override public void updateBlob(String c, InputStream x, long len) throws SQLException { unsupported(); }
    @Override public void updateBlob(int i, InputStream x) throws SQLException { unsupported(); }
    @Override public void updateBlob(String c, InputStream x) throws SQLException { unsupported(); }
    @Override public void updateClob(int i, Clob x) throws SQLException { unsupported(); }
    @Override public void updateClob(String c, Clob x) throws SQLException { unsupported(); }
    @Override public void updateClob(int i, Reader x, long len) throws SQLException { unsupported(); }
    @Override public void updateClob(String c, Reader x, long len) throws SQLException { unsupported(); }
    @Override public void updateClob(int i, Reader x) throws SQLException { unsupported(); }
    @Override public void updateClob(String c, Reader x) throws SQLException { unsupported(); }
    @Override public void updateNClob(int i, NClob x) throws SQLException { unsupported(); }
    @Override public void updateNClob(String c, NClob x) throws SQLException { unsupported(); }
    @Override public void updateNClob(int i, Reader x, long len) throws SQLException { unsupported(); }
    @Override public void updateNClob(String c, Reader x, long len) throws SQLException { unsupported(); }
    @Override public void updateNClob(int i, Reader x) throws SQLException { unsupported(); }
    @Override public void updateNClob(String c, Reader x) throws SQLException { unsupported(); }
    @Override public void updateArray(int i, Array x) throws SQLException { unsupported(); }
    @Override public void updateArray(String c, Array x) throws SQLException { unsupported(); }
    @Override public void updateRowId(int i, RowId x) throws SQLException { unsupported(); }
    @Override public void updateRowId(String c, RowId x) throws SQLException { unsupported(); }
    @Override public void updateSQLXML(int i, SQLXML x) throws SQLException { unsupported(); }
    @Override public void updateSQLXML(String c, SQLXML x) throws SQLException { unsupported(); }
    @Override public void updateNString(int i, String x) throws SQLException { unsupported(); }
    @Override public void updateNString(String c, String x) throws SQLException { unsupported(); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { return unsupported(); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
