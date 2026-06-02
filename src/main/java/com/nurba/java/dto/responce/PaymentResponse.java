package com.nurba.java.dto.responce;

import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        PaymentProvider provider,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String providerPaymentId,
        String paymentUrl,
        String webhookEventId,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
