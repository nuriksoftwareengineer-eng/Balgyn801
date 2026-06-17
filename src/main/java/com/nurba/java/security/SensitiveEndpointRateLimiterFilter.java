package com.nurba.java.security;

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
 * In-memory sliding-window (60s) rate limiter для чувствительных публичных POST-endpoint'ов:
 * вход, регистрация, отправка кастом-дизайна и создание заказа. Ключ — client IP (учитывает X-Forwarded-For).
 *
 * <p>Назначение: ограничить перебор паролей (login), массовую регистрацию, спам кастом-дизайнов
 * и спам создания заказов. Реализация повторяет {@link com.nurba.java.security.webhook.PaymentRateLimiterFilter}.</p>
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class SensitiveEndpointRateLimiterFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SensitiveEndpointRateLimiterFilter.class);

    private static final String LOGIN_PATH         = "/api/v1/auth/login";
    private static final String REGISTER_PATH      = "/api/v1/auth/register";
    private static final String CUSTOM_DESIGN_PATH = "/api/v1/custom-design";
    private static final String ORDER_PATH         = "/api/v1/order";
    private static final String UPLOAD_PATH        = "/api/v1/media/upload";

    private final SecurityRateLimitProperties properties;

    // ConcurrentHashMap<bucketKey, timestamp deque>
    private final ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = requestPath(request);
        return !LOGIN_PATH.equals(path)
                && !REGISTER_PATH.equals(path)
                && !CUSTOM_DESIGN_PATH.equals(path)
                && !ORDER_PATH.equals(path)
                && !UPLOAD_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = requestPath(request);
        int limit;
        String prefix;
        if (CUSTOM_DESIGN_PATH.equals(path)) {
            limit = properties.getCustomDesignPerMinute();
            prefix = "cd:";
        } else if (ORDER_PATH.equals(path)) {
            limit = properties.getOrderPerMinute();
            prefix = "order:";
        } else if (UPLOAD_PATH.equals(path)) {
            limit = properties.getUploadPerMinute();
            prefix = "upload:";
        } else {
            limit = properties.getAuthPerMinute();
            prefix = "auth:";
        }

        String ip  = resolveClientIp(request);
        String key = prefix + ip;

        if (!allow(key, limit)) {
            log.warn("Rate limit exceeded: ip={} path={}", ip, path);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"title\":\"Too Many Requests\",\"detail\":\"Превышен лимит запросов, попробуйте позже\",\"status\":429}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** Sliding-window check: returns true if the request is within the per-minute limit. */
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
        if (properties.isTrustProxy()) {
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
