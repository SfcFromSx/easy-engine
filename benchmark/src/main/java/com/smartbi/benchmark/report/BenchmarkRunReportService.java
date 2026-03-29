package com.smartbi.benchmark.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartbi.benchmark.domain.BenchmarkDataSource;
import com.smartbi.benchmark.domain.BenchmarkJob;
import com.smartbi.benchmark.domain.BenchmarkRun;
import com.smartbi.benchmark.domain.BenchmarkStrategy;
import com.smartbi.benchmark.domain.RunStatus;
import com.smartbi.benchmark.repo.BenchmarkDataSourceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成任务快照与结构化评价 JSON，便于页面展示及 JDBC/缓存改动前后对比。
 */
@Service
public class BenchmarkRunReportService {

    private static final int SCHEMA_VERSION = 1;

    private final BenchmarkDataSourceRepository dataSourceRepository;
    private final ObjectMapper json;

    public BenchmarkRunReportService(BenchmarkDataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
        this.json = new ObjectMapper();
        this.json.registerModule(new JavaTimeModule());
        this.json.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String toJson(Map<String, Object> map) {
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            return "{\"schemaVersion\":" + SCHEMA_VERSION + ",\"error\":\"serialize_failed\",\"message\":"
                    + json.valueToTree(e.getMessage()) + "}";
        }
    }

    public String buildJobSnapshotJson(BenchmarkJob job, int sqlSourceCount, String testSetName) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("jobId", job.getId());
        snap.put("jobName", job.getName());

        if (job.getDataSourceId() != null) {
            BenchmarkDataSource ds = dataSourceRepository.findById(job.getDataSourceId()).orElse(null);
            if (ds != null) {
                snap.put("dataSourceId", ds.getId());
                snap.put("dataSourceName", ds.getName());
                snap.put("driverClass", ds.getDriverClass());
                snap.put("jdbcUrlRedacted", redactJdbcUrl(ds.getJdbcUrl()));
                snap.put("jdbcUser", ds.getJdbcUser());
            }
        }

