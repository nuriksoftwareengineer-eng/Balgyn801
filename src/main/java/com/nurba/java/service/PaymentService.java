package com.nurba.java.service;

import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.request.PaymentWebhookRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.PaymentProvider;

public interface PaymentService {
    PaymentResponse initPayment(PaymentInitRequest request);

    PaymentResponse handleWebhook(PaymentProvider provider, PaymentWebhookRequest request);
}
