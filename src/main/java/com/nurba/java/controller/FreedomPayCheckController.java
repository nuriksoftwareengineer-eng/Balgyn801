package com.nurba.java.controller;

import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Called by the frontend after the user returns from the FreedomPay payment page.
 * Accepts all FreedomPay redirect query-params (as JSON), verifies pg_sig locally,
 * and confirms the order — no check_payment.php API call needed.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/freedom-pay")
@RequiredArgsConstructor
public class FreedomPayCheckController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments/freedom-pay/verify-redirect
     *
     * Body: JSON object containing ALL query params from the FreedomPay success redirect URL
     * (pg_payment_id, pg_order_id, pg_amount, pg_currency, pg_result, pg_salt, pg_sig, …).
     *
     * Returns 200 with PaymentResponse on success, 400 on invalid signature.
     */
    @PostMapping("/verify-redirect")
    public ResponseEntity<PaymentResponse> verifyRedirect(
            @RequestBody Map<String, String> redirectParams) {

        log.info("[FreedomPay] verify-redirect called, param keys={}", redirectParams.keySet());
        try {
            PaymentResponse result = paymentService.verifyFreedomPayRedirect(redirectParams);
            return ResponseEntity.ok(result);
        } catch (BusinessRuleException e) {
            log.warn("[FreedomPay] verify-redirect rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[FreedomPay] verify-redirect error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
