package com.nurba.java.service;

import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.payment.gateway.GatewayCallbackResult;

import java.util.Map;

public interface PaymentService {

    /** Initialises payment via the provider specified in request.provider(). */
    PaymentResponse initPayment(PaymentInitRequest request);

    /** Routes an incoming server-to-server callback to the appropriate gateway. */
    PaymentResponse handleCallback(PaymentProvider provider, Map<String, String> params);

    /** Verifies a customer return redirect from any payment page. */
    PaymentResponse verifyReturn(PaymentProvider provider, Map<String, String> params);

    /** Explicit two-phase capture — currently PayPal only. */
    PaymentResponse capturePayment(PaymentProvider provider, String providerPaymentId);

    /**
     * Applies a gateway result produced outside the standard handleCallback/capture flow —
     * currently used by PayPalWebhookController, whose webhook event ID (replay-dedup key)
     * differs from the PayPal order ID (Payment lookup key, carried in result.eventId()).
     * Goes through the same applyResult() pipeline as every other provider, so payment/order
     * mutation and email/Telegram notifications stay in one place.
     */
    PaymentResponse applyExternalEvent(PaymentProvider provider, String dedupEventId, GatewayCallbackResult result);
}
