package com.nurba.java.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
import com.nurba.java.domain.ProcessedWebhookEvent;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.payment.PayPalOrdersClient;
import com.nurba.java.payment.PayPalWebhookVerifier;
import com.nurba.java.payment.dto.PayPalCaptureResponse;
import com.nurba.java.payment.dto.PayPalCreateOrderResponse;
import com.nurba.java.payment.dto.PayPalWebhookEvent;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.repositories.ProcessedWebhookEventRepository;
import com.nurba.java.service.ExchangeRateService;
import com.nurba.java.service.PayPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalServiceImpl implements PayPalService {

    private static final String EVENT_CAPTURE_COMPLETED = "PAYMENT.CAPTURE.COMPLETED";
    private static final String EVENT_CAPTURE_DENIED    = "PAYMENT.CAPTURE.DENIED";
    private static final String EVENT_CAPTURE_REFUNDED  = "PAYMENT.CAPTURE.REFUNDED";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedEventRepository;
    private final OrderExpiryService orderExpiryService;
    private final PayPalOrdersClient payPalOrdersClient;
    private final PayPalWebhookVerifier webhookVerifier;
    private final ExchangeRateService exchangeRateService;
    private final ObjectMapper objectMapper;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    // ─────────────────────────────────────────────────────────────────────────
    // Create order
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse createOrder(Long orderId, String returnUrl, String cancelUrl) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.EXPIRED) {
            throw new BusinessRuleException("Для этого заказа нельзя инициализировать оплату PayPal");
        }
        if (order.getTotalPrice() == null || order.getTotalPrice().signum() <= 0) {
            throw new BusinessRuleException("Некорректная сумма заказа");
        }

        // Idempotency: reuse existing PENDING PayPal payment for this order
        Payment payment = paymentRepository
                .findByOrderAndProviderAndStatus(order, PaymentProvider.PAYPAL, PaymentStatus.PENDING)
                .orElseGet(() -> createNewPayPalPayment(order, returnUrl, cancelUrl));
        return toResponseWithCancelToken(payment);
    }

    private Payment createNewPayPalPayment(Order order, String returnUrl, String cancelUrl) {
        BigDecimal kztPerUsd = exchangeRateService.kztPerUsd();
        BigDecimal amountUsd = order.getTotalPrice()
                .divide(kztPerUsd, 2, RoundingMode.HALF_UP);

        PayPalCreateOrderResponse ppOrder = payPalOrdersClient.createOrder(amountUsd, "USD", returnUrl, cancelUrl);

        String approvalUrl = ppOrder.approvalUrl();
        if (approvalUrl == null) {
            throw new BusinessRuleException("PayPal не вернул ссылку на подтверждение оплаты");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.PAYPAL);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amountUsd);
        payment.setCurrency("USD");
        payment.setProviderPaymentId(ppOrder.id());
        payment.setPaymentUrl(approvalUrl);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        log.info("[PayPal] Created order {} for order #{}, amountUsd={}", ppOrder.id(), order.getId(), amountUsd);
        return paymentRepository.save(payment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cancel order (buyer abandoned PayPal checkout)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse cancelOrder(String paypalOrderId, String cancelToken) {
        // Verify HMAC-signed cancel token when jwt.secret is configured.
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            String expected = generateCancelToken(paypalOrderId);
            String received = cancelToken != null ? cancelToken : "";
            boolean valid = MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    received.getBytes(StandardCharsets.UTF_8));
            if (!valid) {
                log.warn("[PayPal] cancelOrder rejected: invalid cancelToken for paypalOrderId={}", paypalOrderId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid cancel token");
            }
        }

        Payment payment = paymentRepository.findByProviderPaymentId(paypalOrderId)
                .orElseThrow(() -> new NotFoundException("Платёж PayPal не найден: " + paypalOrderId));

        if (payment.getProvider() != PaymentProvider.PAYPAL) {
            throw new BusinessRuleException("providerPaymentId принадлежит другому провайдеру");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("[PayPal] cancelOrder: payment already in status={}, skipping", payment.getStatus());
            return toResponse(payment);
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setErrorMessage("Покупатель отменил оплату на странице PayPal");
        payment.setUpdatedAt(LocalDateTime.now());

        Order order = payment.getOrder();
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            orderExpiryService.expire(order);
        }

        log.info("[PayPal] Payment {} cancelled by buyer for order #{}", paypalOrderId, order.getId());
        return toResponse(paymentRepository.save(payment));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Capture order
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse captureOrder(String paypalOrderId) {
        Payment payment = paymentRepository.findByProviderPaymentIdForUpdate(paypalOrderId)
                .orElseThrow(() -> new NotFoundException("Платёж PayPal не найден: " + paypalOrderId));

        if (payment.getProvider() != PaymentProvider.PAYPAL) {
            throw new BusinessRuleException("providerPaymentId принадлежит другому провайдеру");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("[PayPal] captureOrder: payment already in status={}, skipping", payment.getStatus());
            return toResponse(payment);
        }

        PayPalCaptureResponse capture = payPalOrdersClient.captureOrder(paypalOrderId);

        if ("COMPLETED".equalsIgnoreCase(capture.status())) {
            // Validate the captured amount matches our stored amount (partial-capture guard).
            BigDecimal capturedAmt = capture.capturedAmount();
            if (capturedAmt != null && payment.getAmount() != null) {
                BigDecimal tolerance = new BigDecimal("0.02");
                if (payment.getAmount().subtract(capturedAmt).abs().compareTo(tolerance) > 0) {
                    log.warn("[PayPal] Amount mismatch for order {}: expected={} captured={}",
                            paypalOrderId, payment.getAmount(), capturedAmt);
                    throw new BusinessRuleException(
                            "PayPal captured amount (" + capturedAmt + ") does not match expected (" +
                            payment.getAmount() + ")");
                }
            }
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setWebhookEventId(capture.captureId());
            payment.setUpdatedAt(LocalDateTime.now());

            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatus.CANCELLED
                    || order.getStatus() == OrderStatus.EXPIRED) {
                log.warn("[PayPal] Capture COMPLETED for already-{} order #{} — skipping confirmation",
                        order.getStatus(), order.getId());
            } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT
                    || order.getStatus() == OrderStatus.NEW) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("[PayPal] Order #{} confirmed via capture {}", order.getId(), paypalOrderId);
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage("PayPal capture status: " + capture.status());
            payment.setUpdatedAt(LocalDateTime.now());
            log.warn("[PayPal] Capture returned non-COMPLETED status={} for order {}",
                    capture.status(), paypalOrderId);

            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                orderExpiryService.expire(order);
            }
        }

        return toResponse(paymentRepository.save(payment));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Webhook handling
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void handleWebhook(String rawBody, Map<String, String> headers) {
        if (!webhookVerifier.verify(rawBody, headers)) {
            throw new BusinessRuleException("Invalid PayPal webhook signature");
        }

        PayPalWebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, PayPalWebhookEvent.class);
        } catch (Exception e) {
            throw new BusinessRuleException("Failed to parse PayPal webhook body: " + e.getMessage());
        }

        String eventId   = event.id();
        String eventType = event.eventType();

        if (eventId == null || eventId.isBlank()) {
            log.warn("[PayPal] Webhook event has no ID, ignoring");
            return;
        }

        // Replay protection
        if (processedEventRepository.existsByProviderAndEventId(PaymentProvider.PAYPAL, eventId)) {
            log.info("[PayPal] Webhook replay ignored: eventId={} eventType={}", eventId, eventType);
            return;
        }

        log.info("[PayPal] Processing webhook eventId={} eventType={}", eventId, eventType);

        switch (eventType) {
            case EVENT_CAPTURE_COMPLETED -> handleCaptureCompleted(event, eventId);
            case EVENT_CAPTURE_DENIED    -> handleCaptureDenied(event, eventId);
            case EVENT_CAPTURE_REFUNDED  -> handleCaptureRefunded(event, eventId);
            default -> log.info("[PayPal] Ignored unhandled event type: {}", eventType);
        }
    }

    private void handleCaptureCompleted(PayPalWebhookEvent event, String eventId) {
        String orderId = extractOrderId(event);
        if (orderId == null) return;

        paymentRepository.findByProviderPaymentId(orderId).ifPresentOrElse(payment -> {
            if (payment.getProvider() != PaymentProvider.PAYPAL) return;
            if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                recordEvent(eventId, payment);
                return;
            }
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setWebhookEventId(eventId);
            payment.setLastWebhookPayload(event.resource() != null ? event.resource().toString() : null);
            payment.setUpdatedAt(LocalDateTime.now());

            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatus.CANCELLED
                    || order.getStatus() == OrderStatus.EXPIRED) {
                log.warn("[PayPal] Webhook CAPTURE.COMPLETED for already-{} order #{} — skipping confirmation",
                        order.getStatus(), order.getId());
            } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT
                    || order.getStatus() == OrderStatus.NEW) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("[PayPal] Order #{} confirmed via webhook event={}", order.getId(), eventId);
            }
            Payment saved = paymentRepository.save(payment);
            recordEvent(eventId, saved);
        }, () -> log.warn("[PayPal] CAPTURE.COMPLETED: no payment found for orderId={}", orderId));
    }

    private void handleCaptureDenied(PayPalWebhookEvent event, String eventId) {
        String orderId = extractOrderId(event);
        if (orderId == null) return;

        paymentRepository.findByProviderPaymentId(orderId).ifPresentOrElse(payment -> {
            if (payment.getProvider() != PaymentProvider.PAYPAL) return;
            payment.setStatus(PaymentStatus.FAILED);
            payment.setWebhookEventId(eventId);
            payment.setLastWebhookPayload(event.resource() != null ? event.resource().toString() : null);
            payment.setErrorMessage("PayPal capture denied");
            payment.setUpdatedAt(LocalDateTime.now());
            Payment saved = paymentRepository.save(payment);
            if (saved.getOrder().getStatus() == OrderStatus.PENDING_PAYMENT) {
                orderExpiryService.expire(saved.getOrder());
            }
            recordEvent(eventId, saved);
        }, () -> log.warn("[PayPal] CAPTURE.DENIED: no payment found for orderId={}", orderId));
    }

    private void handleCaptureRefunded(PayPalWebhookEvent event, String eventId) {
        String orderId = extractOrderId(event);
        if (orderId == null) return;

        paymentRepository.findByProviderPaymentId(orderId).ifPresentOrElse(payment -> {
            if (payment.getProvider() != PaymentProvider.PAYPAL) return;
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setWebhookEventId(eventId);
            payment.setLastWebhookPayload(event.resource() != null ? event.resource().toString() : null);
            payment.setUpdatedAt(LocalDateTime.now());
            Payment saved = paymentRepository.save(payment);
            log.info("[PayPal] Payment for order #{} refunded", saved.getOrder().getId());
            recordEvent(eventId, saved);
        }, () -> log.warn("[PayPal] CAPTURE.REFUNDED: no payment found for orderId={}", orderId));
    }

    private void recordEvent(String eventId, Payment payment) {
        ProcessedWebhookEvent pwe = new ProcessedWebhookEvent();
        pwe.setProvider(PaymentProvider.PAYPAL);
        pwe.setEventId(eventId);
        pwe.setPayment(payment);
        pwe.setProcessedAt(LocalDateTime.now());
        processedEventRepository.save(pwe);
    }

    /**
     * Extracts the PayPal order ID from a CAPTURE webhook resource.
     *
     * We store providerPaymentId = PayPal ORDER id (not capture id).
     * PayPal CAPTURE events have:
     *   resource.id                                           → capture ID  (wrong for lookup)
     *   resource.supplementary_data.related_ids.order_id     → order ID    (primary)
     *   resource.links[rel=up].href → .../checkout/orders/{ORDER_ID}       (fallback)
     */
    private static String extractOrderId(PayPalWebhookEvent event) {
        if (event.resource() == null) return null;

        // Primary: supplementary_data block present in most CAPTURE events
        var supplementary = event.resource()
                .path("supplementary_data").path("related_ids").path("order_id");
        if (!supplementary.isMissingNode() && !supplementary.isNull()) {
            String orderId = supplementary.asText();
            if (!orderId.isBlank()) return orderId;
        }

        // Fallback: extract order ID from the "up" link that points to the parent order.
        // Link format: https://api.paypal.com/v2/checkout/orders/{ORDER_ID}
        var links = event.resource().path("links");
        if (links.isArray()) {
            for (var link : links) {
                if ("up".equals(link.path("rel").asText())) {
                    String href = link.path("href").asText("");
                    if (href.contains("/checkout/orders/")) {
                        String[] parts = href.split("/");
                        if (parts.length > 0) return parts[parts.length - 1];
                    }
                }
            }
        }

        log.warn("[PayPal] extractOrderId: could not find order_id in webhook resource; event type={}",
                event.eventType());
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static PaymentResponse toResponse(Payment payment) {
        BigDecimal amount = payment.getAmount() == null
                ? BigDecimal.ZERO
                : payment.getAmount().setScale(2, RoundingMode.HALF_UP);
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder() != null ? payment.getOrder().getId() : null,
                payment.getProvider(),
                payment.getStatus(),
                amount,
                payment.getCurrency(),
                payment.getProviderPaymentId(),
                payment.getPaymentUrl(),
                payment.getWebhookEventId(),
                payment.getErrorMessage(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                null
        );
    }

    private PaymentResponse toResponseWithCancelToken(Payment payment) {
        PaymentResponse base = toResponse(payment);
        String token = (payment.getProviderPaymentId() != null && !jwtSecret.isBlank())
                ? generateCancelToken(payment.getProviderPaymentId())
                : null;
        return new PaymentResponse(
                base.id(), base.orderId(), base.provider(), base.status(),
                base.amount(), base.currency(), base.providerPaymentId(),
                base.paymentUrl(), base.webhookEventId(), base.errorMessage(),
                base.createdAt(), base.updatedAt(),
                token
        );
    }

    private String generateCancelToken(String paypalOrderId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(
                    (paypalOrderId + "|cancel").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Cannot generate PayPal cancel token", e);
        }
    }
}
