package com.smartbi.query.service;

import com.smartbi.query.api.dto.SqlResponseStubDto;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class QueryResultMapper {

    public SqlResponseStubDto toResponse(ResultSet resultSet, String cube, long durationMs) throws SQLException {
        SqlResponseStubDto response = new SqlResponseStubDto();
        response.setCube(cube);
        response.setDuration(durationMs);
        response.setAffectedRowCount(0);
        response.setPartial(false);
        response.setHitExceptionCache(false);
        response.setStorageCacheUsed(false);

        ResultSetMetaData metaData = resultSet.getMetaData();
        List<SqlResponseStubDto.ColumnMetaStubDto> columns = new ArrayList<SqlResponseStubDto.ColumnMetaStubDto>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            SqlResponseStubDto.ColumnMetaStubDto column = new SqlResponseStubDto.ColumnMetaStubDto();
            column.setAutoIncrement(metaData.isAutoIncrement(i));
            column.setCaseSensitive(metaData.isCaseSensitive(i));
            column.setSearchable(metaData.isSearchable(i));
            column.setCurrency(metaData.isCurrency(i));
            column.setIsNullable(metaData.isNullable(i));
            column.setSigned(metaData.isSigned(i));
            column.setDisplaySize(metaData.getColumnDisplaySize(i));
            column.setLabel(metaData.getColumnLabel(i));
            column.setName(metaData.getColumnName(i));
            column.setSchemaName(metaData.getSchemaName(i));
            column.setCatelogName(metaData.getCatalogName(i));
            column.setTableName(metaData.getTableName(i));
            column.setPrecision(metaData.getPrecision(i));
            column.setScale(metaData.getScale(i));
            column.setColumnType(metaData.getColumnType(i));
            column.setColumnTypeName(metaData.getColumnTypeName(i));
            column.setReadOnly(metaData.isReadOnly(i));
            column.setWritable(metaData.isWritable(i));
            column.setDefinitelyWritable(metaData.isDefinitelyWritable(i));
            columns.add(column);
        }
        response.setColumnMetas(columns);

        List<String[]> rows = new ArrayList<String[]>();
        while (resultSet.next()) {
            String[] row = new String[metaData.getColumnCount()];
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                row[i - 1] = resultSet.getString(i);
            }
            rows.add(row);
        }
        response.setResults(rows);
        response.setTotalScanCount(rows.size());
        return response;
    }

    public SqlResponseStubDto exceptionResponse(String cube, long durationMs, String message) {
        SqlResponseStubDto response = new SqlResponseStubDto();
        response.setCube(cube);
        response.setDuration(durationMs);
        response.setIsException(true);
        response.setExceptionMessage(message);
        response.setStorageCacheUsed(false);
        return response;
    }
}
