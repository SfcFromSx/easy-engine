package com.kylin.cache;

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Types;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * {@link ResultSetSerializer} 的单元测试。
 * 验证 JDBC ResultSet 是否可以转换为 JSON 字节并转换回 CachedResultSet。
 */
public class ResultSetSerializerTest {

    @Test
    public void testSerializationRoundTrip() throws Exception {
        // 模拟一个带有 2 列和 1 行的 ResultSet
        ResultSet mockRs = mock(ResultSet.class);
        java.sql.ResultSetMetaData mockMeta = mock(java.sql.ResultSetMetaData.class);

        when(mockRs.getMetaData()).thenReturn(mockMeta);
        when(mockMeta.getColumnCount()).thenReturn(2);

        when(mockMeta.getColumnName(1)).thenReturn("id");
        when(mockMeta.getColumnLabel(1)).thenReturn("id");
        when(mockMeta.getColumnType(1)).thenReturn(Types.INTEGER);
        when(mockMeta.getColumnTypeName(1)).thenReturn("INTEGER");

        when(mockMeta.getColumnName(2)).thenReturn("name");
        when(mockMeta.getColumnLabel(2)).thenReturn("name");
        when(mockMeta.getColumnType(2)).thenReturn(Types.VARCHAR);
        when(mockMeta.getColumnTypeName(2)).thenReturn("VARCHAR");

        // 模拟 1 行数据
        when(mockRs.next()).thenReturn(true, false);
        when(mockRs.getObject(1)).thenReturn(101);
        when(mockRs.getObject(2)).thenReturn("Kylin");

        // 执行序列化
        byte[] json = ResultSetSerializer.serialize(mockRs);
        assertNotNull(json);

        // 执行反序列化
        CachedResultSet result = ResultSetSerializer.deserialize(json);

        // 验证元数据
        assertEquals(2, result.getMetaData().getColumnCount());
        assertEquals("id", result.getMetaData().getColumnName(1));
        assertEquals(Types.INTEGER, result.getMetaData().getColumnType(1));

        // 验证数据
        assertTrue(result.next());
        assertEquals(101, result.getInt(1));
        assertEquals("Kylin", result.getString(2));
        assertFalse(result.next());
    }

    @Test
    public void testMultiTypeSupport() throws Exception {
        // 测试 BIGINT, DECIMAL, BOOLEAN, FLOAT 等多种类型
        ResultSet mockRs = mock(ResultSet.class);
        java.sql.ResultSetMetaData mockMeta = mock(java.sql.ResultSetMetaData.class);

        when(mockRs.getMetaData()).thenReturn(mockMeta);
        when(mockMeta.getColumnCount()).thenReturn(5);

        // 1. BIGINT
        when(mockMeta.getColumnType(1)).thenReturn(Types.BIGINT);
        when(mockMeta.getColumnName(1)).thenReturn("big");
        when(mockMeta.getColumnLabel(1)).thenReturn("big");
        when(mockMeta.getColumnTypeName(1)).thenReturn("BIGINT");

        // 2. DECIMAL
        when(mockMeta.getColumnType(2)).thenReturn(Types.DECIMAL);
        when(mockMeta.getColumnName(2)).thenReturn("dec");
        when(mockMeta.getColumnLabel(2)).thenReturn("dec");
        when(mockMeta.getColumnTypeName(2)).thenReturn("DECIMAL");

        // 3. BOOLEAN
        when(mockMeta.getColumnType(3)).thenReturn(Types.BOOLEAN);
        when(mockMeta.getColumnName(3)).thenReturn("bool");
        when(mockMeta.getColumnLabel(3)).thenReturn("bool");
        when(mockMeta.getColumnTypeName(3)).thenReturn("BOOLEAN");

        // 4. DOUBLE
        when(mockMeta.getColumnType(4)).thenReturn(Types.DOUBLE);
        when(mockMeta.getColumnName(4)).thenReturn("dbl");
        when(mockMeta.getColumnLabel(4)).thenReturn("dbl");
        when(mockMeta.getColumnTypeName(4)).thenReturn("DOUBLE");

        // 5. FLOAT
        when(mockMeta.getColumnType(5)).thenReturn(Types.FLOAT);
        when(mockMeta.getColumnName(5)).thenReturn("flt");
        when(mockMeta.getColumnLabel(5)).thenReturn("flt");
        when(mockMeta.getColumnTypeName(5)).thenReturn("FLOAT");

        when(mockRs.next()).thenReturn(true, false);
        when(mockRs.getObject(1)).thenReturn(9999999999L);
        when(mockRs.getObject(2)).thenReturn(new BigDecimal("123.456"));
        when(mockRs.getObject(3)).thenReturn(true);
        when(mockRs.getObject(4)).thenReturn(3.14159);
        when(mockRs.getObject(5)).thenReturn(1.1f);

        byte[] data = ResultSetSerializer.serialize(mockRs);
        CachedResultSet result = ResultSetSerializer.deserialize(data);

        assertTrue(result.next());
        assertEquals(9999999999L, result.getLong(1));
        assertEquals(new BigDecimal("123.456"), result.getBigDecimal(2));
        assertTrue(result.getBoolean(3));
        assertEquals(3.14159, result.getDouble(4), 0.00001);
        assertEquals(1.1f, result.getFloat(5), 0.0001);
    }

    @Test
    public void testEmptyResultSet() throws Exception {
        ResultSet mockRs = mock(ResultSet.class);
        java.sql.ResultSetMetaData mockMeta = mock(java.sql.ResultSetMetaData.class);
        when(mockRs.getMetaData()).thenReturn(mockMeta);
        when(mockMeta.getColumnCount()).thenReturn(1);
        when(mockMeta.getColumnName(1)).thenReturn("col");
        when(mockMeta.getColumnType(1)).thenReturn(Types.INTEGER);
        when(mockMeta.getColumnTypeName(1)).thenReturn("INTEGER");
        when(mockMeta.getColumnLabel(1)).thenReturn("col");

        when(mockRs.next()).thenReturn(false);

        byte[] json = ResultSetSerializer.serialize(mockRs);
        CachedResultSet result = ResultSetSerializer.deserialize(json);

        assertFalse(result.next());
    }

    @Test
    public void testNullValues() throws Exception {
        ResultSet mockRs = mock(ResultSet.class);
        java.sql.ResultSetMetaData mockMeta = mock(java.sql.ResultSetMetaData.class);
        when(mockRs.getMetaData()).thenReturn(mockMeta);
        when(mockMeta.getColumnCount()).thenReturn(1);
        when(mockMeta.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(mockMeta.getColumnName(1)).thenReturn("val");
        when(mockMeta.getColumnTypeName(1)).thenReturn("VARCHAR");
        when(mockMeta.getColumnLabel(1)).thenReturn("val");

        when(mockRs.next()).thenReturn(true, false);
        when(mockRs.getObject(1)).thenReturn(null);

        byte[] json = ResultSetSerializer.serialize(mockRs);
        CachedResultSet result = ResultSetSerializer.deserialize(json);

        assertTrue(result.next());
        assertNull(result.getObject(1));
        assertTrue(result.wasNull());
    }
}
