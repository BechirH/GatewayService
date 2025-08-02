package com.hsurveys.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SafeCorsFilter extends AbstractGatewayFilterFactory<SafeCorsFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(SafeCorsFilter.class);

    public SafeCorsFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    try {
                        // Only process if response is not committed and status is not an error
                        if (!exchange.getResponse().isCommitted() && 
                            exchange.getResponse().getStatusCode() != null &&
                            exchange.getResponse().getStatusCode().is2xxSuccessful()) {
                            
                            HttpHeaders headers = exchange.getResponse().getHeaders();
                            
                            // Safely dedupe CORS headers
                            dedupeHeader(headers, "Access-Control-Allow-Origin");
                            dedupeHeader(headers, "Access-Control-Allow-Credentials");
                            dedupeHeader(headers, "Vary");
                            
                            logger.debug("CORS headers processed safely for: {}", 
                                exchange.getRequest().getURI().getPath());
                        } else {
                            logger.debug("Skipping CORS processing - response committed: {}, status: {}", 
                                exchange.getResponse().isCommitted(),
                                exchange.getResponse().getStatusCode());
                        }
                    } catch (Exception e) {
                        logger.debug("Skipping CORS header processing due to: {}", e.getMessage());
                    }
                }));
        };
    }

    private void dedupeHeader(HttpHeaders headers, String headerName) {
        try {
            var values = headers.get(headerName);
            if (values != null && values.size() > 1) {
                String firstValue = values.get(0);
                headers.remove(headerName);
                headers.add(headerName, firstValue);
            }
        } catch (Exception e) {
            logger.debug("Could not dedupe header {}: {}", headerName, e.getMessage());
        }
    }

    public static class Config {
        // Configuration can be added here if needed
    }
} 