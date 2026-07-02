package com.nurba.java.controller;

import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.PaymentProvider;
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
 * Called by the frontend after the buyer returns from the VTB payment page.
 * VTB appends ?orderId={vtbGatewayUuid} to the returnUrl; the frontend
 * forwards this param here to verify the payment outcome.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/vtb-kz")
@RequiredArgsConstructor
public class VtbReturnController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments/vtb-kz/verify-return
     * Body: { "orderId": "&lt;vtbGatewayUuid&gt;" }
     */
    @PostMapping("/verify-return")
    public ResponseEntity<PaymentResponse> verifyReturn(@RequestBody Map<String, String> params) {
        log.info("[VTB] verify-return called: orderId={}", params.get("orderId"));
        try {
            PaymentResponse result = paymentService.verifyReturn(PaymentProvider.VTB_KZ, params);
            return ResponseEntity.ok(result);
        } catch (BusinessRuleException e) {
            log.warn("[VTB] verify-return rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[VTB] verify-return error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
