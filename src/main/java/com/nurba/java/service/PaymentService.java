package com.nurba.java.service;

import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.responce.PaymentResponse;

import java.util.Map;

public interface PaymentService {
    PaymentResponse initPayment(PaymentInitRequest request);

    PaymentResponse handleFreedomPayCallback(Map<String, String> params);
}
