package com.nurba.java.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.config.PayPalProperties;
import com.nurba.java.payment.dto.PayPalCaptureResponse;
import com.nurba.java.payment.dto.PayPalCreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * HTTP client for PayPal Orders API v2.
 * Handles create-order and capture-order operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayPalOrdersClient {

    private final PayPalProperties props;
    private final PayPalTokenClient tokenClient;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.stub.base-url:http://localhost:8080}")
    private String stubBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Creates a PayPal order with CAPTURE intent.
     *
     * @param amountUsd   amount in USD (already converted from KZT)
     * @param currency    currency code, e.g. "USD"
     * @return parsed response containing PayPal order ID and approval URL
     */
    public PayPalCreateOrderResponse createOrder(BigDecimal amountUsd, String currency,
                                                  String returnUrl, String cancelUrl) {
        if (props.isStubMode()) {
            String stubId = "stub-pp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.info("[PAYMENT-STUB] PayPal running in stub mode — no real API call. stubOrderId={}", stubId);
            String approvalUrl = stubBaseUrl + "/api/v1/payments/stub/paypal/approve?paypalOrderId=" + stubId;
            if (returnUrl != null && !returnUrl.isBlank()) {
                approvalUrl += "&returnUrl=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
            }
            return new PayPalCreateOrderResponse(
                    stubId, "CREATED",
                    List.of(new PayPalCreateOrderResponse.Link(approvalUrl, "payer-action", "GET")));
        }
        String url = props.getBaseUrl() + "/v2/checkout/orders";
        log.info("[PayPal] createOrder → POST {} amount={} {} (mode={})", url, amountUsd, currency, props.getMode());
        String body = buildCreateOrderBody(amountUsd.toPlainString(), currency, returnUrl, cancelUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + tokenClient.getAccessToken())
                .header("Content-Type", "application/json")
                .header("PayPal-Request-Id", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                log.error("[PayPal] createOrder failed: HTTP {} body={}", response.statusCode(), response.body());
                throw new PayPalApiException("PayPal createOrder failed: HTTP " + response.statusCode()
                        + " body=" + response.body());
            }
            log.info("[PayPal] createOrder OK: HTTP {} ", response.statusCode());
            return objectMapper.readValue(response.body(), PayPalCreateOrderResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[PayPal] createOrder transport error to {}: {}", url, e.toString());
            throw new PayPalApiException("PayPal createOrder request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Captures a previously approved PayPal order.
     *
     * @param paypalOrderId the PayPal order ID returned by {@link #createOrder}
     * @return parsed capture response
     */
    public PayPalCaptureResponse captureOrder(String paypalOrderId) {
        if (props.isStubMode()) {
            log.info("[PAYMENT-STUB] PayPal captureOrder stub — returning COMPLETED. paypalOrderId={}", paypalOrderId);
            return new PayPalCaptureResponse(paypalOrderId, "COMPLETED", null);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + "/v2/checkout/orders/" + paypalOrderId + "/capture"))
                .header("Authorization", "Bearer " + tokenClient.getAccessToken())
                .header("Content-Type", "application/json")
                .header("PayPal-Request-Id", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("[PayPal] captureOrder orderId={} failed: HTTP {} body={}",
                        paypalOrderId, response.statusCode(), response.body());
                throw new PayPalApiException("PayPal captureOrder failed: HTTP " + response.statusCode()
                        + " body=" + response.body());
            }
            log.info("[PayPal] captureOrder orderId={} OK: HTTP {}", paypalOrderId, response.statusCode());
            return objectMapper.readValue(response.body(), PayPalCaptureResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[PayPal] captureOrder orderId={} transport error: {}", paypalOrderId, e.toString());
            throw new PayPalApiException("PayPal captureOrder request failed: " + e.getMessage(), e);
        }
    }

    private static String buildCreateOrderBody(String amount, String currency,
                                               String returnUrl, String cancelUrl) {
        if (returnUrl != null && !returnUrl.isBlank()) {
            String effectiveCancel = (cancelUrl != null && !cancelUrl.isBlank()) ? cancelUrl : returnUrl;
            return """
                    {
                      "intent": "CAPTURE",
                      "purchase_units": [{
                        "amount": {
                          "currency_code": "%s",
                          "value": "%s"
                        }
                      }],
                      "application_context": {
                        "return_url": "%s",
                        "cancel_url": "%s",
                        "user_action": "PAY_NOW"
                      }
                    }
                    """.formatted(currency, amount, returnUrl, effectiveCancel);
        }
        return """
                {
                  "intent": "CAPTURE",
                  "purchase_units": [{
                    "amount": {
                      "currency_code": "%s",
                      "value": "%s"
                    }
                  }]
                }
                """.formatted(currency, amount);
    }
}
