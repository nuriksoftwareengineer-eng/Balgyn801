package com.nurba.java.controller;

import com.nurba.java.config.FreedomPayProperties;
import com.nurba.java.config.PayPalProperties;
import org.springframework.beans.factory.annotation.Value;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
import com.nurba.java.domain.ProcessedWebhookEvent;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.repositories.ProcessedWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Local payment stub endpoints — only active when payment credentials are blank.
 * <p>
 * PayPal stub: simulates the PayPal approval-page redirect so the normal
 * capture flow works without real PayPal credentials.
 * <p>
 * FreedomPay stub: auto-confirms the order and redirects to the success URL,
 * bypassing the real FreedomPay callback.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/stub")
@RequiredArgsConstructor
public class PaymentStubController {

    @Value("${app.frontend.base-url:http://localhost:5174}")
    private String frontendBaseUrl;

    private final PayPalProperties payPalProperties;
    private final FreedomPayProperties freedomPayProperties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedEventRepository;

    /**
     * Simulates PayPal approval-page redirect.
     * Appends {@code token} and {@code PayerID} to {@code returnUrl} — mirroring real PayPal.
     * The frontend then calls POST /paypal/capture/{token} as normal.
     */
    @GetMapping("/paypal/approve")
    public ResponseEntity<Void> approvePayPal(
            @RequestParam String paypalOrderId,
            @RequestParam(required = false, defaultValue = "") String returnUrl) {

        if (!payPalProperties.isStubMode()) {
            return ResponseEntity.notFound().build();
        }

        log.info("[PAYMENT-STUB] PayPal approve: paypalOrderId={}", paypalOrderId);

        String location;
        if (!returnUrl.isBlank()) {
            String sep = returnUrl.contains("?") ? "&" : "?";
            location = returnUrl + sep + "token=" + paypalOrderId + "&PayerID=STUB_PAYER";
        } else {
            location = "/";
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }

    /**
     * Simulates FreedomPay payment completion.
     * Sets payment to SUCCEEDED and order to CONFIRMED, then redirects to the success URL.
     */
    @GetMapping("/freedom-pay/approve")
    @Transactional
    public ResponseEntity<Void> approveFreedomPay(@RequestParam Long orderId) {

        if (!freedomPayProperties.isStubMode()) {
            return ResponseEntity.notFound().build();
        }

        log.info("[PAYMENT-STUB] FreedomPay approve: orderId={}", orderId);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("[PAYMENT-STUB] Order {} not found", orderId);
            return ResponseEntity.notFound().build();
        }

        Optional<Payment> pending = paymentRepository.findByOrderAndProviderAndStatus(
                order, PaymentProvider.FREEDOM_PAY, PaymentStatus.PENDING);

        if (pending.isEmpty()) {
            log.warn("[PAYMENT-STUB] No PENDING FreedomPay payment for orderId={}", orderId);
            return ResponseEntity.notFound().build();
        }

        Payment payment = pending.get();
        String providerPaymentId = payment.getProviderPaymentId();

        if (!processedEventRepository.existsByProviderAndEventId(
                PaymentProvider.FREEDOM_PAY, providerPaymentId)) {

            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setLastWebhookPayload("stub-auto-confirm");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            if (order.getStatus() == OrderStatus.PENDING_PAYMENT
                    || order.getStatus() == OrderStatus.NEW) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("[PAYMENT-STUB] Order #{} confirmed via FreedomPay stub", orderId);
            }

            ProcessedWebhookEvent pwe = new ProcessedWebhookEvent();
            pwe.setProvider(PaymentProvider.FREEDOM_PAY);
            pwe.setEventId(providerPaymentId);
            pwe.setPayment(payment);
            pwe.setProcessedAt(LocalDateTime.now());
            processedEventRepository.save(pwe);
        } else {
            log.info("[PAYMENT-STUB] FreedomPay already processed: {}", providerPaymentId);
        }

        // Stub already confirmed payment + order — go directly to success page.
        // Do NOT use freedomPayProperties.getSuccessUrl() here: that now points to /payment-return
        // (for real FreedomPay flow), but the stub doesn't need the check_payment.php verification step.
        String location = frontendBaseUrl + "/payment/success?orderId=" + orderId;

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }
}
