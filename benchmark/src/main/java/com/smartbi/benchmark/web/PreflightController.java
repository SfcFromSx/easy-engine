package com.smartbi.benchmark.web;

import com.smartbi.benchmark.config.BenchmarkPreflightProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端「环境预检」：页面触发，替代脚本探测；不启动压测。
 */
@RestController
@RequestMapping("/api/v1/preflight")
public class PreflightController {

    private final RestTemplate restTemplate;
    private final BenchmarkPreflightProperties props;

    public PreflightController(@Qualifier("benchmarkRestTemplate") RestTemplate benchmarkRestTemplate,
                              BenchmarkPreflightProperties props) {
        this.restTemplate = benchmarkRestTemplate;
        this.props = props;
    }

    @GetMapping
    public Map<String, Object> check() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> mysql = new LinkedHashMap<>();
        mysql.put("status", "OK");
        mysql.put("hint", "应用已连上配置的 MySQL；端口以 application 为准");
        out.put("mysql", mysql);
        out.put("kylinRest", probeKylin());
        out.put("prestoUi", probePresto());
        out.put("message", "压测请在「控制台」页发起；本接口仅做连通性参考。");
        return out;
    }

    private Map<String, Object> probeKylin() {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setBasicAuth(props.getKylinUser(), props.getKylinPassword());
            ResponseEntity<String> r = restTemplate.exchange(
                    props.getKylinAuthUrl(),
                    HttpMethod.GET,
                    new HttpEntity<>(h),
                    String.class);
            if (r.getStatusCode().is2xxSuccessful()) {
                return okProbe(props.getKylinAuthUrl());
            }
            return failHttp(props.getKylinAuthUrl(), r.getStatusCodeValue());
        } catch (RestClientException e) {
            return failErr(props.getKylinAuthUrl(), e.getMessage());
        }
    }

    private Map<String, Object> probePresto() {
        try {
            ResponseEntity<String> r = restTemplate.getForEntity(props.getPrestoInfoUrl(), String.class);
            if (r.getStatusCode().is2xxSuccessful()) {
                return okProbe(props.getPrestoInfoUrl());
            }
            return failHttp(props.getPrestoInfoUrl(), r.getStatusCodeValue());
        } catch (RestClientException e) {
            return failErr(props.getPrestoInfoUrl(), e.getMessage());
        }
    }

    private static Map<String, Object> okProbe(String url) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "OK");
        m.put("url", url);
        return m;
    }

    private static Map<String, Object> failHttp(String url, int code) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "FAIL");
        m.put("httpStatus", code);
        m.put("url", url);
        return m;
    }

    private static Map<String, Object> failErr(String url, String err) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "FAIL");
        m.put("error", err);
        m.put("url", url);
        return m;
    }
}
