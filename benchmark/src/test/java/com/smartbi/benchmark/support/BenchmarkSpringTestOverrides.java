package com.smartbi.benchmark.support;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public final class BenchmarkSpringTestOverrides {

    private static final Path DRIVER_DIR = Paths.get("target/test-drivers/uploaded").toAbsolutePath().normalize();
    private static final Path LEGACY_DRIVER_DIR = Paths.get("benchmark/target/test-drivers/uploaded").toAbsolutePath().normalize();
    private static final String DELETE_ERROR_PREFIX = "Failed to reset benchmark JDBC driver test directory: ";

    private BenchmarkSpringTestOverrides() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("benchmark.jdbc.driver-dir", BenchmarkSpringTestOverrides::prepareDriverDirectory);
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

    private static String prepareDriverDirectory() {
        resetDirectory(LEGACY_DRIVER_DIR);
        resetDirectory(DRIVER_DIR);
        return DRIVER_DIR.toString();
    }

    private static void resetDirectory(Path path) {
        try {
            if (Files.exists(path)) {
                try (Stream<Path> stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(current -> deletePath(current));
                }
            }
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException(DELETE_ERROR_PREFIX + path, e);
        }
    }

    private static void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException(DELETE_ERROR_PREFIX + path, e);
        }
    }
}
