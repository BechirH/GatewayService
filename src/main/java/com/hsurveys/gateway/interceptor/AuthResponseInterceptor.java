package com.hsurveys.gateway.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsurveys.gateway.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class AuthResponseInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthResponseInterceptor.class);
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthResponseInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Only process successful auth responses
        if (isAuthEndpoint(request.getRequestURI()) && response.getStatus() == 200) {
            try {
                // Generate JWT token for successful authentication
                String username = extractUsernameFromResponse(response);
                if (username != null) {
                    String jwtToken = generateJwtToken(username);
                    setJwtCookie(response, jwtToken);
                    logger.debug("Generated JWT token for user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Error generating JWT token", e);
            }
        }
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/refresh");
    }

    private String extractUsernameFromResponse(HttpServletResponse response) {
        // This is a simplified approach - in a real implementation,
        // you might need to capture the response body
        // For now, we'll generate a token based on the request context
        return "authenticated_user"; // Placeholder
    }

    private String generateJwtToken(String username) {
        // Generate a basic token - in a real implementation,
        // you would extract user details from the authentication response
        return jwtUtil.generateToken(
            username,
            UUID.randomUUID(), // userId
            UUID.randomUUID(), // organizationId
            null, // departmentId
            null, // teamId
            List.of("USER") // authorities
        );
    }

    private void setJwtCookie(HttpServletResponse response, String jwtToken) {
        Cookie accessCookie = new Cookie("access_token", jwtToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 15); // 15 minutes
        response.addCookie(accessCookie);
    }
} 