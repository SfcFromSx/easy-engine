package com.smartbi.benchmark.support;

import org.springframework.test.context.DynamicPropertyRegistry;

public final class BenchmarkSpringTestOverrides {

    private BenchmarkSpringTestOverrides() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> BenchmarkTestFixtures.get("spring.datasource.url"));
        registry.add("spring.datasource.username", () -> BenchmarkTestFixtures.get("spring.datasource.username"));
        registry.add("spring.datasource.password", () -> BenchmarkTestFixtures.get("spring.datasource.password"));
        registry.add("spring.datasource.driver-class-name", () -> BenchmarkTestFixtures.get("spring.datasource.driver-class-name"));

        registry.add("benchmark.preflight.kylin-auth-url", () -> BenchmarkTestFixtures.get("benchmark.preflight.kylin-auth-url"));
        registry.add("benchmark.preflight.kylin-user", () -> BenchmarkTestFixtures.get("benchmark.preflight.kylin-user"));
        registry.add("benchmark.preflight.kylin-password", () -> BenchmarkTestFixtures.get("benchmark.preflight.kylin-password"));
        registry.add("benchmark.preflight.presto-info-url", () -> BenchmarkTestFixtures.get("benchmark.preflight.presto-info-url"));

        registry.add("benchmark.test.datasource.update.jdbc-url", () -> BenchmarkTestFixtures.get("benchmark.test.datasource.update.jdbc-url"));
        registry.add("benchmark.test.datasource.update.jdbc-user", () -> BenchmarkTestFixtures.get("benchmark.test.datasource.update.jdbc-user"));
        registry.add("benchmark.test.datasource.update.jdbc-password", () -> BenchmarkTestFixtures.get("benchmark.test.datasource.update.jdbc-password"));
        registry.add("benchmark.test.datasource.update.driver-class", () -> BenchmarkTestFixtures.get("benchmark.test.datasource.update.driver-class"));

        registry.add("benchmark.test.smoke.test-datasource.name", () -> BenchmarkTestFixtures.get("benchmark.test.smoke.test-datasource.name"));
        registry.add("benchmark.test.smoke.test-datasource.type", () -> BenchmarkTestFixtures.get("benchmark.test.smoke.test-datasource.type"));
        registry.add("benchmark.test.smoke.test-datasource.driver-class", () -> BenchmarkTestFixtures.get("benchmark.test.smoke.test-datasource.driver-class"));
        registry.add("benchmark.test.smoke.test-datasource.jdbc-url", () -> BenchmarkTestFixtures.get("benchmark.test.smoke.test-datasource.jdbc-url"));
        registry.add("benchmark.test.smoke.test-datasource.jdbc-user", () -> BenchmarkTestFixtures.get("benchmark.test.smoke.test-datasource.jdbc-user"));
        registry.add("benchmark.test.smoke.test-datasource.jdbc-password", () -> BenchmarkTestFixtures.get("benchmark.test.smoke.test-datasource.jdbc-password"));

        registry.add("benchmark.test.jdbc-upload.test-datasource.name", () -> BenchmarkTestFixtures.get("benchmark.test.jdbc-upload.test-datasource.name"));
        registry.add("benchmark.test.jdbc-upload.test-datasource.driver-class", () -> BenchmarkTestFixtures.get("benchmark.test.jdbc-upload.test-datasource.driver-class"));
        registry.add("benchmark.test.jdbc-upload.test-datasource.jdbc-url", () -> BenchmarkTestFixtures.get("benchmark.test.jdbc-upload.test-datasource.jdbc-url"));
        registry.add("benchmark.test.jdbc-upload.test-datasource.jdbc-user", () -> BenchmarkTestFixtures.get("benchmark.test.jdbc-upload.test-datasource.jdbc-user"));
        registry.add("benchmark.test.jdbc-upload.test-datasource.jdbc-password", () -> BenchmarkTestFixtures.get("benchmark.test.jdbc-upload.test-datasource.jdbc-password"));

        registry.add("benchmark.test.async-runner.target.driver-class", () -> BenchmarkTestFixtures.get("benchmark.test.async-runner.target.driver-class"));
        registry.add("benchmark.test.async-runner.target.jdbc-url", () -> BenchmarkTestFixtures.get("benchmark.test.async-runner.target.jdbc-url"));
        registry.add("benchmark.test.async-runner.target.jdbc-user", () -> BenchmarkTestFixtures.get("benchmark.test.async-runner.target.jdbc-user"));
        registry.add("benchmark.test.async-runner.target.jdbc-password", () -> BenchmarkTestFixtures.get("benchmark.test.async-runner.target.jdbc-password"));
    }
}
