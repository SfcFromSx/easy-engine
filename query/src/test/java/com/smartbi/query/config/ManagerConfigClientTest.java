package com.smartbi.query.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ManagerConfigClientTest {

    // Covers ManagerConfigClient#fetchDatasourceConfigs blank-manager-url fallback branch.
    @Test
    void shouldReturnEmptyListWhenManagerUrlIsBlank() {
        QueryProperties properties = new QueryProperties();
        properties.setManagerUrl("  ");

        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);

        assertTrue(client.fetchDatasourceConfigs().isEmpty());
    }

    // Covers ManagerConfigClient#fetchDatasourceConfigs trailing-slash trimming and successful body mapping.
    @Test
    void shouldTrimTrailingSlashAndMapDatasourceConfigs() {
        QueryProperties properties = new QueryProperties();
        properties.setManagerUrl("http://localhost:8090/");
        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);
        MockRestServiceServer server = bind(client);

        server.expect(requestTo("http://localhost:8090/api/v1/query-datasources"))
                .andRespond(withSuccess("[{\"name\":\"default\",\"type\":\"h2\",\"jdbcUrl\":\"jdbc:h2:mem:test\",\"driverClass\":\"org.h2.Driver\"}]",
                        MediaType.APPLICATION_JSON));

        List<ManagerConfigClient.ManagerDatasourceConfig> configs = client.fetchDatasourceConfigs();

        assertEquals(1, configs.size());
        assertEquals("default", configs.get(0).getName());
        server.verify();
    }

    // Covers ManagerConfigClient#fetchDatasourceConfigs null-body fallback branch.
    @Test
    void shouldReturnEmptyListWhenManagerRespondsWithoutBody() {
        QueryProperties properties = new QueryProperties();
        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);
        MockRestServiceServer server = bind(client);

        server.expect(requestTo("http://localhost:8090/api/v1/query-datasources"))
                .andRespond(withNoContent());

        assertTrue(client.fetchDatasourceConfigs().isEmpty());
        server.verify();
    }

    // Covers ManagerConfigClient#fetchDatasourceConfigs empty-array fallback branch.
    @Test
    void shouldReturnEmptyListWhenManagerRespondsWithEmptyArray() {
        QueryProperties properties = new QueryProperties();
        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);
        MockRestServiceServer server = bind(client);

        server.expect(requestTo("http://localhost:8090/api/v1/query-datasources"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.fetchDatasourceConfigs().isEmpty());
        server.verify();
    }

    // Covers ManagerConfigClient#fetchDatasourceConfigs error-path wrapping for RestClientException responses.
    @Test
    void shouldWrapRestClientFailures() {
        QueryProperties properties = new QueryProperties();
        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);
        MockRestServiceServer server = bind(client);

        server.expect(requestTo("http://localhost:8090/api/v1/query-datasources"))
                .andRespond(withServerError());

        IllegalStateException exception = assertThrows(IllegalStateException.class, client::fetchDatasourceConfigs);

        assertTrue(exception.getMessage().contains("http://localhost:8090/api/v1/query-datasources"));
        server.verify();
    }

    private static MockRestServiceServer bind(ManagerConfigClient client) {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        return MockRestServiceServer.bindTo(restTemplate).build();
    }
}
