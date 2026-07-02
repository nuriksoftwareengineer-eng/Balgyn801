package com.nurba.java.payment;

import com.nurba.java.enums.PaymentStatus;

/**
 * Result of a Freedom Pay check_payment.php call.
 *
 * @param status       mapped PaymentStatus (SUCCEEDED/FAILED/PENDING), or null if the API call itself failed
 * @param errorMessage human-readable error (null on success)
 */
public record FreedomPayCheckResult(PaymentStatus status, String errorMessage) {}
