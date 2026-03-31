package com.smartbi.benchmark;

import com.smartbi.benchmark.config.BenchmarkJdbcProperties;
import com.smartbi.benchmark.config.BenchmarkPreflightProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({BenchmarkPreflightProperties.class, BenchmarkJdbcProperties.class})
public class BenchmarkApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchmarkApplication.class, args);
    }
}
