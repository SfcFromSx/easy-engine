package com.smartbi.query.web;

import com.smartbi.query.config.QueryProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class BasicAuthInterceptor implements HandlerInterceptor {

    private final QueryProperties queryProperties;

    public BasicAuthInterceptor(QueryProperties queryProperties) {
        this.queryProperties = queryProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String expectedUser = queryProperties.getAuth().getUsername();
        String expectedPassword = queryProperties.getAuth().getPassword();
        if (!StringUtils.hasText(expectedUser)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Basic ")) {
            unauthorized(response);
            return false;
        }

        String base64Part = authHeader.substring("Basic ".length()).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(base64Part), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            unauthorized(response);
            return false;
        }

        int colonIndex = decoded.indexOf(':');
        String username = colonIndex >= 0 ? decoded.substring(0, colonIndex) : decoded;
        String password = colonIndex >= 0 ? decoded.substring(colonIndex + 1) : "";

        if (!expectedUser.equals(username) || !safeEquals(expectedPassword, password)) {
            unauthorized(response);
            return false;
        }
        return true;
    }

    private static void unauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"engine-query\"");
        response.getWriter().write("{\"message\":\"unauthorized\"}");
    }

    private static boolean safeEquals(String left, String right) {
        String l = left == null ? "" : left;
        String r = right == null ? "" : right;
        return l.equals(r);
    }
}
