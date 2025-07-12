package com.hsurveys.gateway.filter;

import com.hsurveys.gateway.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        logger.debug("Processing request: {}", path);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            logger.debug("Skipping authentication for public endpoint: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token from Authorization header or cookies
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            logger.warn("No token found in request: {}", path);
            sendUnauthorizedResponse(response, "No authentication token found");
            return;
        }

        try {
            // Validate token
            if (!jwtUtil.validateToken(token)) {
                logger.warn("Invalid token for request: {}", path);
                sendUnauthorizedResponse(response, "Invalid authentication token");
                return;
            }

            // Extract user information
            String username = jwtUtil.extractUsername(token);
            UUID userId = jwtUtil.extractUserId(token);
            UUID organizationId = jwtUtil.extractOrganizationId(token);
            UUID departmentId = jwtUtil.extractDepartmentId(token);
            UUID teamId = jwtUtil.extractTeamId(token);
            List<String> authorities = jwtUtil.extractAuthorities(token);
            List<String> roles = jwtUtil.extractRoles(token);

            logger.debug("Token validated for user: {} in organization: {}", username, organizationId);

            // Add user context to request headers for downstream services
            if (userId != null) {
                request.setAttribute("X-User-Id", userId.toString());
            }
            if (username != null) {
                request.setAttribute("X-Username", username);
                request.setAttribute("X-User-Name", username); // For Organization service
            }
            if (organizationId != null) {
                request.setAttribute("X-Organization-Id", organizationId.toString());
            }
            if (departmentId != null) {
                request.setAttribute("X-Department-Id", departmentId.toString());
            }
            if (teamId != null) {
                request.setAttribute("X-Team-Id", teamId.toString());
            }
            if (authorities != null && !authorities.isEmpty()) {
                String authoritiesStr = String.join(",", authorities);
                request.setAttribute("X-Authorities", authoritiesStr);
                request.setAttribute("X-User-Authorities", authoritiesStr); // For Organization service
            }
            if (roles != null && !roles.isEmpty()) {
                String rolesStr = String.join(",", roles);
                request.setAttribute("X-Roles", rolesStr);
                request.setAttribute("X-User-Roles", rolesStr); // For Organization service
            }
            request.setAttribute("X-Authenticated", "true");

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error processing token for request: {}", path, e);
            sendUnauthorizedResponse(response, "Token processing error");
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/organizations/register");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        // First, try to get token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // If not found in header, try to get from cookies
        String cookies = request.getHeader("Cookie");
        if (cookies != null) {
            String[] cookieArray = cookies.split(";");
            for (String cookie : cookieArray) {
                cookie = cookie.trim();
                if (cookie.startsWith("access_token=")) {
                    return cookie.substring("access_token=".length());
                }
            }
        }

        return null;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String responseBody = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        response.getWriter().write(responseBody);
    }
} 