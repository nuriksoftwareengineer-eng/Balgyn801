package com.nurba.java.controller;

import com.nurba.java.config.FreedomPayProperties;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.payment.FreedomPaySignature;
import com.nurba.java.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Receives payment result callbacks from Freedom Pay.
 *
 * <p>Freedom Pay POSTs to {@code pg_result_url} with form-urlencoded parameters.
 * We verify the MD5 signature, process the payment, and respond with XML.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/callback")
@RequiredArgsConstructor
public class FreedomPayCallbackController {

    private final PaymentService paymentService;
    private final FreedomPayProperties props;

    @PostMapping(
            path = "/freedom-pay",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public ResponseEntity<String> handleCallback(@RequestParam Map<String, String> params) {
        log.info("[FreedomPay] Callback received: pg_order_id={} pg_payment_id={} pg_result={}",
                params.get("pg_order_id"), params.get("pg_payment_id"), params.get("pg_result"));

        // Signature verification
        if (!verifySignature(params)) {
            log.warn("[FreedomPay] Invalid pg_sig in callback. pg_payment_id={}",
                    params.get("pg_payment_id"));
            return ResponseEntity.ok(xmlRejected("Invalid signature"));
        }

        try {
            paymentService.handleCallback(com.nurba.java.enums.PaymentProvider.FREEDOM_PAY, params);
            return ResponseEntity.ok(xmlOk());
        } catch (BusinessRuleException e) {
            log.warn("[FreedomPay] Callback rejected: {}", e.getMessage());
            return ResponseEntity.ok(xmlRejected(e.getMessage()));
        } catch (Exception e) {
            log.error("[FreedomPay] Callback processing error: {}", e.getMessage(), e);
            return ResponseEntity.ok(xmlRejected("Internal error"));
        }
    }

    private boolean verifySignature(Map<String, String> params) {
        if (props.getSecretKey().isBlank()) {
            log.warn("[FreedomPay] secretKey not configured — rejecting unsigned callback");
            return false;
        }
        String received = params.get("pg_sig");
        return FreedomPaySignature.verify(
                props.getCallbackScriptName(), params, props.getSecretKey(), received);
    }

    private static String xmlOk() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<response><pg_status>ok</pg_status></response>";
    }

    private static String xmlRejected(String description) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<response><pg_status>rejected</pg_status>"
                + "<pg_description>" + escapeXml(description) + "</pg_description>"
                + "</response>";
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
