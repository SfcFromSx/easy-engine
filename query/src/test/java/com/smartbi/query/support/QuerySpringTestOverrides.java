package com.smartbi.query.support;

import org.springframework.test.context.DynamicPropertyRegistry;

public final class QuerySpringTestOverrides {

    private QuerySpringTestOverrides() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> QueryTestFixtures.get("spring.datasource.url"));
        registry.add("spring.datasource.username", () -> QueryTestFixtures.get("spring.datasource.username"));
        registry.add("spring.datasource.password", () -> QueryTestFixtures.get("spring.datasource.password"));
        registry.add("spring.datasource.driver-class-name", () -> QueryTestFixtures.get("spring.datasource.driver-class-name"));

        registry.add("engine.query.manager-url", () -> QueryTestFixtures.get("engine.query.manager-url"));
        registry.add("engine.query.datasource.default.name", () -> QueryTestFixtures.get("engine.query.datasource.default.name"));
        registry.add("engine.query.datasource.default.type", () -> QueryTestFixtures.get("engine.query.datasource.default.type"));
        registry.add("engine.query.datasource.default.driver-class", () -> QueryTestFixtures.get("engine.query.datasource.default.driver-class"));
        registry.add("engine.query.datasource.default.jdbc-url", () -> QueryTestFixtures.get("engine.query.datasource.default.jdbc-url"));
        registry.add("engine.query.datasource.default.username", () -> QueryTestFixtures.get("engine.query.datasource.default.username"));
        registry.add("engine.query.datasource.default.password", () -> QueryTestFixtures.get("engine.query.datasource.default.password"));

        registry.add("engine.query.datasource.named.presto_local.name", () -> QueryTestFixtures.get("engine.query.datasource.named.presto_local.name"));
        registry.add("engine.query.datasource.named.presto_local.type", () -> QueryTestFixtures.get("engine.query.datasource.named.presto_local.type"));
        registry.add("engine.query.datasource.named.presto_local.driver-class", () -> QueryTestFixtures.get("engine.query.datasource.named.presto_local.driver-class"));
        registry.add("engine.query.datasource.named.presto_local.jdbc-url", () -> QueryTestFixtures.get("engine.query.datasource.named.presto_local.jdbc-url"));
        registry.add("engine.query.datasource.named.presto_local.username", () -> QueryTestFixtures.get("engine.query.datasource.named.presto_local.username"));
        registry.add("engine.query.datasource.named.presto_local.password", () -> QueryTestFixtures.get("engine.query.datasource.named.presto_local.password"));

        registry.add("engine.query.datasource.named.trino_local.name", () -> QueryTestFixtures.get("engine.query.datasource.named.trino_local.name"));
        registry.add("engine.query.datasource.named.trino_local.type", () -> QueryTestFixtures.get("engine.query.datasource.named.trino_local.type"));
        registry.add("engine.query.datasource.named.trino_local.driver-class", () -> QueryTestFixtures.get("engine.query.datasource.named.trino_local.driver-class"));
        registry.add("engine.query.datasource.named.trino_local.jdbc-url", () -> QueryTestFixtures.get("engine.query.datasource.named.trino_local.jdbc-url"));
        registry.add("engine.query.datasource.named.trino_local.username", () -> QueryTestFixtures.get("engine.query.datasource.named.trino_local.username"));
        registry.add("engine.query.datasource.named.trino_local.password", () -> QueryTestFixtures.get("engine.query.datasource.named.trino_local.password"));
    }
}
