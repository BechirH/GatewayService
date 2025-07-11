package com.hsurveys.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/users/**", "/api/auth/**", "/api/roles/**", "/api/permissions/**")
                        .uri("http://localhost:8081"))
                .route("survey-service", r -> r
                        .path("/api/surveys/**", "/api/questions/**", "/api/options/**")
                        .uri("http://localhost:8082"))
                .route("organization-service", r -> r
                        .path("/api/organizations/**", "/api/departments/**", "/api/teams/**")
                        .uri("http://localhost:8083"))
                .build();
    }
} 