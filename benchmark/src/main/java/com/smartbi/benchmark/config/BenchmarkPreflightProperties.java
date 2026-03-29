package com.smartbi.benchmark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark.preflight")
public class BenchmarkPreflightProperties {

    /**
     * Kylin REST 认证探测（与 docker-compose 默认一致）。
     */
    private String kylinAuthUrl = "http://127.0.0.1:17070/kylin/api/user/authentication";
    private String kylinUser = "ADMIN";
    private String kylinPassword = "KYLIN";

    private String prestoInfoUrl = "http://127.0.0.1:18081/v1/info";

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
