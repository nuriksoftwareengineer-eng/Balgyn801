package com.nurba.java.service;

import com.nurba.java.dto.responce.PaymentResponse;

import java.util.Map;

/**
 * Handles the two PayPal flows that don't fit the generic PaymentGateway contract:
 * webhook events (raw body + RSA signature, not Map&lt;String,String&gt;) and buyer-initiated
 * cancel (HMAC token, no provider round-trip). Order creation and capture go through
 * PaymentController → PaymentService → PaymentGatewayRegistry → PayPalGateway instead —
 * see PaymentGateway.handleCallback() javadoc.
 */
public interface PayPalService {

    /** Handles an incoming PayPal webhook event (signature already verified by controller). */
    void handleWebhook(String rawBody, Map<String, String> headers);

    /** Cancels a PENDING PayPal payment when the buyer abandons the PayPal checkout page.
     *  cancelToken must match HMAC-SHA256(paypalOrderId + "|cancel", jwtSecret). */
    PaymentResponse cancelOrder(String paypalOrderId, String cancelToken);
}
