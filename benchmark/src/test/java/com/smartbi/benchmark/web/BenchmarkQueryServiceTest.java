package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.jdbc.JdbcDriverRegistry;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import com.smartbi.benchmark.support.BenchmarkTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BenchmarkQueryServiceTest {

    private static final String H2_DRIVER_CLASS = BenchmarkTestFixtures.get("benchmark.test.query-service.driver-class");
    private static final String JDBC_URL_PREFIX = BenchmarkTestFixtures.get("benchmark.test.query-service.jdbc-url-prefix");
    private static final String JDBC_URL_SUFFIX = BenchmarkTestFixtures.get("benchmark.test.query-service.jdbc-url-suffix");
    private static final String DATASOURCE_NAME = "query-ds";
    private static final String JDBC_USERNAME = BenchmarkTestFixtures.get("benchmark.test.query-service.jdbc-user");
    private static final String JDBC_PASSWORD = BenchmarkTestFixtures.get("benchmark.test.query-service.jdbc-password");
    private static final String SALES_CREATE_SQL = "CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))";
    private static final String SALES_INSERT_SQL = "INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha')";
    private static final String SELECT_SQL = "SELECT NAME FROM SALES ORDER BY ID";
    private static final String UPDATE_SQL = "UPDATE SALES SET NAME='beta' WHERE ID=1";
    private static final String MISSING_TABLE_SQL = "SELECT * FROM MISSING_TABLE";
    private static final String NAME_COLUMN = "NAME";
    private static final String ALPHA = "alpha";
    private static final String AFFECTED_ROWS_MESSAGE = "Affected rows: 1";
    private static final String ERROR_PREFIX = "Error: ";

    private BenchmarkDataSourceRepository dataSourceRepository;
    private JdbcDriverRegistry driverRegistry;
    private BenchmarkQueryService service;
    private BenchmarkDataSource dataSource;
    private String jdbcUrl;

    @BeforeEach
    void setUp() throws Exception {
        dataSourceRepository = mock(BenchmarkDataSourceRepository.class);
        driverRegistry = mock(JdbcDriverRegistry.class);
        service = new BenchmarkQueryService(dataSourceRepository, driverRegistry);

        jdbcUrl = JDBC_URL_PREFIX + UUID.randomUUID() + JDBC_URL_SUFFIX;
        dataSource = new BenchmarkDataSource();
        dataSource.setId(5L);
        dataSource.setName(DATASOURCE_NAME);
        dataSource.setDriverClass(H2_DRIVER_CLASS);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setJdbcUser(JDBC_USERNAME);
        dataSource.setJdbcPassword(JDBC_PASSWORD);

        Class.forName(H2_DRIVER_CLASS);
        try (Connection connection = DriverManager.getConnection(jdbcUrl, JDBC_USERNAME, JDBC_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute(SALES_CREATE_SQL);
            statement.execute(SALES_INSERT_SQL);
        }

        when(dataSourceRepository.findById(5L)).thenReturn(Optional.of(dataSource));
        when(driverRegistry.openConnection(any(BenchmarkDataSource.class)))
                .thenAnswer(invocation -> DriverManager.getConnection(jdbcUrl, JDBC_USERNAME, JDBC_PASSWORD));
    }

    // Covers BenchmarkQueryService#executeQuery result-set shaping.
    @Test
    void shouldReturnHeadersAndRowsForSelectStatements() {
        BenchmarkQueryService.QueryResponse response = service.executeQuery(5L, SELECT_SQL);

        assertEquals(1, response.headers.size());
        assertEquals(NAME_COLUMN, response.headers.get(0));
        assertEquals(ALPHA, response.rows.get(0).get(NAME_COLUMN));
        assertNull(response.message);
        assertTrue(response.latencyMs >= 0L);
    }

    // Covers BenchmarkQueryService#executeQuery update-count branch.
    @Test
    void shouldReturnAffectedRowsForMutationStatements() {
        BenchmarkQueryService.QueryResponse response = service.executeQuery(5L, UPDATE_SQL);

        assertNull(response.headers);
        assertNull(response.rows);
        assertEquals(AFFECTED_ROWS_MESSAGE, response.message);
    }

    // Covers BenchmarkQueryService#executeQuery error handling branch.
    @Test
    void shouldReturnErrorMessageWhenJdbcExecutionFails() {
        BenchmarkQueryService.QueryResponse response = service.executeQuery(5L, MISSING_TABLE_SQL);

        assertNull(response.headers);
        assertNull(response.rows);
        assertTrue(response.message.startsWith(ERROR_PREFIX));
    }
}
