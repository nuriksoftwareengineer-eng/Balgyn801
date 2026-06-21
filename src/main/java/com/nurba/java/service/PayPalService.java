package com.nurba.java.service;

import com.nurba.java.dto.responce.PaymentResponse;

import java.util.Map;

public interface PayPalService {

    /** Creates a PayPal order for the given internal order ID and returns the approval URL. */
    PaymentResponse createOrder(Long orderId, String returnUrl, String cancelUrl);

    /** Captures a previously approved PayPal order (called after buyer approves on PayPal). */
    PaymentResponse captureOrder(String paypalOrderId);

    /** Handles an incoming PayPal webhook event (signature already verified by controller). */
    void handleWebhook(String rawBody, Map<String, String> headers);

    /** Cancels a PENDING PayPal payment when the buyer abandons the PayPal checkout page.
     *  cancelToken must match HMAC-SHA256(paypalOrderId + "|cancel", jwtSecret). */
    PaymentResponse cancelOrder(String paypalOrderId, String cancelToken);
}
