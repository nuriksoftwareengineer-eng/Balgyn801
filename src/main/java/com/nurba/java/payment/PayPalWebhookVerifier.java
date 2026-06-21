package com.nurba.java.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.config.PayPalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Verifies PayPal webhook signatures using the official
 * POST /v1/notifications/verify-webhook-signature API.
 *
 * This is the recommended approach per PayPal documentation when RSA certificate
 * downloading is not feasible. PayPal validates the signature server-side and
 * returns verification_status = "SUCCESS" or "FAILURE".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayPalWebhookVerifier {

    private final PayPalProperties props;
    private final PayPalTokenClient tokenClient;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Verifies a PayPal webhook event.
     *
     * @param rawBody   the raw (unmodified) request body bytes decoded as UTF-8
     * @param headers   the incoming HTTP headers (case-insensitive lookup done internally)
     * @return true if PayPal confirms the signature is valid
     */
    public boolean verify(String rawBody, Map<String, String> headers) {
        String transmissionId   = getHeader(headers, "paypal-transmission-id");
        String transmissionTime = getHeader(headers, "paypal-transmission-time");
        String certUrl          = getHeader(headers, "paypal-cert-url");
        String authAlgo         = getHeader(headers, "paypal-auth-algo");
        String transmissionSig  = getHeader(headers, "paypal-transmission-sig");

        if (transmissionId == null || transmissionTime == null || certUrl == null
                || authAlgo == null || transmissionSig == null) {
            log.warn("[PayPal] Webhook missing required signature headers");
            return false;
        }

        String verifyBody = buildVerifyBody(transmissionId, transmissionTime, certUrl,
                authAlgo, transmissionSig, rawBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + "/v1/notifications/verify-webhook-signature"))
                .header("Authorization", "Bearer " + tokenClient.getAccessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(verifyBody))
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("[PayPal] verify-webhook-signature returned HTTP {}", response.statusCode());
                return false;
            }
            var root = objectMapper.readTree(response.body());
            String status = root.path("verification_status").asText("");
            boolean valid = "SUCCESS".equalsIgnoreCase(status);
            if (!valid) {
                log.warn("[PayPal] Webhook signature verification_status={}", status);
            }
            return valid;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[PayPal] Webhook verification request failed: {}", e.getMessage());
            return false;
        }
    }

    private String buildVerifyBody(String transmissionId, String transmissionTime,
                                   String certUrl, String authAlgo, String transmissionSig,
                                   String webhookEvent) {
        try {
            String escapedEvent = objectMapper.writeValueAsString(
                    objectMapper.readTree(webhookEvent));
            return """
                    {
                      "transmission_id": "%s",
                      "transmission_time": "%s",
                      "cert_url": "%s",
                      "auth_algo": "%s",
                      "transmission_sig": "%s",
                      "webhook_id": "%s",
                      "webhook_event": %s
                    }
                    """.formatted(
                    transmissionId, transmissionTime, certUrl, authAlgo,
                    transmissionSig, props.getWebhookId(), escapedEvent);
        } catch (IOException e) {
            throw new PayPalApiException("Failed to build webhook verify body: " + e.getMessage(), e);
        }
    }

    private static String getHeader(Map<String, String> headers, String name) {
        if (headers == null) return null;
        // Case-insensitive lookup
        return headers.entrySet().stream()
                .filter(e -> name.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
