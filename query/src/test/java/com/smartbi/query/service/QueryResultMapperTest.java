package com.smartbi.query.service;

import com.smartbi.query.api.dto.SqlResponseStubDto;
import com.smartbi.query.support.QueryTestFixtures;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryResultMapperTest {

    private static final String H2_DRIVER_CLASS = "org.h2.Driver";
    private static final String SALES_CREATE_SQL = "CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))";
    private static final String SALES_INSERT_SQL =
            "INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')";
    private static final String SALES_QUERY_SQL = "SELECT ID AS IDENTIFIER, NAME FROM SALES ORDER BY ID";
    private static final String DEFAULT_CUBE = "default";
    private static final String BOOM = "boom";
    private static final String ALPHA = "alpha";
    private static final String IDENTIFIER = "IDENTIFIER";
    private static final String ID = "ID";
    private static final String NAME = "NAME";

    // Covers QueryResultMapper#toResponse happy-path result and metadata mapping.
    @Test
    void shouldMapResultSetRowsAndColumnMetadata() throws Exception {
        Class.forName(H2_DRIVER_CLASS);
        QueryResultMapper mapper = new QueryResultMapper();

        try (Connection connection = DriverManager.getConnection(
                QueryTestFixtures.get("query.test.result-mapper.jdbc-url"),
                QueryTestFixtures.get("query.test.result-mapper.jdbc-user"),
                QueryTestFixtures.get("query.test.result-mapper.jdbc-password"));
             Statement statement = connection.createStatement()) {
            statement.execute(SALES_CREATE_SQL);
            statement.execute(SALES_INSERT_SQL);

            try (ResultSet resultSet = statement.executeQuery(SALES_QUERY_SQL)) {
                SqlResponseStubDto response = mapper.toResponse(resultSet, DEFAULT_CUBE, 18L);

                assertEquals(DEFAULT_CUBE, response.getCube());
                assertEquals(18L, response.getDuration());
                assertEquals(0, response.getAffectedRowCount());
                assertFalse(response.isPartial());
                assertFalse(response.isHitExceptionCache());
                assertFalse(response.isStorageCacheUsed());
                assertEquals(2, response.getTotalScanCount());
                assertEquals(2, response.getResults().size());
                assertEquals("1", response.getResults().get(0)[0]);
                assertEquals(ALPHA, response.getResults().get(0)[1]);
                assertEquals(IDENTIFIER, response.getColumnMetas().get(0).getLabel());
                assertEquals(ID, response.getColumnMetas().get(0).getName());
                assertEquals(NAME, response.getColumnMetas().get(1).getName());
            }
        }
    }

    // Covers QueryResultMapper#exceptionResponse error-path defaults.
    @Test
    void shouldBuildKylinStyleExceptionResponses() {
        QueryResultMapper mapper = new QueryResultMapper();

        SqlResponseStubDto response = mapper.exceptionResponse(DEFAULT_CUBE, 27L, BOOM);

        assertEquals(DEFAULT_CUBE, response.getCube());
        assertEquals(27L, response.getDuration());
        assertTrue(response.getIsException());
        assertEquals(BOOM, response.getExceptionMessage());
        assertFalse(response.isStorageCacheUsed());
    }
}
