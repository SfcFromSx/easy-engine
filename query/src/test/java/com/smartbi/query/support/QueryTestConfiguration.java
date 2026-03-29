package com.smartbi.query.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class QueryTestConfiguration {

    @Bean
    @Primary
    public InMemoryQueryInfrastructure inMemoryQueryInfrastructure() {
        return new InMemoryQueryInfrastructure();
    }
}
