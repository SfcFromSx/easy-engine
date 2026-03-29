package com.smartbi.query;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.SpringApplication;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class EngineQueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineQueryApplication.class, args);
    }
}
