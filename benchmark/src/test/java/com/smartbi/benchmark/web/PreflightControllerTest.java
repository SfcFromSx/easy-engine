package com.smartbi.benchmark.web;

import com.smartbi.benchmark.config.BenchmarkPreflightProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreflightControllerTest {

    // Covers PreflightController#check success probes for Kylin and Presto.
    @Test
    void shouldReportHealthyDependenciesWhenBothProbesSucceed() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BenchmarkPreflightProperties props = new BenchmarkPreflightProperties();
        props.setKylinAuthUrl("http://kylin/auth");
        props.setPrestoInfoUrl("http://presto/info");
        PreflightController controller = new PreflightController(restTemplate, props);
        when(restTemplate.exchange(eq("http://kylin/auth"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));
        when(restTemplate.getForEntity("http://presto/info", String.class)).thenReturn(ResponseEntity.ok("ok"));

        Map<String, Object> result = controller.check();

        assertEquals("OK", ((Map<?, ?>) result.get("mysql")).get("status"));
        assertEquals("OK", ((Map<?, ?>) result.get("kylinRest")).get("status"));
        assertEquals("OK", ((Map<?, ?>) result.get("prestoUi")).get("status"));
    }

    // Covers PreflightController#check HTTP failure and client-exception branches.
    @Test
    void shouldReportProbeFailuresWithStatusOrErrorDetails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        BenchmarkPreflightProperties props = new BenchmarkPreflightProperties();
        props.setKylinAuthUrl("http://kylin/auth");
        props.setPrestoInfoUrl("http://presto/info");
        PreflightController controller = new PreflightController(restTemplate, props);
        when(restTemplate.exchange(eq("http://kylin/auth"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(503).body("down"));
        when(restTemplate.getForEntity("http://presto/info", String.class))
                .thenThrow(new RestClientException("presto offline"));

        Map<String, Object> result = controller.check();

        assertEquals("FAIL", ((Map<?, ?>) result.get("kylinRest")).get("status"));
        assertEquals(503, ((Map<?, ?>) result.get("kylinRest")).get("httpStatus"));
        assertEquals("FAIL", ((Map<?, ?>) result.get("prestoUi")).get("status"));
        assertEquals("presto offline", ((Map<?, ?>) result.get("prestoUi")).get("error"));
    }
}
