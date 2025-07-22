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

        // Handle preflight requests
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            logger.debug("Handling CORS preflight request for origin: {}", origin);
            setCorsHeaders(response, origin);
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        // For non-preflight requests, set CORS headers after the chain completes
        return chain.filter(exchange)
                .doOnSuccess(aVoid -> {
                    // Only set CORS headers if response is not committed
                    if (!response.isCommitted()) {
                        setCorsHeaders(response, origin);
                    } else {
                        logger.debug("Response already committed, skipping CORS headers for: {}", request.getPath());
                    }
                })
                .doOnError(throwable -> {
                    // Set CORS headers even on error if response is not committed
                    if (!response.isCommitted()) {
                        setCorsHeaders(response, origin);
                    }
                });
    }

    private void setCorsHeaders(ServerHttpResponse response, String origin) {
        HttpHeaders headers = response.getHeaders();

        // Determine allowed origin
        String allowedOrigin = "*";
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            allowedOrigin = origin;
        } else if (origin != null && ALLOWED_ORIGINS.isEmpty()) {
            // If no specific origins configured, allow the requesting origin
            allowedOrigin = origin;
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
    }

    private void setHeaderIfNotExists(HttpHeaders headers, String headerName, String headerValue) {
        if (!headers.containsKey(headerName)) {
            headers.set(headerName, headerValue);
        } else {
            logger.trace("Header {} already exists, skipping", headerName);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // Run first to ensure CORS is handled properly
    }
}