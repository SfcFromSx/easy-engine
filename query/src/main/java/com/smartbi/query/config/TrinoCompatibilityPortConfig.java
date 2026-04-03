package com.smartbi.query.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrinoCompatibilityPortConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> trinoCompatibilityConnectorCustomizer(
            QueryProperties queryProperties) {
        return factory -> {
            int trinoPort = queryProperties.getTrino().getPort();
            Integer primaryPort = factory.getPort();
            if (trinoPort <= 0 || (primaryPort != null && trinoPort == primaryPort.intValue())) {
                return;
            }
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setScheme("http");
            connector.setSecure(false);
            connector.setPort(trinoPort);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}
