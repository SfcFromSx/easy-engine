package com.kylin.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 {@link ResultSet} 序列化为 JSON 字节数组，并将其反序列化回
 * 内存中的 {@link CachedResultSet}。
 *
 * <p>JSON 结构：
 * <pre>
 * {
 *   "columns": [
 *     {"name": "col1", "type": 12, "typeName": "VARCHAR", "label": "col1"}
 *   ],
 *   "rows": [
 *     ["value1", 42, null]
 *   ]
 * }
 * </pre>
 */
public class ResultSetSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 将给定的 {@link ResultSet} 序列化为 JSON 字节数组。
     * 调用者负责关闭 ResultSet。
     */
    public static byte[] serialize(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        ObjectNode root = MAPPER.createObjectNode();

        // 列元数据
        ArrayNode columns = root.putArray("columns");
        for (int i = 1; i <= colCount; i++) {
            ObjectNode col = columns.addObject();
            col.put("name", meta.getColumnName(i));
            col.put("type", meta.getColumnType(i));
            col.put("typeName", meta.getColumnTypeName(i));
            col.put("label", meta.getColumnLabel(i));
        }

        // 行数据
        ArrayNode rows = root.putArray("rows");
        while (rs.next()) {
            ArrayNode row = rows.addArray();
            for (int i = 1; i <= colCount; i++) {
                Object val = rs.getObject(i);
                if (val == null) {
                    row.addNull();
                } else {
                    int sqlType = meta.getColumnType(i);
                    appendValue(row, val, sqlType);
                }
            }
        }

        return MAPPER.writeValueAsBytes(root);
    }

    /**
     * 将 JSON 字节数组反序列化为 {@link CachedResultSet}。
     */
    public static CachedResultSet deserialize(byte[] data) throws Exception {
        ObjectNode root = (ObjectNode) MAPPER.readTree(data);
        ArrayNode columnsNode = (ArrayNode) root.get("columns");
        ArrayNode rowsNode = (ArrayNode) root.get("rows");

        int colCount = columnsNode.size();
        String[] names = new String[colCount];
        int[] types = new int[colCount];
        String[] typeNames = new String[colCount];
        String[] labels = new String[colCount];

        for (int i = 0; i < colCount; i++) {
            ObjectNode col = (ObjectNode) columnsNode.get(i);
            names[i]     = col.get("name").asText();
            types[i]     = col.get("type").asInt();
            typeNames[i] = col.get("typeName").asText();
            labels[i]    = col.get("label").asText();
        }

        List<Object[]> rows = new ArrayList<>();
        for (int r = 0; r < rowsNode.size(); r++) {
            ArrayNode rowNode = (ArrayNode) rowsNode.get(r);
            Object[] row = new Object[colCount];
            for (int c = 0; c < colCount; c++) {
                row[c] = extractValue(rowNode.get(c), types[c]);
            }
            rows.add(row);
        }

        return new CachedResultSet(names, types, typeNames, labels, rows);
    }

    // -------------------------------------------------------------------------
    // 助手方法
    // -------------------------------------------------------------------------

    private static void appendValue(ArrayNode row, Object val, int sqlType) {
        switch (sqlType) {
            case Types.BOOLEAN:
            case Types.BIT:
                if (val instanceof Boolean) { row.add((Boolean) val); break; }
                row.add(Boolean.parseBoolean(val.toString())); break;
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                if (val instanceof Number) { row.add(((Number) val).intValue()); break; }
                row.add(Integer.parseInt(val.toString())); break;
            case Types.BIGINT:
                if (val instanceof Number) { row.add(((Number) val).longValue()); break; }
                row.add(Long.parseLong(val.toString())); break;
            case Types.FLOAT:
            case Types.REAL:
                if (val instanceof Number) { row.add(((Number) val).floatValue()); break; }
                row.add(Float.parseFloat(val.toString())); break;
            case Types.DOUBLE:
                if (val instanceof Number) { row.add(((Number) val).doubleValue()); break; }
                row.add(Double.parseDouble(val.toString())); break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                row.add(val.toString()); break;
            default:
                row.add(val.toString());
        }
    }

    private static Object extractValue(com.fasterxml.jackson.databind.JsonNode node, int sqlType) {
        if (node == null || node.isNull()) return null;
        switch (sqlType) {
            case Types.BOOLEAN:
            case Types.BIT:
                return node.asBoolean();
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return node.asInt();
            case Types.BIGINT:
                return node.asLong();
            case Types.FLOAT:
            case Types.REAL:
                return (float) node.asDouble();
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return new java.math.BigDecimal(node.asText());
            default:
                return node.asText();
        }
    }
}
