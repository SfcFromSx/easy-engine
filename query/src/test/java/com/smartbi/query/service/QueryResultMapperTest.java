package com.smartbi.query.service;

import com.smartbi.query.api.dto.SqlResponseStubDto;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryResultMapperTest {

    // Covers QueryResultMapper#toResponse happy-path result and metadata mapping.
    @Test
    void shouldMapResultSetRowsAndColumnMetadata() throws Exception {
        Class.forName("org.h2.Driver");
        QueryResultMapper mapper = new QueryResultMapper();

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:result_mapper;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')");

            try (ResultSet resultSet = statement.executeQuery("SELECT ID AS IDENTIFIER, NAME FROM SALES ORDER BY ID")) {
                SqlResponseStubDto response = mapper.toResponse(resultSet, "default", 18L);

                assertEquals("default", response.getCube());
                assertEquals(18L, response.getDuration());
                assertEquals(0, response.getAffectedRowCount());
                assertFalse(response.isPartial());
                assertFalse(response.isHitExceptionCache());
                assertFalse(response.isStorageCacheUsed());
                assertEquals(2, response.getTotalScanCount());
                assertEquals(2, response.getResults().size());
                assertEquals("1", response.getResults().get(0)[0]);
                assertEquals("alpha", response.getResults().get(0)[1]);
                assertEquals("IDENTIFIER", response.getColumnMetas().get(0).getLabel());
                assertEquals("ID", response.getColumnMetas().get(0).getName());
                assertEquals("NAME", response.getColumnMetas().get(1).getName());
            }
        }
    }

    // Covers QueryResultMapper#exceptionResponse error-path defaults.
    @Test
    void shouldBuildKylinStyleExceptionResponses() {
        QueryResultMapper mapper = new QueryResultMapper();

        SqlResponseStubDto response = mapper.exceptionResponse("default", 27L, "boom");

        assertEquals("default", response.getCube());
        assertEquals(27L, response.getDuration());
        assertTrue(response.getIsException());
        assertEquals("boom", response.getExceptionMessage());
        assertFalse(response.isStorageCacheUsed());
    }
}
