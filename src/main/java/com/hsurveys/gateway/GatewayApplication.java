package com.hsurveys.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	// Optional: Programmatic route configuration (alternative to YAML)
	// You can use either this or the YAML configuration, not both
	/*
	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			.route("user-service", r -> r
				.path("/api/users/**", "/api/auth/**", "/api/roles/**", "/api/permissions/**")
				.uri("http://user-service:8080")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("user-service")
						.setFallbackUri("forward:/fallback/user-service"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter())
						.setKeyResolver(userKeyResolver()))))
			.route("organization-service", r -> r
				.path("/api/organizations/**", "/api/departments/**", "/api/teams/**")
				.uri("http://organization-service:8080")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("organization-service")
						.setFallbackUri("forward:/fallback/organization-service"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter())
						.setKeyResolver(userKeyResolver()))))
			.route("survey-service", r -> r
				.path("/api/surveys/**", "/api/questions/**", "/api/options/**")
				.uri("http://survey-service:8080")
				.filters(f -> f
					.circuitBreaker(config -> config
						.setName("survey-service")
						.setFallbackUri("forward:/fallback/survey-service"))
					.requestRateLimiter(config -> config
						.setRateLimiter(redisRateLimiter())
						.setKeyResolver(userKeyResolver()))))
			.build();
	}
	*/
}
