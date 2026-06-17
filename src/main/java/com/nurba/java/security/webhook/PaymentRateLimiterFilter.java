package com.nurba.java.security.webhook;

import com.nurba.java.config.PaymentWebhookProperties;
import com.nurba.java.security.SecurityRateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for payment endpoints.
 * Keyed by client IP (honours X-Forwarded-For for proxied setups).
 * Runs first in the servlet filter chain (order 1).
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class PaymentRateLimiterFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PaymentRateLimiterFilter.class);
    private static final String INIT_PATH     = "/api/v1/payments/init";
    private static final String WEBHOOK_PREFIX = "/api/v1/payments/webhook/";

    private final PaymentWebhookProperties properties;
    private final SecurityRateLimitProperties rateLimitProperties;

    // ConcurrentHashMap<ip_bucketKey, timestamp deque>
    private final ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = requestPath(request);
        return !INIT_PATH.equals(path) && !path.startsWith(WEBHOOK_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path     = requestPath(request);
        boolean isInit  = INIT_PATH.equals(path);
        int limit       = isInit
                ? properties.getInitRateLimitPerMinute()
                : properties.getWebhookRateLimitPerMinute();

        String ip  = resolveClientIp(request);
        String key = (isInit ? "init:" : "hook:") + ip;

        if (!allow(key, limit)) {
            log.warn("Rate limit exceeded: ip={} path={}", ip, path);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"title\":\"Too Many Requests\",\"detail\":\"Rate limit exceeded\",\"status\":429}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** Sliding-window check: returns true if the request is within the per-minute limit. */
    private boolean allow(String key, int limit) {
        Instant now      = Instant.now();
        Instant windowStart = now.minusSeconds(60);

        Deque<Instant> timestamps = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (rateLimitProperties.isTrustProxy()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                return parts[parts.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** Uses requestURI (always populated) because servletPath is empty at filter time. */
    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null ? uri : "";
    }
}
