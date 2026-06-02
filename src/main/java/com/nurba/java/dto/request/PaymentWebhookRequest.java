package com.nurba.java.dto.request;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentWebhookRequest(
        String eventId,
        @Positive Long paymentId,
        @Positive Long orderId,
        String providerPaymentId,
        String status,
        BigDecimal amount,
        String currency,
        Map<String, Object> payload
) {
}
