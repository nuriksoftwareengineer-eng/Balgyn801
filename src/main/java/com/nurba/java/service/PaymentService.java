package com.nurba.java.service;

import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.responce.PaymentResponse;

import java.util.Map;

public interface PaymentService {
    PaymentResponse initPayment(PaymentInitRequest request);

    PaymentResponse handleFreedomPayCallback(Map<String, String> params);

    /**
     * Verifies the pg_sig from FreedomPay's browser success redirect and confirms the order.
     * Called when the user returns to pg_success_url; check_payment.php is NOT used.
     */
    PaymentResponse verifyFreedomPayRedirect(Map<String, String> redirectParams);
}
