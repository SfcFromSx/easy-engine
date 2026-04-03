package com.smartbi.query.support;

import org.springframework.test.context.DynamicPropertyRegistry;

public final class QuerySpringTestOverrides {

    private QuerySpringTestOverrides() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> QueryTestFixtures.get("query.test.meta-db.url"));
        registry.add("spring.datasource.username", () -> QueryTestFixtures.get("query.test.meta-db.username"));
        registry.add("spring.datasource.password", () -> QueryTestFixtures.get("query.test.meta-db.password"));
        registry.add("spring.datasource.driver-class-name", () -> QueryTestFixtures.get("query.test.meta-db.driver-class-name"));

        registry.add("engine.query.datasource.default.name", () -> QueryTestFixtures.get("query.test.registry.default.name"));
        registry.add("engine.query.datasource.default.type", () -> QueryTestFixtures.get("query.test.registry.default.type"));
        registry.add("engine.query.datasource.default.driver-class", () -> QueryTestFixtures.get("query.test.registry.default.driver-class"));
        registry.add("engine.query.datasource.default.jdbc-url", () -> QueryTestFixtures.get("query.test.registry.default.jdbc-url"));
        registry.add("engine.query.datasource.default.username", () -> QueryTestFixtures.get("query.test.registry.default.username"));
        registry.add("engine.query.datasource.default.password", () -> QueryTestFixtures.get("query.test.registry.default.password"));

        registry.add("engine.query.datasource.named.presto_local.name", () -> QueryTestFixtures.get("query.test.registry.presto.name"));
        registry.add("engine.query.datasource.named.presto_local.type", () -> QueryTestFixtures.get("query.test.registry.presto.type"));
        registry.add("engine.query.datasource.named.presto_local.driver-class", () -> QueryTestFixtures.get("query.test.registry.presto.driver-class"));
        registry.add("engine.query.datasource.named.presto_local.jdbc-url", () -> QueryTestFixtures.get("query.test.registry.presto.jdbc-url"));
        registry.add("engine.query.datasource.named.presto_local.username", () -> QueryTestFixtures.get("query.test.registry.presto.username"));
        registry.add("engine.query.datasource.named.presto_local.password", () -> QueryTestFixtures.get("query.test.registry.presto.password"));

        registry.add("engine.query.datasource.named.trino_local.name", () -> QueryTestFixtures.get("query.test.registry.trino.name"));
        registry.add("engine.query.datasource.named.trino_local.type", () -> QueryTestFixtures.get("query.test.registry.trino.type"));
        registry.add("engine.query.datasource.named.trino_local.driver-class", () -> QueryTestFixtures.get("query.test.registry.trino.driver-class"));
        registry.add("engine.query.datasource.named.trino_local.jdbc-url", () -> QueryTestFixtures.get("query.test.registry.trino.jdbc-url"));
        registry.add("engine.query.datasource.named.trino_local.username", () -> QueryTestFixtures.get("query.test.registry.trino.username"));
        registry.add("engine.query.datasource.named.trino_local.password", () -> QueryTestFixtures.get("query.test.registry.trino.password"));
    }
}
