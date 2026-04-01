package com.smartbi.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class EngineConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowCredentials(true);
            }

            @Override
            public void addViewControllers(@NonNull ViewControllerRegistry registry) {
                // Forward any path that does NOT contain a dot and is not handled by a controller to index.html
                registry.addViewController("/{path:[^\\.]*}")
                        .setViewName("forward:/index.html");
                registry.addViewController("/**/{path:[^\\.]*}")
                        .setViewName("forward:/index.html");
            }
        };
    }
}
