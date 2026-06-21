package com.nurba.java.payment;

/**
 * Result of a Freedom Pay init_payment.php call.
 *
 * @param providerPaymentId Freedom Pay's pg_payment_id (null on error or stub)
 * @param redirectUrl       URL to redirect the customer to (null on error)
 * @param success           true when Freedom Pay accepted the init request
 * @param errorDescription  human-readable error from Freedom Pay (null on success)
 */
public record FreedomPayInitResult(
        String providerPaymentId,
        String redirectUrl,
        boolean success,
        String errorDescription
) {}
