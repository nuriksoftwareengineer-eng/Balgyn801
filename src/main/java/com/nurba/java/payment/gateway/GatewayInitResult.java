package com.nurba.java.payment.gateway;

import java.math.BigDecimal;

/**
 * Returned by PaymentGateway.init(). Carries all data needed to persist a Payment entity.
 * Must NOT be written to the database inside the gateway — persistence is PaymentServiceImpl's job.
 */
public record GatewayInitResult(
        String providerPaymentId,  // FreedomPay pg_payment_id / VTB orderId / PayPal order ID
        String redirectUrl,        // URL to redirect the buyer to
        BigDecimal amount,         // amount in payment currency (KZT for FP/VTB; USD for PayPal)
        String currency,           // ISO 4217 alphabetic code ("KZT", "USD", …)
        String cancelToken         // HMAC cancel token — PayPal only; null for all other providers
) {}
