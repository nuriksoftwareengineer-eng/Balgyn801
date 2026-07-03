package com.nurba.java.controller;

import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.service.PayPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles PayPal payment cancellation via HMAC cancel token.
 * Called when the buyer clicks "Cancel" on the PayPal approval page.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/paypal")
@RequiredArgsConstructor
public class PayPalCancelController {

    private final PayPalService payPalService;

    @PostMapping("/cancel/{paypalOrderId}")
    public ResponseEntity<PaymentResponse> cancelOrder(
            @PathVariable String paypalOrderId,
            @RequestParam(defaultValue = "") String cancelToken) {
        log.info("[PayPal] cancel request for paypalOrderId={}", paypalOrderId);
        PaymentResponse response = payPalService.cancelOrder(paypalOrderId, cancelToken);
        return ResponseEntity.ok(response);
    }
}
