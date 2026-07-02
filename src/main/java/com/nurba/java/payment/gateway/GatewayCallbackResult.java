package com.nurba.java.payment.gateway;

import com.nurba.java.enums.PaymentStatus;

import java.math.BigDecimal;

/**
 * Returned by gateway callback / return / capture handlers.
 * Carries enough data for PaymentServiceImpl.applyResult() to persist the outcome.
 */
public record GatewayCallbackResult(
        String eventId,        // dedup key: pg_payment_id / VTB mdOrder / PayPal orderId
        PaymentStatus status,  // SUCCEEDED / FAILED / PENDING
        BigDecimal amount,     // standard units; null when provider doesn't report it
        String rawPayload      // stored in payment.lastWebhookPayload for audit
) {}
