package com.smartbi.benchmark.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.benchmark.BenchmarkApplication;
import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.BenchmarkStrategy;
import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.jdbc.JdbcDriverRegistry;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.report.BenchmarkRunReportService;
import com.smartbi.benchmark.support.BenchmarkTestFixtures;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BenchmarkApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenchmarkAsyncRunnerTrinoJdbcIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TRINO_GATEWAY_HOST = BenchmarkTestFixtures.get("benchmark.test.trino-jdbc-gateway.host");
    private static final int TRINO_GATEWAY_PORT = Integer.parseInt(
            BenchmarkTestFixtures.get("benchmark.test.trino-jdbc-gateway.port"));
    private static final String BENCHMARK_TRINO_JDBC_URL = BenchmarkTestFixtures.get(
            "benchmark.test.trino-jdbc-gateway.jdbc-url");
    private static final String DIRECT_TRINO_URL = BenchmarkTestFixtures.get(
            "benchmark.test.trino-jdbc-gateway.direct-url");
    private static final String INFO_URI_PREFIX = BenchmarkTestFixtures.get(
            "benchmark.test.trino-jdbc-gateway.info-uri-prefix");
    private static final Pattern PREPARE_PATTERN = Pattern.compile(
            "(?is)^PREPARE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+FROM\\s+(.+?)\\s*;?\\s*$");
    private static final Pattern EXECUTE_PATTERN = Pattern.compile(
            "(?is)^EXECUTE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(?:USING\\s+(.+?))?\\s*;?\\s*$");
    private static final Pattern DEALLOCATE_PATTERN = Pattern.compile(
            "(?is)^DEALLOCATE\\s+PREPARE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;?\\s*$");

    @Autowired
    private BenchmarkJobRepository jobRepository;

    @Autowired
    private BenchmarkRunRepository runRepository;

    @Autowired
    private SqlTemplateRepository templateRepository;

    @Autowired
    private BenchmarkTestSetItemRepository testSetItemRepository;

    @Autowired
    private BenchmarkTestSetRepository testSetRepository;

    @Autowired
    private BenchmarkRunReportService reportService;

    @Autowired
    private BenchmarkDataSourceRepository dataSourceRepository;

    @Autowired
    private JdbcDriverRegistry driverRegistry;

    private HttpServer trinoGateway;
    private BenchmarkAsyncRunner runner;

    @BeforeAll
    void startGateway() throws Exception {
        Class.forName("io.trino.jdbc.TrinoDriver");
        trinoGateway = HttpServer.create(new InetSocketAddress(TRINO_GATEWAY_HOST, TRINO_GATEWAY_PORT), 0);
        trinoGateway.createContext("/v1/statement", new TrinoStatementHandler());
        trinoGateway.setExecutor(Executors.newCachedThreadPool());
        trinoGateway.start();
    }

    @AfterAll
    void stopGateway() {
        if (trinoGateway != null) {
            trinoGateway.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        runRepository.deleteAll();
        jobRepository.deleteAll();
        testSetItemRepository.deleteAll();
        testSetRepository.deleteAll();
        templateRepository.deleteAll();
        dataSourceRepository.deleteAll();

        runner = new BenchmarkAsyncRunner(
                jobRepository,
                runRepository,
                templateRepository,
                testSetItemRepository,
                testSetRepository,
                reportService,
                dataSourceRepository,
                driverRegistry
        );
    }

    @Test
    void shouldExecuteStatementBenchmarksThroughTrinoJdbcGateway() throws Exception {
        BenchmarkRun persisted = executeRun("SELECT NAME FROM SALES ORDER BY ID", "STATEMENT", null);

        assertEquals(RunStatus.COMPLETED, persisted.getStatus());
        assertEquals(Long.valueOf(1L), persisted.getTotalQueries());
        assertEquals(Long.valueOf(1L), persisted.getSuccessCount());
        assertEquals(Long.valueOf(0L), persisted.getErrorCount());
    }

    @Test
    void shouldExecutePreparedBenchmarksThroughTrinoJdbcGateway() throws Exception {
        BenchmarkRun persisted = executeRun(
                "SELECT NAME FROM SALES WHERE ID = ?",
                "PREPARED_STATEMENT",
                "[{\"type\":\"INTEGER\",\"value\":1}]");

        assertEquals(RunStatus.COMPLETED, persisted.getStatus());
        assertEquals(Long.valueOf(1L), persisted.getTotalQueries());
        assertEquals(Long.valueOf(1L), persisted.getSuccessCount());
        assertEquals(Long.valueOf(0L), persisted.getErrorCount());

        try (Connection connection = DriverManager.getConnection(DIRECT_TRINO_URL);
             PreparedStatement statement = connection.prepareStatement("SELECT NAME FROM SALES WHERE ID = ?")) {
            statement.setInt(1, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("alpha", resultSet.getString(1));
            }
        }
    }

    private BenchmarkRun executeRun(String sqlText,
                                    String executionMode,
                                    String paramJson) {
        BenchmarkDataSource dataSource = new BenchmarkDataSource();
        dataSource.setName("query-trino-gateway");
        dataSource.setDriverClass("io.trino.jdbc.TrinoDriver");
        dataSource.setJdbcUrl(BENCHMARK_TRINO_JDBC_URL);
        dataSource.setJdbcUser("ADMIN");
        dataSource.setJdbcPassword("");
        dataSource = dataSourceRepository.save(dataSource);

        BenchmarkTestSet testSet = new BenchmarkTestSet();
        testSet.setName("trino-jdbc-gateway");
        testSet = testSetRepository.save(testSet);

        BenchmarkTestSetItem item = new BenchmarkTestSetItem();
        item.setTestSetId(testSet.getId());
        item.setSortOrder(1);
        item.setLabel("case");
        item.setSqlText(sqlText);
        item.setExecutionMode(executionMode);
        item.setParamJson(paramJson);
        testSetItemRepository.save(item);

        BenchmarkJob job = new BenchmarkJob();
        job.setName("trino-gateway-job");
        job.setDataSourceId(dataSource.getId());
        job.setConcurrentThreads(1);
        job.setRounds(1);
        job.setStrategy(BenchmarkStrategy.ROUND_ROBIN);
        job.setTestSetId(testSet.getId());
        job = jobRepository.save(job);

        BenchmarkRun run = new BenchmarkRun();
        run.setJobId(job.getId());
        run.setStatus(RunStatus.RUNNING);
        run = runRepository.save(run);

        runner.executeRun(run.getId());
        return runRepository.findById(run.getId())
                .orElseThrow(() -> new AssertionError("Benchmark run not persisted"));
    }

    private static final class TrinoStatementHandler implements HttpHandler {

        private static final String HEADER_PREPARED_STATEMENT = "X-Trino-Prepared-Statement";
        private static final String HEADER_ADDED_PREPARE = "X-Trino-Added-Prepare";
        private static final String HEADER_DEALLOCATED_PREPARE = "X-Trino-Deallocated-Prepare";

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bodyBytes = readAll(exchange.getRequestBody());
            String sql = new String(bodyBytes, StandardCharsets.UTF_8).trim();
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "application/json");

            Map<String, Object> response = baseResponse();
            String queryId = "bench_trino_" + System.nanoTime();
            response.put("id", queryId);
            response.put("infoUri", INFO_URI_PREFIX + queryId);

            Matcher prepareMatcher = PREPARE_PATTERN.matcher(sql);
            if (prepareMatcher.matches()) {
                response.put("columns", Collections.singletonList(booleanColumn()));
                response.put("data", Collections.emptyList());
                response.put("updateType", "PREPARE");
                response.put("updateCount", Long.valueOf(0L));
                headers.add(HEADER_ADDED_PREPARE,
                        encodePreparedStatement(prepareMatcher.group(1), prepareMatcher.group(2).trim()));
                write(exchange, response);
                return;
            }

            Matcher deallocateMatcher = DEALLOCATE_PATTERN.matcher(sql);
            if (deallocateMatcher.matches()) {
                response.put("columns", Collections.singletonList(booleanColumn()));
                response.put("data", Collections.emptyList());
                response.put("updateType", "DEALLOCATE PREPARE");
                response.put("updateCount", Long.valueOf(0L));
                headers.add(HEADER_DEALLOCATED_PREPARE,
                        URLEncoder.encode(deallocateMatcher.group(1), StandardCharsets.UTF_8));
                write(exchange, response);
                return;
            }

            Matcher executeMatcher = EXECUTE_PATTERN.matcher(sql);
            if (executeMatcher.matches()) {
                String statementName = executeMatcher.group(1);
                String preparedSql = preparedStatements(exchange.getRequestHeaders()).get(statementName);
                if ("SELECT NAME FROM SALES WHERE ID = ?".equals(preparedSql)) {
                    response.put("columns", Collections.singletonList(varcharColumn("NAME")));
                    response.put("data", Collections.singletonList(Collections.<Object>singletonList("alpha")));
                    write(exchange, response);
                    return;
                }
            }

            if ("SELECT NAME FROM SALES ORDER BY ID".equals(sql)) {
                response.put("columns", Collections.singletonList(varcharColumn("NAME")));
                List<List<Object>> rows = new ArrayList<List<Object>>();
                rows.add(Collections.<Object>singletonList("alpha"));
                rows.add(Collections.<Object>singletonList("beta"));
                response.put("data", rows);
                write(exchange, response);
                return;
            }

            response.put("error", errorPayload("Unsupported SQL: " + sql));
            write(exchange, response);
        }

        private Map<String, String> preparedStatements(Headers headers) {
            List<String> values = headers.get(HEADER_PREPARED_STATEMENT);
            if (values == null || values.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, String> statements = new LinkedHashMap<String, String>();
            for (String value : values) {
                int separator = value.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String name = URLDecoder.decode(value.substring(0, separator), StandardCharsets.UTF_8);
                String sql = URLDecoder.decode(value.substring(separator + 1), StandardCharsets.UTF_8);
                statements.put(name, sql);
            }
            return statements;
        }

        private String encodePreparedStatement(String name, String sql) {
            return URLEncoder.encode(name, StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(sql, StandardCharsets.UTF_8);
        }

        private void write(HttpExchange exchange, Map<String, Object> payload) throws IOException {
            byte[] bytes = JSON.writeValueAsBytes(payload);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }

        private byte[] readAll(InputStream inputStream) throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }

        private Map<String, Object> baseResponse() {
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("id", null);
            response.put("infoUri", "");
            response.put("partialCancelUri", null);
            response.put("nextUri", null);
            response.put("columns", null);
            response.put("data", null);
            response.put("stats", finishedStats());
            response.put("error", null);
            response.put("warnings", Collections.emptyList());
            response.put("updateType", null);
            response.put("updateCount", null);
            return response;
        }

        private Map<String, Object> varcharColumn(String name) {
            Map<String, Object> typeSignature = new LinkedHashMap<String, Object>();
            typeSignature.put("rawType", "varchar");

            Map<String, Object> argument = new LinkedHashMap<String, Object>();
            argument.put("kind", "LONG");
            argument.put("value", Long.valueOf(2147483647L));
            typeSignature.put("arguments", Collections.singletonList(argument));

            Map<String, Object> column = new LinkedHashMap<String, Object>();
            column.put("name", name);
            column.put("type", "varchar");
            column.put("typeSignature", typeSignature);
            return column;
        }

        private Map<String, Object> booleanColumn() {
            Map<String, Object> typeSignature = new LinkedHashMap<String, Object>();
            typeSignature.put("rawType", "boolean");
            typeSignature.put("arguments", Collections.emptyList());

            Map<String, Object> column = new LinkedHashMap<String, Object>();
            column.put("name", "result");
            column.put("type", "boolean");
            column.put("typeSignature", typeSignature);
            return column;
        }

        private Map<String, Object> finishedStats() {
            Map<String, Object> stats = new LinkedHashMap<String, Object>();
            stats.put("state", "FINISHED");
            stats.put("queued", Boolean.FALSE);
            stats.put("scheduled", Boolean.TRUE);
            stats.put("progressPercentage", Double.valueOf(100D));
            stats.put("runningPercentage", Double.valueOf(0D));
            stats.put("nodes", Integer.valueOf(1));
            stats.put("totalSplits", Integer.valueOf(1));
            stats.put("queuedSplits", Integer.valueOf(0));
            stats.put("runningSplits", Integer.valueOf(0));
            stats.put("completedSplits", Integer.valueOf(1));
            stats.put("planningTimeMillis", Long.valueOf(0L));
            stats.put("analysisTimeMillis", Long.valueOf(0L));
            stats.put("cpuTimeMillis", Long.valueOf(0L));
            stats.put("wallTimeMillis", Long.valueOf(0L));
            stats.put("queuedTimeMillis", Long.valueOf(0L));
            stats.put("elapsedTimeMillis", Long.valueOf(0L));
            stats.put("finishingTimeMillis", Long.valueOf(0L));
            stats.put("physicalInputTimeMillis", Long.valueOf(0L));
            stats.put("processedRows", Long.valueOf(0L));
            stats.put("processedBytes", Long.valueOf(0L));
            stats.put("physicalInputBytes", Long.valueOf(0L));
            stats.put("physicalWrittenBytes", Long.valueOf(0L));
            stats.put("internalNetworkInputBytes", Long.valueOf(0L));
            stats.put("peakMemoryBytes", Long.valueOf(0L));
            stats.put("spilledBytes", Long.valueOf(0L));
            stats.put("rootStage", null);
            return stats;
        }

        private Map<String, Object> errorPayload(String message) {
            Map<String, Object> failureInfo = new LinkedHashMap<String, Object>();
            failureInfo.put("type", "java.sql.SQLException");
            failureInfo.put("message", message);
            failureInfo.put("cause", null);
            failureInfo.put("suppressed", Collections.emptyList());
            failureInfo.put("stack", Collections.emptyList());
            failureInfo.put("errorInfo", null);
            failureInfo.put("errorLocation", null);

            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("message", message);
            error.put("sqlState", "HY000");
            error.put("errorCode", Integer.valueOf(1));
            error.put("errorName", "GENERIC_INTERNAL_ERROR");
            error.put("errorType", "USER_ERROR");
            error.put("errorLocation", null);
            error.put("failureInfo", failureInfo);
            return error;
        }
    }
}
