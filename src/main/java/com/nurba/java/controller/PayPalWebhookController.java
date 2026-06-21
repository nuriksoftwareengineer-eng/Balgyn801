package com.nurba.java.controller;

import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.service.PayPalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Receives PayPal webhook events.
 *
 * Raw body bytes are preserved and passed to the verifier, which uses the
 * official PayPal verify-webhook-signature API (RSA-SHA256 algorithm, API-backed).
 * Returns HTTP 200 on success, 400 on invalid signature, 500 on internal error.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/paypal")
@RequiredArgsConstructor
public class PayPalWebhookController {

    private final PayPalService payPalService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            HttpServletRequest request) {

        Map<String, String> headers = extractHeaders(request);
        log.info("[PayPal] Webhook received, eventType={}",
                headers.getOrDefault("paypal-event-type", "unknown"));

        try {
            payPalService.handleWebhook(rawBody, headers);
            return ResponseEntity.ok().build();
        } catch (BusinessRuleException e) {
            log.warn("[PayPal] Webhook rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[PayPal] Webhook processing error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private static Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headers.put(name.toLowerCase(), request.getHeader(name));
            }
        }
        return headers;
    }
}
