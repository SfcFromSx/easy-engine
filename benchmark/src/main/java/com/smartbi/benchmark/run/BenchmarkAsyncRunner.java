package com.smartbi.benchmark.run;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.BenchmarkStrategy;
import com.smartbi.benchmark.domain.BenchmarkTestSet;
import com.smartbi.benchmark.domain.BenchmarkTestSetItem;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.domain.SqlExecutionMode;
import com.smartbi.benchmark.domain.SqlTemplate;
import com.smartbi.benchmark.jdbc.JdbcDriverRegistry;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import com.smartbi.benchmark.repo.BenchmarkJobRepository;
import com.smartbi.benchmark.repo.BenchmarkRunRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetItemRepository;
import com.smartbi.benchmark.repo.BenchmarkTestSetRepository;
import com.smartbi.benchmark.repo.SqlTemplateRepository;
import com.smartbi.benchmark.report.BenchmarkRunReportService;
import com.smartbi.benchmark.domain.BenchmarkDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BenchmarkAsyncRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkAsyncRunner.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern ROUTED_ENGINE_BLOCK =
            Pattern.compile("/\\*\\s*YH_TARGET_ENGINE\\s*=\\s*([A-Za-z0-9_.:-]+)\\s*\\*/",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ROUTED_ENGINE_LINE =
            Pattern.compile("(?m)^\\s*--\\s*engine\\s*=\\s*([A-Za-z0-9_.:-]+)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private final BenchmarkJobRepository jobRepository;
    private final BenchmarkRunRepository runRepository;
    private final SqlTemplateRepository templateRepository;
    private final BenchmarkTestSetItemRepository testSetItemRepository;
    private final BenchmarkTestSetRepository testSetRepository;
    private final BenchmarkRunReportService reportService;

    private final BenchmarkDataSourceRepository dataSourceRepository;
    private final JdbcDriverRegistry driverRegistry;

    public BenchmarkAsyncRunner(BenchmarkJobRepository jobRepository,
                                BenchmarkRunRepository runRepository,
                                SqlTemplateRepository templateRepository,
                                BenchmarkTestSetItemRepository testSetItemRepository,
                                BenchmarkTestSetRepository testSetRepository,
                                BenchmarkRunReportService reportService,
                                BenchmarkDataSourceRepository dataSourceRepository,
                                JdbcDriverRegistry driverRegistry) {
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
        this.templateRepository = templateRepository;
        this.testSetItemRepository = testSetItemRepository;
        this.testSetRepository = testSetRepository;
        this.reportService = reportService;
        this.dataSourceRepository = dataSourceRepository;
        this.driverRegistry = driverRegistry;
    }

    @Async
    public void executeRun(long runId) {
        BenchmarkRun run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            return;
        }

        BenchmarkJob job = jobRepository.findById(run.getJobId()).orElse(null);
        if (job == null) {
            failRun(run, "Job configuration missing (ID=" + run.getJobId() + ")", null,
                    Collections.<SqlExecutionMode>emptyList());
            return;
        }

        try {
            runInternal(run, job);
        } catch (Exception e) {
            log.error("Fatal error executing benchmark run {}", runId, e);
            failRun(run, "Execution failed: " + e.getMessage(), job, Collections.<SqlExecutionMode>emptyList());
        }
    }

    private void runInternal(BenchmarkRun run, BenchmarkJob job) throws Exception {
        List<WeightedSql> sources = loadSources(job);
        List<SqlExecutionMode> executionModes = extractExecutionModes(sources);
        if (sources.isEmpty()) {
            failRun(run, "No SQL sources available for this job", job, executionModes);
            return;
        }

        int total = Math.max(1, job.getRounds());
        int threads = Math.max(1, job.getConcurrentThreads());

        run.setTotalTarget(total);
        run.setCurrentProgress(0);
        runRepository.save(run);

        BenchmarkDataSource dsDetail = dataSourceRepository.findById(job.getDataSourceId())
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + job.getDataSourceId()));

        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setDataSource(driverRegistry.createDataSource(dsDetail));
        config.setMaximumPoolSize(threads);
        config.setPoolName("BenchPool-" + run.getId());
        config.setConnectionTimeout(10000);

        try (com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource(config)) {
            long wallStart = System.currentTimeMillis();
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(total);
            List<Long> latencies = new CopyOnWriteArrayList<Long>();
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger errors = new AtomicInteger();
            AtomicInteger completed = new AtomicInteger();
            AtomicReference<String> firstError = new AtomicReference<String>();
            ConcurrentMap<String, FailureGroupAccumulator> failureGroups = new ConcurrentHashMap<String, FailureGroupAccumulator>();

            Random rnd = new Random();
            int[] cumulative = buildCumulative(sources);
            AtomicInteger rr = new AtomicInteger();

            for (int i = 0; i < total; i++) {
                final long rid = run.getId();
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            WeightedSql selected = pickSql(job.getStrategy(), sources, cumulative, rnd, rr);
                            String sql = selected.sqlText;
                            if (job.getStrategy() == BenchmarkStrategy.CACHE_PENETRATION) {
                                sql = "-- no-cache\n" + sql;
                            }
                            long t0 = System.nanoTime();
                            try (Connection c = ds.getConnection()) {
                                executeAndDrain(c, sql, selected);
                                latencies.add((System.nanoTime() - t0) / 1_000_000L);
                                successes.incrementAndGet();
                            } catch (Exception ex) {
                                errors.incrementAndGet();
                                String sampleMessage = formatFailureMessage(ex);
                                firstError.compareAndSet(null, sampleMessage);
                                recordFailure(failureGroups, selected, sampleMessage);
                            }
                        } finally {
                            latch.countDown();
                            int c = completed.incrementAndGet();
                            if (c % 20 == 0 || c == total) {
                                updateProgress(rid, c);
                            }
                        }
                    }
                });
            }

            try {
                latch.await(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                pool.shutdownNow();
            }

            long wall = System.currentTimeMillis() - wallStart;
            finalizeRun(run, job, wall, total, successes.get(), errors.get(), latencies, firstError.get(),
                    sources.size(), executionModes, summarizeFailureGroups(failureGroups));
        } catch (Exception e) {
            log.error("Fatal error executing benchmark run {}", run.getId(), e);
            failRun(run, "Execution failed: " + e.getMessage(), job, executionModes);
        }
    }

    private void updateProgress(long runId, int completed) {
        try {
            runRepository.updateProgress(runId, completed);
        } catch (Exception e) {
            log.warn("Failed to update progress for run {}: {}", runId, e.getMessage());
        }
    }

    private void finalizeRun(BenchmarkRun run, BenchmarkJob job, long wall, int total,
                             int successes, int errors, List<Long> latencies,
                             String errorSample, int sourceCount, List<SqlExecutionMode> executionModes,
                             List<Map<String, Object>> failureBreakdown) {
        run.setEndedAt(Instant.now());
        run.setCurrentProgress(total);
        run.setTotalTarget(total);
        run.setTotalQueries((long) total);
        run.setSuccessCount((long) successes);
        run.setErrorCount((long) errors);
        run.setDurationMs(wall);

        if (wall > 0) {
            run.setQps((double) successes * 1000D / wall);
        }

        List<Long> sorted = new ArrayList<Long>(latencies);
        Collections.sort(sorted);
        if (!sorted.isEmpty()) {
            run.setP50Ms(percentile(sorted, 0.50));
            run.setP95Ms(percentile(sorted, 0.95));
            run.setP99Ms(percentile(sorted, 0.99));
        }

        run.setErrorSample(errorSample);
        run.setStatus(errors == total ? RunStatus.FAILED : RunStatus.COMPLETED);

        String testSetName = resolveTestSetName(job.getTestSetId());
        run.setJobSnapshotJson(reportService.buildJobSnapshotJson(job, sourceCount, testSetName, executionModes));
        run.setEvaluationJson(reportService.buildCompletedEvaluation(
                job, run, sourceCount, executionModes, failureBreakdown));

        runRepository.save(run);
        log.info("Benchmark run {} completed: success={} errors={} wallMs={}",
                run.getId(), successes, errors, wall);
    }

    private List<WeightedSql> loadSources(BenchmarkJob job) {
        if (job.getTestSetId() != null) {
            List<BenchmarkTestSetItem> items = testSetItemRepository.findByTestSetIdOrderBySortOrderAsc(job.getTestSetId());
            List<WeightedSql> list = new ArrayList<WeightedSql>();
            for (BenchmarkTestSetItem it : items) {
                list.add(new WeightedSql(
                        it.getSqlText(),
                        it.getWeight(),
                        SqlExecutionMode.from(it.getExecutionMode()),
                        parseParams(it.getParamJson()),
                        it.getLabel()));
            }
            return list;
        }

        List<SqlTemplate> templates = templateRepository.findAllByOrderByIdAsc();
        List<WeightedSql> list = new ArrayList<WeightedSql>();
        for (SqlTemplate t : templates) {
            list.add(new WeightedSql(
                    t.getSqlText(),
                    t.getWeight(),
                    SqlExecutionMode.from(t.getExecutionMode()),
                    parseParams(t.getParamJson()),
                    t.getName()));
        }
        return list;
    }

    private void failRun(BenchmarkRun run, String msg, BenchmarkJob job, List<SqlExecutionMode> executionModes) {
        run.setStatus(RunStatus.FAILED);
        run.setEndedAt(Instant.now());
        run.setErrorSample(msg);
        if (job != null) {
            run.setJobSnapshotJson(reportService.buildJobSnapshotJson(job, 0, resolveTestSetName(job.getTestSetId()),
                    executionModes));
        }
        run.setEvaluationJson(reportService.buildFailureEvaluation("SETUP", msg, job, executionModes,
                Collections.<Map<String, Object>>emptyList()));
        runRepository.save(run);
    }

    private static List<SqlExecutionMode> extractExecutionModes(List<WeightedSql> sources) {
        List<SqlExecutionMode> executionModes = new ArrayList<SqlExecutionMode>();
        for (WeightedSql source : sources) {
            executionModes.add(source.executionMode);
        }
        return executionModes;
    }

    private String resolveTestSetName(Long testSetId) {
        if (testSetId == null) {
            return null;
        }
        return testSetRepository.findById(testSetId).map(BenchmarkTestSet::getName).orElse(null);
    }

    private static int[] buildCumulative(List<WeightedSql> sources) {
        int[] cumulative = new int[sources.size()];
        int sum = 0;
        for (int i = 0; i < sources.size(); i++) {
            sum += Math.max(1, sources.get(i).weight);
            cumulative[i] = sum;
        }
        return cumulative;
    }

    private static WeightedSql pickSql(BenchmarkStrategy strategy,
                                       List<WeightedSql> sources,
                                       int[] cumulative,
                                       Random rnd,
                                       AtomicInteger rr) {
        if (strategy == BenchmarkStrategy.ROUND_ROBIN) {
            int i = Math.floorMod(rr.getAndIncrement(), sources.size());
            return sources.get(i);
        }
        int max = cumulative[cumulative.length - 1];
        int pick = rnd.nextInt(max);
        for (int i = 0; i < cumulative.length; i++) {
            if (pick < cumulative[i]) {
                return sources.get(i);
            }
        }
        return sources.get(sources.size() - 1);
    }

    private static void recordFailure(ConcurrentMap<String, FailureGroupAccumulator> failures,
                                      WeightedSql selected,
                                      String sampleMessage) {
        String label = normalizeLabel(selected.label);
        String executionMode = resolveExecutionMode(selected.executionMode);
        String routedTarget = extractRoutedTarget(selected.sqlText);
        String groupKey = label + "|" + executionMode + "|" + routedTarget;
        failures.compute(groupKey, (key, current) -> {
            if (current == null) {
                return new FailureGroupAccumulator(key, label, executionMode, routedTarget, sampleMessage);
            }
            current.increment();
            return current;
        });
    }

    private static List<Map<String, Object>> summarizeFailureGroups(
            ConcurrentMap<String, FailureGroupAccumulator> failures) {
        if (failures.isEmpty()) {
            return Collections.emptyList();
        }
        List<FailureGroupAccumulator> accumulators = new ArrayList<FailureGroupAccumulator>(failures.values());
        accumulators.sort(Comparator
                .comparingInt(FailureGroupAccumulator::count).reversed()
                .thenComparing(FailureGroupAccumulator::label)
                .thenComparing(FailureGroupAccumulator::executionMode)
                .thenComparing(FailureGroupAccumulator::routedTarget));

        List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
        for (FailureGroupAccumulator accumulator : accumulators) {
            groups.add(accumulator.asMap());
        }
        return groups;
    }

    private static String formatFailureMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return ex.getClass().getSimpleName();
        }
        return message.trim();
    }

    private static String normalizeLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "(unlabeled)";
        }
        return label.trim();
    }

    private static String resolveExecutionMode(SqlExecutionMode executionMode) {
        SqlExecutionMode resolved = executionMode == null ? SqlExecutionMode.STATEMENT : executionMode;
        return resolved.name();
    }

    private static String extractRoutedTarget(String sqlText) {
        if (sqlText == null || sqlText.trim().isEmpty()) {
            return "default";
        }
        Matcher blockMatcher = ROUTED_ENGINE_BLOCK.matcher(sqlText);
        if (blockMatcher.find()) {
            return blockMatcher.group(1);
        }
        Matcher lineMatcher = ROUTED_ENGINE_LINE.matcher(sqlText);
        if (lineMatcher.find()) {
            return lineMatcher.group(1);
        }
        return "default";
    }

    private static void executeAndDrain(Connection connection, String sql, WeightedSql selected) throws Exception {
        if (selected.executionMode == SqlExecutionMode.PREPARED_STATEMENT) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindParams(statement, selected.params);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) { /* drain */ }
                }
            }
            return;
        }

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) { /* drain */ }
        }
    }

    private static void bindParams(PreparedStatement statement, List<PreparedParam> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            PreparedParam param = params.get(i);
            int index = i + 1;
            if (param == null || param.value == null) {
                statement.setObject(index, null);
                continue;
            }

            String type = param.type == null ? "" : param.type.toUpperCase(Locale.ROOT);
            Object value = param.value;
            if ("INTEGER".equals(type) || "INT".equals(type)) {
                statement.setInt(index, ((Number) value).intValue());
            } else if ("LONG".equals(type) || "BIGINT".equals(type)) {
                statement.setLong(index, ((Number) value).longValue());
            } else if ("DOUBLE".equals(type)) {
                statement.setDouble(index, ((Number) value).doubleValue());
            } else if ("FLOAT".equals(type)) {
                statement.setFloat(index, ((Number) value).floatValue());
            } else if ("DECIMAL".equals(type) || "BIGDECIMAL".equals(type)) {
                statement.setBigDecimal(index, new BigDecimal(String.valueOf(value)));
            } else if ("BOOLEAN".equals(type)) {
                statement.setBoolean(index, (Boolean) value);
            } else if ("DATE".equals(type)) {
                statement.setDate(index, Date.valueOf(String.valueOf(value)));
            } else if ("TIME".equals(type)) {
                statement.setTime(index, Time.valueOf(String.valueOf(value)));
            } else if ("TIMESTAMP".equals(type)) {
                statement.setTimestamp(index, Timestamp.valueOf(String.valueOf(value)));
            } else {
                statement.setObject(index, value);
            }
        }
    }

    private static List<PreparedParam> parseParams(String paramJson) {
        if (paramJson == null || paramJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JsonNode node = JSON.readTree(paramJson);
            if (!node.isArray()) {
                return Collections.emptyList();
            }

            List<PreparedParam> out = new ArrayList<PreparedParam>();
            for (JsonNode item : node) {
                if (item == null || item.isNull()) {
                    out.add(new PreparedParam("OBJECT", null));
                    continue;
                }
                if (item.isObject()) {
                    JsonNode valueNode = item.get("value");
                    String type = item.hasNonNull("type") ? item.get("type").asText() : inferType(valueNode);
                    out.add(new PreparedParam(type, convertJsonValue(valueNode)));
                } else {
                    out.add(new PreparedParam(inferType(item), convertJsonValue(item)));
                }
            }
            return out;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid param_json: " + ex.getMessage(), ex);
        }
    }

    private static String inferType(JsonNode node) {
        if (node == null || node.isNull()) {
            return "OBJECT";
        }
        if (node.isInt()) {
            return "INTEGER";
        }
        if (node.isLong()) {
            return "LONG";
        }
        if (node.isFloatingPointNumber()) {
            return "DOUBLE";
        }
        if (node.isBoolean()) {
            return "BOOLEAN";
        }
        return "STRING";
    }

    private static Object convertJsonValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isInt()) {
            return node.intValue();
        }
        if (node.isLong()) {
            return node.longValue();
        }
        if (node.isFloatingPointNumber()) {
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }

    private static double percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return 0.0;
        if (sorted.size() == 1) return sorted.get(0).doubleValue();

        double pos = p * (sorted.size() - 1);
        int lower = (int) Math.floor(pos);
        int upper = (int) Math.ceil(pos);
        if (lower == upper) return sorted.get(lower).doubleValue();

        double weight = pos - lower;
        return sorted.get(lower) * (1 - weight) + sorted.get(upper) * weight;
    }

    private static final class WeightedSql {
        final String sqlText;
        final int weight;
        final SqlExecutionMode executionMode;
        final List<PreparedParam> params;
        final String label;

        WeightedSql(String sqlText, int weight, SqlExecutionMode executionMode, List<PreparedParam> params, String label) {
            this.sqlText = sqlText;
            this.weight = weight;
            this.executionMode = executionMode;
            this.params = params;
            this.label = label;
        }
    }

    private static final class PreparedParam {
        final String type;
        final Object value;

        PreparedParam(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private static final class FailureGroupAccumulator {
        private final String groupKey;
        private final String label;
        private final String executionMode;
        private final String routedTarget;
        private final String sampleMessage;
        private final AtomicInteger failureCount = new AtomicInteger(1);

        FailureGroupAccumulator(String groupKey,
                                String label,
                                String executionMode,
                                String routedTarget,
                                String sampleMessage) {
            this.groupKey = groupKey;
            this.label = label;
            this.executionMode = executionMode;
            this.routedTarget = routedTarget;
            this.sampleMessage = sampleMessage;
        }

        void increment() {
            failureCount.incrementAndGet();
        }

        int count() {
            return failureCount.get();
        }

        String label() {
            return label;
        }

        String executionMode() {
            return executionMode;
        }

        String routedTarget() {
            return routedTarget;
        }

        Map<String, Object> asMap() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("groupKey", groupKey);
            out.put("sqlLabel", label);
            out.put("executionMode", executionMode);
            out.put("routedTarget", routedTarget);
            out.put("failureCount", failureCount.get());
            out.put("sampleMessage", sampleMessage);
            return out;
        }
    }
}
