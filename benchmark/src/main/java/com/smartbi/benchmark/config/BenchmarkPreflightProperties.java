package com.smartbi.benchmark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark.preflight")
public class BenchmarkPreflightProperties {

    /**
     * Kylin REST 认证探测。
     * 具体地址与凭据必须由外部配置提供，不能在 Java 代码中硬编码。
     */
    private String kylinAuthUrl;
    private String kylinUser;
    private String kylinPassword;

    private String prestoInfoUrl;

    public String getKylinAuthUrl() {
        return kylinAuthUrl;
    }

    public void setKylinAuthUrl(String kylinAuthUrl) {
        this.kylinAuthUrl = kylinAuthUrl;
    }

    public String getKylinUser() {
        return kylinUser;
    }

    public void setKylinUser(String kylinUser) {
        this.kylinUser = kylinUser;
    }

    public String getKylinPassword() {
        return kylinPassword;
    }

    public void setKylinPassword(String kylinPassword) {
        this.kylinPassword = kylinPassword;
    }

    public String getPrestoInfoUrl() {
        return prestoInfoUrl;
    }

    public void setPrestoInfoUrl(String prestoInfoUrl) {
        this.prestoInfoUrl = prestoInfoUrl;
    }
}