        snap.put("strategy", job.getStrategy() != null ? job.getStrategy().name() : null);
        snap.put("concurrentThreads", job.getConcurrentThreads());
        snap.put("rounds", job.getRounds());
        snap.put("testSetId", job.getTestSetId());
        snap.put("testSetName", testSetName);
        snap.put("sqlSourceKind", job.getTestSetId() != null ? "test_set" : "global_templates");
        snap.put("sqlSourceCount", sqlSourceCount);
        return toJson(snap);
    }

    public String buildFailureEvaluation(String phase, String message, BenchmarkJob job) {
        Map<String, Object> root = baseEnvelope(job);
        root.put("verdict", "FAIL");
        root.put("summary", "压测未正常完成：" + truncate(message, 200));
        root.put("phase", phase);
        Map<String, Object> metricsStub = new LinkedHashMap<>();
        metricsStub.put("note", "无有效样本时 latency/QPS 不可用");
        root.put("metrics", metricsStub);
        List<String> issues = new ArrayList<>();
        issues.add(message);
        root.put("issues", issues);
        root.put("jdbcComparisonHints", jdbcHintsPlaceholder());
        return toJson(root);
    }

    public String buildCompletedEvaluation(BenchmarkJob job, BenchmarkRun run, int sqlSourceCount) {
        Map<String, Object> root = baseEnvelope(job);
        long total = nullToZero(run.getTotalQueries());
        long ok = nullToZero(run.getSuccessCount());
        long err = nullToZero(run.getErrorCount());
        double successRate = total > 0 ? (double) ok / (double) total : 0D;

        String verdict;
        String summary;
        if (err == 0) {
            verdict = "PASS";
            summary = String.format("全部 %d 次查询成功，可作为基线与其他 Run 对比。", total);
        } else if (ok > 0) {
            verdict = "PARTIAL";
            summary = String.format("成功 %d / %d，存在失败样本，对比时请同时看 successRate 与 errorSample。", ok, total);
        } else {
            verdict = "FAIL";
            summary = "全部失败，请先修复 SQL/引擎连通性后再做性能对比。";
        }

        root.put("verdict", verdict);
        root.put("summary", summary);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalQueries", total);
        metrics.put("successCount", ok);
        metrics.put("errorCount", err);
        metrics.put("successRate", round4(Double.valueOf(successRate)));
        metrics.put("wallDurationMs", run.getDurationMs());
        metrics.put("qpsSuccessful", run.getQps() != null ? round4(run.getQps()) : null);

        Double p50 = run.getP50Ms();
        Double p95 = run.getP95Ms();
        Double p99 = run.getP99Ms();
        Map<String, Object> lat = new LinkedHashMap<>();
        lat.put("p50", p50);
        lat.put("p95", p95);
        lat.put("p99", p99);
        if (p50 != null && p50 > 0 && p95 != null) {
            lat.put("p95OverP50", round4(p95 / p50));
        }
        metrics.put("latencyMs", lat);
        metrics.put("observedSampleCount", ok);
        root.put("metrics", metrics);

        List<String> issues = new ArrayList<>();
        if (err > 0 && run.getErrorSample() != null) {
            issues.add(truncate(run.getErrorSample(), 500));
        }
        root.put("issues", issues);

        Map<String, Object> jdbcHints = new LinkedHashMap<>();
        List<String> dimensions = new ArrayList<>();
        dimensions.add("对比原则：固定同一任务（jobId）、相同 rounds 与并发，仅更换驱动 JAR 或 JDBC 参数，再比较本报告的 metrics.latencyMs 与 successRate。");
        dimensions.add("p50 反映常见路径耗时；p95/p99 反映长尾。缓存命中优化通常先降低 p50，冷路径或 no-cache 策略会抬高 p95。");
        if (job.getStrategy() == BenchmarkStrategy.CACHE_PENETRATION) {
            dimensions.add("当前为 CACHE_PENETRATION：语句前会附加 -- no-cache，用于压后端与穿透缓存，不宜与未加穿透的同 SQL 直接比绝对延迟。");
        }
        if (job.getStrategy() == BenchmarkStrategy.ROUND_ROBIN) {
            dimensions.add("ROUND_ROBIN 会均匀覆盖测试集中每条 SQL，适合回归；与 RANDOM_WEIGHT 的延迟分布可能不同。");
        }
        jdbcHints.put("dimensions", dimensions);

        String tailNote = interpretTail(p50, p95);
        jdbcHints.put("tailLatencyNote", tailNote);

        List<String> recommended = new ArrayList<>();
        recommended.add("将 evaluationJson 完整保存到工单/PR，便于 JDBC 改动前后对照。");
        recommended.add("若需 CI 门禁，可解析 JSON 字段 metrics.successRate、verdict。");
        jdbcHints.put("recommendedNextActions", recommended);

        root.put("jdbcComparisonHints", jdbcHints);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("sqlSourceCount", sqlSourceCount);
        meta.put("status", run.getStatus() != null ? run.getStatus().name() : null);
        root.put("meta", meta);

        return toJson(root);
    }

    public Map<String, Object> buildComparisonDelta(BenchmarkRun current, BenchmarkRun previous) {
        Map<String, Object> delta = new LinkedHashMap<>();
        if (previous == null || previous.getStatus() != RunStatus.COMPLETED || current.getStatus() != RunStatus.COMPLETED) {
            delta.put("available", false);
            delta.put("reason", "无上一次已完成同任务 Run，或当前 Run 未完成");
            return delta;
        }
        delta.put("available", true);
        delta.put("baselineRunId", previous.getId());
        delta.put("baselineStartedAt", previous.getStartedAt());
        Map<String, Object> deltas = new LinkedHashMap<>();
        deltas.put("p50Ms", numDelta(current.getP50Ms(), previous.getP50Ms()));
        deltas.put("p95Ms", numDelta(current.getP95Ms(), previous.getP95Ms()));
        deltas.put("successRate", rateDelta(current, previous));
        deltas.put("qpsSuccessful", numDelta(current.getQps(), previous.getQps()));
        delta.put("deltas", deltas);
        delta.put("interpretationHint",
                "负的 p50/p95 delta% 表示较基线更快；successRate 应保持稳定或上升。");
        return delta;
    }

    private static Map<String, Object> numDelta(Double cur, Double base) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (cur == null || base == null || base == 0D) {
            m.put("percent", null);
            m.put("note", "缺失或基线为 0，无法算百分比");
            return m;
        }
        double pct = (cur - base) / base * 100D;
        m.put("current", round4(cur));
        m.put("baseline", round4(base));
        m.put("percentChange", round4(pct));
        return m;
    }

    private static Map<String, Object> rateDelta(BenchmarkRun cur, BenchmarkRun prev) {
        double c = rate(cur);
        double p = rate(prev);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("current", round4(c));
        m.put("baseline", round4(p));
        m.put("absoluteDelta", round4(c - p));
        return m;
    }

    private static double rate(BenchmarkRun r) {
        long t = nullToZero(r.getTotalQueries());
        if (t == 0) {
            return 0D;
        }
        return (double) nullToZero(r.getSuccessCount()) / (double) t;
    }

    private Map<String, Object> baseEnvelope(BenchmarkJob job) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("kind", "benchmark.runEvaluation");
        root.put("generatedAt", Instant.now().toString());
        if (job != null) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("id", job.getId());
            ref.put("name", job.getName());
            root.put("jobRef", ref);
        }
        return root;
    }

    private static Map<String, Object> jdbcHintsPlaceholder() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<String> dims = new ArrayList<>();
        dims.add("修复连通性或 SQL 后重新在页面发起压测，再导出新的 evaluationJson 做对比。");
        m.put("dimensions", dims);
        return m;
    }

    private static String interpretTail(Double p50, Double p95) {
        if (p50 == null || p95 == null) {
            return "无足够成功样本的延迟分位数据。";
        }
        if (p50 <= 0) {
            return "p50 异常，请检查是否多数请求失败。";
        }
        double ratio = p95 / p50;
        if (ratio > 10) {
            return "p95 远高于 p50（倍数较大），常见于混合了缓存命中与未命中、或偶发后端慢查询。";
        }
        if (ratio > 3) {
            return "尾部延迟明显高于中位数，可关注并发与引擎负载。";
        }
        return "尾部分布相对温和，可结合 QPS 与 successRate 评估改动效果。";
    }

    private static long nullToZero(Long v) {
        return v == null ? 0L : v;
    }

    private static Double round4(Double v) {
        if (v == null) {
            return null;
        }
        return Math.round(v * 10000D) / 10000D;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * 隐去 JDBC URL 中常见敏感 query 参数，避免报告泄露口令。
     */
    static String redactJdbcUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        String u = url;
        u = u.replaceAll("(?i)(password=)([^&]*)", "$1***");
        u = u.replaceAll("(?i)(pwd=)([^&]*)", "$1***");
        return u;
    }
}
