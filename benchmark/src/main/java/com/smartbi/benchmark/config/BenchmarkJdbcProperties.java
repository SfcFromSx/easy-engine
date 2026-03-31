package com.smartbi.benchmark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark.jdbc")
public class BenchmarkJdbcProperties {

    private String defaultDriverClass = "org.apache.kylin.jdbc.Driver";
    private String driverDir = "./drivers";

    public String getDefaultDriverClass() {
        return defaultDriverClass;
    }

    public void setDefaultDriverClass(String defaultDriverClass) {
        this.defaultDriverClass = defaultDriverClass;
    }

    public String getDriverDir() {
        return driverDir;
    }

    public void setDriverDir(String driverDir) {
        this.driverDir = driverDir;
    }
}
