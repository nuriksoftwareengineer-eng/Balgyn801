package com.nurba.java.payment.gateway;

import com.nurba.java.domain.Order;
import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.enums.PaymentProvider;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Strategy interface — one Spring @Service bean per payment provider.
 * PaymentGatewayRegistry auto-collects all implementations via List<PaymentGateway> injection.
 */
public interface PaymentGateway {

    PaymentProvider getProvider();

    /**
     * Initialise a payment session with the provider.
     * Must return a redirect URL and provider payment ID.
     * Must NOT write to the database — that is PaymentServiceImpl's responsibility.
     */
    GatewayInitResult init(Order order, PaymentInitRequest request);

    /**
     * Process an incoming server-to-server callback (webhook) from the provider.
     * FreedomPay: POST params (MD5 signature already verified by the controller).
     * VTB:        GET params — calls getOrderStatusExtended internally.
     * PayPal:     throws UnsupportedOperationException; handled by PayPalWebhookController.
     */
    GatewayCallbackResult handleCallback(Map<String, String> params);

    /**
     * Process a customer return redirect from the payment page.
     * FreedomPay: verifies pg_sig and maps the result.
     * VTB:        calls getOrderStatusExtended with the ?orderId= query param.
     * PayPal:     throws UnsupportedOperationException; PayPal uses capture() instead.
     */
    default GatewayCallbackResult verifyReturn(Map<String, String> params) {
        throw new UnsupportedOperationException(
                getProvider() + " does not support return-URL verification");
    }

    /**
     * Two-phase capture: confirm a pre-authorised hold.
     * PayPal: POST /v2/checkout/orders/{id}/capture.
     * Others: throws UnsupportedOperationException (single-phase flow).
     */
    default GatewayCallbackResult capture(String providerPaymentId) {
        throw new UnsupportedOperationException(
                getProvider() + " does not support explicit capture");
    }

    /**
     * Refund a completed payment.
     * VTB: refund.do (DEPOSITED) or reverse.do (PRE_AUTHORIZED).
     * Default: throws UnsupportedOperationException.
     */
    default void refund(String providerPaymentId, BigDecimal amount) {
        throw new UnsupportedOperationException(
                getProvider() + " refund is not implemented");
    }

    /**
     * Cancel an unpaid/pending payment.
     * VTB: cancel.do.
     * PayPal: throws UnsupportedOperationException; cancel uses HMAC token — handled separately.
     * Default: throws UnsupportedOperationException.
     */
    default void cancel(String providerPaymentId) {
        throw new UnsupportedOperationException(
                getProvider() + " cancel is not implemented");
    }

    /**
     * Generate a secure cancel token for this provider payment.
     * Only PayPalGateway overrides this (HMAC-SHA256 of orderId + "|cancel").
     * All other providers return null.
     */
    default String generateCancelToken(String providerPaymentId) {
        return null;
    }
}
