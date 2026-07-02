package com.nurba.java.controller;

import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Receives payment event callbacks from VTB Kazakhstan.
 * VTB sends GET requests (confirmed from the official WooCommerce plugin source).
 * Always responds HTTP 200 — VTB does not retry on non-200 in a predictable way.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/callback")
@RequiredArgsConstructor
public class VtbCallbackController {

    private final PaymentService paymentService;

    @GetMapping("/vtb-kz")
    public ResponseEntity<Void> handleCallback(@RequestParam Map<String, String> params) {
        log.info("[VTB] Callback received: mdOrder={} operation={}",
                params.get("mdOrder"), params.get("operation"));
        try {
            paymentService.handleCallback(PaymentProvider.VTB_KZ, params);
        } catch (BusinessRuleException e) {
            log.warn("[VTB] Callback rejected: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[VTB] Callback processing error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }
}
