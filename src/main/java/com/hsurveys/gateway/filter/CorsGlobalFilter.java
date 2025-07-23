package com.hsurveys.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class CorsGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorsGlobalFilter.class);

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:3000",
            "http://46.62.136.95:3000"
    );

    private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, PATCH, OPTIONS";
    private static final String ALLOWED_HEADERS = "Authorization, Content-Type, X-Requested-With, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, X-User-Id, X-Organization-Id";
    private static final String MAX_AGE = "3600";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);
        logger.debug("Processing request from origin: {} for path: {}", origin, request.getPath());

        // Handle preflight requests
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            logger.debug("Handling CORS preflight request for origin: {}", origin);
            setCorsHeaders(response, origin);
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        // For non-preflight requests, set CORS headers immediately
        setCorsHeaders(response, origin);

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    logger.debug("Request completed successfully for: {}", request.getPath());
                })
                .doOnError(throwable -> {
                    logger.error("Request failed for: {}, error: {}", request.getPath(), throwable.getMessage());
                    // Ensure CORS headers are set even on error
                    if (!response.isCommitted()) {
                        setCorsHeaders(response, origin);
                    }
                });
    }

    private void setCorsHeaders(ServerHttpResponse response, String origin) {
        HttpHeaders headers = response.getHeaders();

        // Determine allowed origin - FIXED LOGIC
        String allowedOrigin;
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            allowedOrigin = origin;
            logger.debug("Origin {} is in allowed list, setting as allowed origin", origin);
        } else if (origin != null && !ALLOWED_ORIGINS.isEmpty()) {
            // Origin is not in the allowed list and we have a specific list - reject
            logger.warn("Origin {} is not in allowed origins list: {}", origin, ALLOWED_ORIGINS);
            allowedOrigin = "null"; // This will effectively deny the request
        } else {
            // Fallback - this should rarely be used in production
            allowedOrigin = "*";
            logger.debug("Using wildcard origin");
        }

        // Set CORS headers only if they don't already exist to avoid duplicates
        setHeaderIfNotExists(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        setHeaderIfNotExists(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        setHeaderIfNotExists(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
        setHeaderIfNotExists(headers, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS);
        setHeaderIfNotExists(headers, HttpHeaders.ACCESS_CONTROL_MAX_AGE, MAX_AGE);
        setHeaderIfNotExists(headers, HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Correlation-ID");

        // Set Vary header to handle caching correctly
        setHeaderIfNotExists(headers, HttpHeaders.VARY, "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");

        logger.debug("CORS headers set - Origin: {}, Methods: {}", allowedOrigin, ALLOWED_METHODS);
    }

    private void setHeaderIfNotExists(HttpHeaders headers, String headerName, String headerValue) {
        if (!headers.containsKey(headerName)) {
            headers.set(headerName, headerValue);
            logger.trace("Set header: {} = {}", headerName, headerValue);
        } else {
            logger.trace("Header {} already exists with value: {}", headerName, headers.getFirst(headerName));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // Run first to ensure CORS is handled properly
    }
}