package com.hsurveys.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Enumeration;

@RestController
@RequestMapping("/api")
public class GatewayController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://localhost:8081";
    private static final String SURVEY_SERVICE_URL = "http://localhost:8082";
    private static final String ORGANIZATION_SERVICE_URL = "http://localhost:8083";

    @RequestMapping(value = {"/users/**", "/auth/**", "/roles/**", "/permissions/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> routeToUserService(HttpServletRequest request, @RequestBody(required = false) String body) {
        return routeRequest(request, USER_SERVICE_URL, body);
    }

    @RequestMapping(value = {"/surveys/**", "/questions/**", "/options/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> routeToSurveyService(HttpServletRequest request, @RequestBody(required = false) String body) {
        return routeRequest(request, SURVEY_SERVICE_URL, body);
    }

    @RequestMapping(value = {"/organizations/**", "/departments/**", "/teams/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> routeToOrganizationService(HttpServletRequest request, @RequestBody(required = false) String body) {
        return routeRequest(request, ORGANIZATION_SERVICE_URL, body);
    }

    private ResponseEntity<String> routeRequest(HttpServletRequest request, String baseUrl, String body) {
        try {
            String requestURI = request.getRequestURI();
            String queryString = request.getQueryString();
            String fullUrl = baseUrl + requestURI;
            if (queryString != null) {
                fullUrl += "?" + queryString;
            }

            HttpHeaders headers = new HttpHeaders();
            
            // Copy all original headers
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                headers.add(headerName, headerValue);
            }

            // Add user context headers from request attributes (set by JWT filter)
            String userId = (String) request.getAttribute("X-User-Id");
            String username = (String) request.getAttribute("X-Username");
            String userName = (String) request.getAttribute("X-User-Name");
            String organizationId = (String) request.getAttribute("X-Organization-Id");
            String departmentId = (String) request.getAttribute("X-Department-Id");
            String teamId = (String) request.getAttribute("X-Team-Id");
            String authorities = (String) request.getAttribute("X-Authorities");
            String userAuthorities = (String) request.getAttribute("X-User-Authorities");
            String authenticated = (String) request.getAttribute("X-Authenticated");

            if (userId != null) {
                headers.add("X-User-Id", userId);
            }
            if (username != null) {
                headers.add("X-Username", username);
            }
            if (userName != null) {
                headers.add("X-User-Name", userName);
            }
            if (organizationId != null) {
                headers.add("X-Organization-Id", organizationId);
            }
            if (departmentId != null) {
                headers.add("X-Department-Id", departmentId);
            }
            if (teamId != null) {
                headers.add("X-Team-Id", teamId);
            }
            if (authorities != null) {
                headers.add("X-Authorities", authorities);
            }
            if (userAuthorities != null) {
                headers.add("X-User-Authorities", userAuthorities);
            }
            if (authenticated != null) {
                headers.add("X-Authenticated", authenticated);
            }

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            HttpMethod method = HttpMethod.valueOf(request.getMethod());

            return restTemplate.exchange(URI.create(fullUrl), method, entity, String.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Gateway Error: " + e.getMessage());
        }
    }
} 