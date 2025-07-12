package com.hsurveys.gateway.filter;

import com.hsurveys.gateway.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            
            logger.debug("Processing request: {}", path);

            // Skip authentication for auth endpoints
            if (isAuthEndpoint(path)) {
                logger.debug("Skipping authentication for auth endpoint: {}", path);
                return chain.filter(exchange);
            }

            // Extract token from Authorization header or cookies
            String token = extractTokenFromRequest(request);
            
            if (token == null) {
                logger.warn("No token found in request: {}", path);
                return unauthorizedResponse(exchange, "No authentication token found");
            }

            try {
                // Validate token
                if (!jwtUtil.validateToken(token)) {
                    logger.warn("Invalid token for request: {}", path);
                    return unauthorizedResponse(exchange, "Invalid authentication token");
                }

                // Extract user information
                String username = jwtUtil.extractUsername(token);
                UUID userId = jwtUtil.extractUserId(token);
                UUID organizationId = jwtUtil.extractOrganizationId(token);
                UUID departmentId = jwtUtil.extractDepartmentId(token);
                UUID teamId = jwtUtil.extractTeamId(token);
                List<String> authorities = jwtUtil.extractAuthorities(token);

                logger.debug("Token validated for user: {} in organization: {}", username, organizationId);

                // Add user context to request headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId.toString() : "")
                        .header("X-Username", username != null ? username : "")
                        .header("X-User-Name", username != null ? username : "") // For Organization service
                        .header("X-Organization-Id", organizationId != null ? organizationId.toString() : "")
                        .header("X-Department-Id", departmentId != null ? departmentId.toString() : "")
                        .header("X-Team-Id", teamId != null ? teamId.toString() : "")
                        .header("X-Authorities", String.join(",", authorities))
                        .header("X-User-Authorities", String.join(",", authorities)) // For Organization service
                        .header("X-Authenticated", "true")
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                logger.error("Error processing token for request: {}", path, e);
                return unauthorizedResponse(exchange, "Token processing error");
            }
        };
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/users/") && path.contains("/exists") ||
               path.startsWith("/api/users/bulk") ||
               path.startsWith("/api/organizations") && path.contains("/exists") ||
               path.startsWith("/api/departments/") && path.contains("/exists") ||
               path.startsWith("/api/teams/") && path.contains("/exists") ||
               path.startsWith("/api/departments/user/") ||
               path.startsWith("/api/teams/user/");
    }

    private String extractTokenFromRequest(ServerHttpRequest request) {
        // First, try to get token from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // If not found in header, try to get from cookies
        List<String> cookies = request.getHeaders().get(HttpHeaders.COOKIE);
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.contains("access_token=")) {
                    String[] parts = cookie.split("access_token=");
                    if (parts.length > 1) {
                        String tokenPart = parts[1];
                        // Remove any additional cookie parameters
                        if (tokenPart.contains(";")) {
                            tokenPart = tokenPart.split(";")[0];
                        }
                        return tokenPart;
                    }
                }
            }
        }

        return null;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        String responseBody = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties if needed
    }
} 