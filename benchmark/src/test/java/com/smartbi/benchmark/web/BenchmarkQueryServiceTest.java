package com.smartbi.benchmark.web;

import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.jdbc.JdbcDriverRegistry;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
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

        jdbcUrl = "jdbc:h2:mem:benchmark-query-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        dataSource = new BenchmarkDataSource();
        dataSource.setId(5L);
        dataSource.setName("query-ds");
        dataSource.setDriverClass("org.h2.Driver");
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setJdbcUser("sa");
        dataSource.setJdbcPassword("");

        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha')");
        }

        when(dataSourceRepository.findById(5L)).thenReturn(Optional.of(dataSource));
        when(driverRegistry.openConnection(any(BenchmarkDataSource.class)))
                .thenAnswer(invocation -> DriverManager.getConnection(jdbcUrl, "sa", ""));
    }

    // Covers BenchmarkQueryService#executeQuery result-set shaping.
    @Test
    void shouldReturnHeadersAndRowsForSelectStatements() {
        BenchmarkQueryService.QueryResponse response = service.executeQuery(5L, "SELECT NAME FROM SALES ORDER BY ID");

        assertEquals(1, response.headers.size());
        assertEquals("NAME", response.headers.get(0));
        assertEquals("alpha", response.rows.get(0).get("NAME"));
        assertNull(response.message);
        assertTrue(response.latencyMs >= 0L);
    }

    // Covers BenchmarkQueryService#executeQuery update-count branch.
    @Test
    void shouldReturnAffectedRowsForMutationStatements() {
        BenchmarkQueryService.QueryResponse response = service.executeQuery(5L, "UPDATE SALES SET NAME='beta' WHERE ID=1");

        assertNull(response.headers);
        assertNull(response.rows);
        assertEquals("Affected rows: 1", response.message);
    }

    // Covers BenchmarkQueryService#executeQuery error handling branch.
    @Test
    void shouldReturnErrorMessageWhenJdbcExecutionFails() {
        BenchmarkQueryService.QueryResponse response = service.executeQuery(5L, "SELECT * FROM MISSING_TABLE");

        assertNull(response.headers);
        assertNull(response.rows);
        assertTrue(response.message.startsWith("Error: "));
    }
}
