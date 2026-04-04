package com.smartbi.query.web;

import com.smartbi.query.EngineQueryApplication;
import com.smartbi.query.support.QuerySpringTestOverrides;
import com.smartbi.query.support.QueryTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = EngineQueryApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=18092",
                "engine.query.trino.port=18093",
                "QUERY_TEST_DEFAULT_JDBC_URL=jdbc:h2:mem:querytrinojdbcdefault;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "QUERY_TEST_TRINO_JDBC_URL=jdbc:h2:mem:querytrinojdbcnamed;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        })
@Import(QueryTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TrinoJdbcCompatibilityIntegrationTest {

    private static final String H2_DRIVER_CLASS = "org.h2.Driver";
    private static final String TRINO_DRIVER_CLASS = "io.trino.jdbc.TrinoDriver";
    private static final String TRINO_URL = "jdbc:trino://127.0.0.1:18093/test/default?user=ADMIN";

    @Value("${engine.query.datasource.default.jdbc-url}")
    private String defaultJdbcUrl;

    @Value("${engine.query.datasource.default.username}")
    private String defaultJdbcUser;

    @Value("${engine.query.datasource.default.password:}")
    private String defaultJdbcPassword;

    @Value("${engine.query.datasource.named.trino_local.jdbc-url}")
    private String trinoJdbcUrl;

    @Value("${engine.query.datasource.named.trino_local.username}")
    private String trinoJdbcUser;

    @Value("${engine.query.datasource.named.trino_local.password:}")
    private String trinoJdbcPassword;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        QuerySpringTestOverrides.register(registry);
    }

    @BeforeEach
    void setUp() throws Exception {
        Class.forName(H2_DRIVER_CLASS);
        Class.forName(TRINO_DRIVER_CLASS);
        try (Connection connection = DriverManager.getConnection(defaultJdbcUrl, defaultJdbcUser, defaultJdbcPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS SALES");
            statement.execute("CREATE TABLE SALES (ID INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO SALES (ID, NAME) VALUES (1, 'alpha'), (2, 'beta')");
        }
        try (Connection connection = DriverManager.getConnection(trinoJdbcUrl, trinoJdbcUser, trinoJdbcPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS REGION");
            statement.execute("CREATE TABLE REGION (REGIONKEY INT PRIMARY KEY, NAME VARCHAR(32))");
            statement.execute("INSERT INTO REGION (REGIONKEY, NAME) VALUES (1, 'trino-africa'), (2, 'trino-america')");
        }
    }

    @Test
    void shouldExecuteStatementQueriesThroughTrinoJdbcPort() throws Exception {
        try (Connection connection = DriverManager.getConnection(TRINO_URL);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT NAME FROM SALES ORDER BY ID")) {
            assertTrue(resultSet.next());
            assertEquals("alpha", resultSet.getString(1));
            assertTrue(resultSet.next());
            assertEquals("beta", resultSet.getString(1));
        }
    }

    @Test
    void shouldExecutePreparedQueriesThroughTrinoJdbcPort() throws Exception {
        try (Connection connection = DriverManager.getConnection(TRINO_URL);
             PreparedStatement statement = connection.prepareStatement("SELECT NAME FROM SALES WHERE ID = ?")) {
            statement.setInt(1, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("alpha", resultSet.getString(1));
            }
        }
    }

    @Test
    void shouldRouteEngineTaggedQueriesThroughTrinoJdbcPort() throws Exception {
        try (Connection connection = DriverManager.getConnection(TRINO_URL);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "/* ENGINE=trino_local */ SELECT NAME FROM REGION WHERE REGIONKEY = 1")) {
            assertTrue(resultSet.next());
            assertEquals("trino-africa", resultSet.getString(1));
        }
    }

    @Test
    void shouldRejectMissingPreparedStatementsAndUnsupportedUsingLiterals() throws Exception {
        try (Connection connection = DriverManager.getConnection(TRINO_URL);
             Statement statement = connection.createStatement()) {
            SQLException missingPrepared = assertThrows(SQLException.class,
                    () -> statement.executeQuery("EXECUTE missing_stmt USING INTEGER '1'"));
            assertTrue(missingPrepared.getMessage().contains("Prepared statement is missing"));

            SQLException unsupportedLiteral = assertThrows(SQLException.class,
                    () -> statement.executeQuery("EXECUTE IMMEDIATE 'SELECT NAME FROM SALES WHERE ID = ?' USING ARRAY[1]"));
            assertTrue(unsupportedLiteral.getMessage().contains("Unsupported EXECUTE USING literal"));
        }
    }

    @Test
    void shouldRejectNonQuerySqlThroughTrinoJdbcPort() throws Exception {
        try (Connection connection = DriverManager.getConnection(TRINO_URL);
             Statement statement = connection.createStatement()) {
            SQLException error = assertThrows(SQLException.class,
                    () -> statement.executeQuery("DELETE FROM SALES WHERE ID = 1"));
            assertTrue(error.getMessage().contains("Only query SQL is supported"));
        }
    }
}
