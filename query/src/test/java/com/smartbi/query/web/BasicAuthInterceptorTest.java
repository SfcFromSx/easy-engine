package com.smartbi.query.web;

import com.smartbi.query.config.QueryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicAuthInterceptorTest {

    // Covers BasicAuthInterceptor#preHandle OPTIONS bypass branch.
    @Test
    void shouldAllowOptionsRequestsWithoutAuthentication() throws Exception {
        BasicAuthInterceptor interceptor = new BasicAuthInterceptor(properties("ADMIN", "KYLIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/kylin/api/query");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    // Covers BasicAuthInterceptor#preHandle auth-disabled bypass branch.
    @Test
    void shouldAllowRequestsWhenExpectedUsernameIsBlank() throws Exception {
        BasicAuthInterceptor interceptor = new BasicAuthInterceptor(properties("", "KYLIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/kylin/api/query");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    // Covers BasicAuthInterceptor#preHandle missing-header unauthorized branch.
    @Test
    void shouldRejectRequestsWithoutAuthorizationHeader() throws Exception {
        BasicAuthInterceptor interceptor = new BasicAuthInterceptor(properties("ADMIN", "KYLIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/kylin/api/query");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertEquals("{\"message\":\"unauthorized\"}", response.getContentAsString());
    }

    // Covers BasicAuthInterceptor#preHandle malformed-base64 unauthorized branch.
    @Test
    void shouldRejectRequestsWithMalformedBase64Credentials() throws Exception {
        BasicAuthInterceptor interceptor = new BasicAuthInterceptor(properties("ADMIN", "KYLIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/kylin/api/query");
        request.addHeader("Authorization", "Basic !!!");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    // Covers BasicAuthInterceptor#preHandle missing-colon and BasicAuthInterceptor#safeEquals mismatch branches.
    @Test
    void shouldRejectRequestsWithMissingColonOrWrongCredentials() throws Exception {
        BasicAuthInterceptor interceptor = new BasicAuthInterceptor(properties("ADMIN", "KYLIN"));
        MockHttpServletResponse missingColonResponse = new MockHttpServletResponse();
        MockHttpServletRequest missingColonRequest = new MockHttpServletRequest("POST", "/kylin/api/query");
        missingColonRequest.addHeader("Authorization", header("ADMIN"));

        assertFalse(interceptor.preHandle(missingColonRequest, missingColonResponse, new Object()));
        assertEquals(401, missingColonResponse.getStatus());

        MockHttpServletResponse wrongPasswordResponse = new MockHttpServletResponse();
        MockHttpServletRequest wrongPasswordRequest = new MockHttpServletRequest("POST", "/kylin/api/query");
        wrongPasswordRequest.addHeader("Authorization", header("ADMIN:WRONG"));

        assertFalse(interceptor.preHandle(wrongPasswordRequest, wrongPasswordResponse, new Object()));
        assertEquals(401, wrongPasswordResponse.getStatus());
    }

    // Covers BasicAuthInterceptor#preHandle successful auth branch.
    @Test
    void shouldAllowRequestsWithMatchingCredentials() throws Exception {
        BasicAuthInterceptor interceptor = new BasicAuthInterceptor(properties("ADMIN", "KYLIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/kylin/api/query");
        request.addHeader("Authorization", header("ADMIN:KYLIN"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    private static QueryProperties properties(String username, String password) {
        QueryProperties properties = new QueryProperties();
        properties.getAuth().setUsername(username);
        properties.getAuth().setPassword(password);
        return properties;
    }

    private static String header(String value) {
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
