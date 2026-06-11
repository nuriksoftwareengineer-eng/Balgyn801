package com.nurba.java.security.webhook;

import com.nurba.java.enums.PaymentProvider;
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

/**
 * Verifies the HMAC-SHA256 signature on {@code POST /api/v1/payments/webhook/{provider}}.
 * Wraps the request so the body can be consumed again by the controller.
 * Runs before Spring Security (order 2 in servlet filter chain).
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureFilter.class);
    private static final String WEBHOOK_PATH_PREFIX = "/api/v1/payments/webhook/";

    private final WebhookSignatureService signatureService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !requestPath(request).startsWith(WEBHOOK_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        ReReadableRequestWrapper wrapped = new ReReadableRequestWrapper(request);
        PaymentProvider provider = extractProvider(requestPath(wrapped));

        if (provider == null) {
            // Unknown provider — let the controller return a 400
            chain.doFilter(wrapped, response);
            return;
        }

        String signatureHeader = wrapped.getHeader(WebhookSignatureService.SIGNATURE_HEADER);
        if (!signatureService.isValid(provider, wrapped.getBodyBytes(), signatureHeader)) {
            log.warn("Invalid webhook signature for provider={} path={}",
                    provider, requestPath(wrapped));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"title\":\"Unauthorized\",\"detail\":\"Invalid webhook signature\",\"status\":401}");
            return;
        }

        chain.doFilter(wrapped, response);
    }

    private static PaymentProvider extractProvider(String path) {
        String suffix = path.substring(WEBHOOK_PATH_PREFIX.length());
        int slash = suffix.indexOf('/');
        String name = (slash >= 0) ? suffix.substring(0, slash) : suffix;
        try {
            return PaymentProvider.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Uses requestURI (always populated) because servletPath is set by the dispatcher
     *  servlet AFTER filters run, so getServletPath() returns "" at filter time. */
    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null ? uri : "";
    }
}
