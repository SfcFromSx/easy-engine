package com.smartbi.query.config;

import com.smartbi.query.support.QueryTestFixtures;
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

    private static final String MANAGER_URL = QueryTestFixtures.get("query.test.manager-url");
    private static final String DATASOURCE_URL = QueryTestFixtures.get("query.test.manager-response.default-jdbc-url");
    private static final String DRIVER_CLASS = QueryTestFixtures.get("query.test.manager-response.default-driver-class");

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
        properties.setManagerUrl(MANAGER_URL + "/");
        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);
        MockRestServiceServer server = bind(client);

        server.expect(requestTo(MANAGER_URL + "/api/v1/query-routing-context"))
                .andRespond(withSuccess("{\"datasources\":[{\"name\":\"default\",\"type\":\"mysql\",\"jdbcUrl\":\"" + DATASOURCE_URL
                                + "\",\"driverClass\":\"" + DRIVER_CLASS + "\"}],\"accelerationRules\":[]}",
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
        properties.setManagerUrl(MANAGER_URL);
        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);
        MockRestServiceServer server = bind(client);

        server.expect(requestTo(MANAGER_URL + "/api/v1/query-routing-context"))
                .andRespond(withNoContent());

        assertTrue(client.fetchDatasourceConfigs().isEmpty());
        server.verify();
    }

    // Covers ManagerConfigClient#fetchDatasourceConfigs empty-array fallback branch.
    @Test
    void shouldReturnEmptyListWhenManagerRespondsWithEmptyArray() {
        QueryProperties properties = new QueryProperties();
        properties.setManagerUrl(MANAGER_URL);
        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);
        MockRestServiceServer server = bind(client);

        server.expect(requestTo(MANAGER_URL + "/api/v1/query-routing-context"))
                .andRespond(withSuccess("{\"datasources\":[],\"accelerationRules\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(client.fetchDatasourceConfigs().isEmpty());
        server.verify();
    }

    // Covers ManagerConfigClient#fetchDatasourceConfigs error-path wrapping for RestClientException responses.
    @Test
    void shouldWrapRestClientFailures() {
        QueryProperties properties = new QueryProperties();
        properties.setManagerUrl(MANAGER_URL);
        ManagerConfigClient client = new ManagerConfigClient(new RestTemplateBuilder(), properties);
        MockRestServiceServer server = bind(client);

        server.expect(requestTo(MANAGER_URL + "/api/v1/query-routing-context"))
                .andRespond(withServerError());

        IllegalStateException exception = assertThrows(IllegalStateException.class, client::fetchDatasourceConfigs);

        assertTrue(exception.getMessage().contains(MANAGER_URL + "/api/v1/query-routing-context"));
        server.verify();
    }

    private static MockRestServiceServer bind(ManagerConfigClient client) {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        return MockRestServiceServer.bindTo(restTemplate).build();
    }
}
