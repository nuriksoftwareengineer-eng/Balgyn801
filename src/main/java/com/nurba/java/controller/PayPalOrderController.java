package com.nurba.java.controller;

import com.nurba.java.dto.request.PayPalCreateOrderRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.service.PayPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PayPal Orders API v2 — customer-facing endpoints.
 *
 * Flow:
 *  1. POST /create-order   → returns PayPal approval URL; buyer navigates there.
 *  2. POST /capture/{id}   → called after buyer approves; captures the funds.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/paypal")
@RequiredArgsConstructor
public class PayPalOrderController {

    private final PayPalService payPalService;

    /** Creates a PayPal order for the given internal order ID. */
    @PostMapping("/create-order")
    public ResponseEntity<PaymentResponse> createOrder(@RequestBody PayPalCreateOrderRequest request) {
        if (request.orderId() == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("[PayPal] create-order request for orderId={}", request.orderId());
        PaymentResponse response = payPalService.createOrder(
                request.orderId(), request.returnUrl(), request.cancelUrl());
        return ResponseEntity.ok(response);
    }

    /** Captures a previously approved PayPal order. */
    @PostMapping("/capture/{paypalOrderId}")
    public ResponseEntity<PaymentResponse> captureOrder(@PathVariable String paypalOrderId) {
        log.info("[PayPal] capture request for paypalOrderId={}", paypalOrderId);
        PaymentResponse response = payPalService.captureOrder(paypalOrderId);
        return ResponseEntity.ok(response);
    }

    /** Called when a buyer abandons the PayPal checkout page (cancel URL redirect). */
    @PostMapping("/cancel/{paypalOrderId}")
    public ResponseEntity<PaymentResponse> cancelOrder(
            @PathVariable String paypalOrderId,
            @RequestParam(defaultValue = "") String cancelToken) {
        log.info("[PayPal] cancel request for paypalOrderId={}", paypalOrderId);
        PaymentResponse response = payPalService.cancelOrder(paypalOrderId, cancelToken);
        return ResponseEntity.ok(response);
    }
}
