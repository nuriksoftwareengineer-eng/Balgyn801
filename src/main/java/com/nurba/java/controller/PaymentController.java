package com.nurba.java.controller;

import com.nurba.java.api.PaymentApi;
import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    @Override
    public PaymentResponse initPayment(PaymentInitRequest request) {
        return paymentService.initPayment(request);
    }
}
