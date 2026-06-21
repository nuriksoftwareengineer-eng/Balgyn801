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
 * Covers {@code POST /api/v1/payments/init} and {@code POST /api/v1/payments/callback/**}.
 * Keyed by client IP. Runs first in the servlet filter chain (order 1).
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class PaymentRateLimiterFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PaymentRateLimiterFilter.class);
    private static final String INIT_PATH                = "/api/v1/payments/init";
    private static final String CALLBACK_PREFIX          = "/api/v1/payments/callback/";
    private static final String PAYPAL_WEBHOOK_PATH      = "/api/v1/payments/paypal/webhook";
    private static final String PAYPAL_CREATE_ORDER_PATH = "/api/v1/payments/paypal/create-order";
    private static final String PAYPAL_CAPTURE_PREFIX    = "/api/v1/payments/paypal/capture/";

    private final PaymentWebhookProperties properties;
    private final SecurityRateLimitProperties rateLimitProperties;

    private final ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = requestPath(request);
        return !INIT_PATH.equals(path)
                && !path.startsWith(CALLBACK_PREFIX)
                && !PAYPAL_WEBHOOK_PATH.equals(path)
                && !PAYPAL_CREATE_ORDER_PATH.equals(path)
                && !path.startsWith(PAYPAL_CAPTURE_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = requestPath(request);
        boolean isUserFacing = INIT_PATH.equals(path)
                || PAYPAL_CREATE_ORDER_PATH.equals(path)
                || path.startsWith(PAYPAL_CAPTURE_PREFIX);
        int limit = isUserFacing
                ? properties.getInitRateLimitPerMinute()
                : properties.getWebhookRateLimitPerMinute();

        String ip = resolveClientIp(request);
        String keyPrefix = INIT_PATH.equals(path) ? "init:"
                : PAYPAL_CREATE_ORDER_PATH.equals(path) ? "paypal-create:"
                : path.startsWith(PAYPAL_CAPTURE_PREFIX) ? "paypal-capture:"
                : PAYPAL_WEBHOOK_PATH.equals(path) ? "paypal-webhook:"
                : "callback:";
        String key = keyPrefix + ip;

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

    private boolean allow(String key, int limit) {
        Instant now         = Instant.now();
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

    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null ? uri : "";
    }
}
