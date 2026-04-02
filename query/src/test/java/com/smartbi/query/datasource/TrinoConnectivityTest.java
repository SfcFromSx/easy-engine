package com.smartbi.query.datasource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import com.smartbi.query.EngineQueryApplication;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EngineQueryApplication.class)
@ActiveProfiles("dev") // Use dev profile to get the real docker URL
public class TrinoConnectivityTest {

    @Value("${engine.query.datasource.named.trino_local.jdbc-url}")
    private String trinoJdbcUrl;

    @Value("${engine.query.datasource.named.trino_local.username}")
    private String trinoJdbcUser;

    @Value("${engine.query.datasource.named.trino_local.password:}")
    private String trinoJdbcPassword;

    @Test
    void testTrinoConnectivity() throws Exception {
        System.out.println("Connecting to Trino at: " + trinoJdbcUrl);
        try (Connection connection = DriverManager.getConnection(trinoJdbcUrl, trinoJdbcUser, trinoJdbcPassword);
             Statement statement = connection.createStatement()) {
            
            // Trino has a 'system' schema that's always present.
            try (ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                assertTrue(resultSet.next());
                System.out.println("Successfully connected to Trino and executed SELECT 1");
            }

            // Test TPCH catalog which is usually default.
            try (ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM tpch.tiny.region")) {
                assertTrue(resultSet.next());
                long count = resultSet.getLong(1);
                System.out.println("TPCH Region row count: " + count);
                assertTrue(count > 0);
            }
        }
    }
}
