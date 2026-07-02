package com.nurba.java.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.payment.PayPalWebhookVerifier;
import com.nurba.java.payment.dto.PayPalWebhookEvent;
import com.nurba.java.payment.gateway.GatewayCallbackResult;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.service.PayPalService;
import com.nurba.java.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Handles PayPal webhooks and buyer-initiated cancel — the two flows that don't fit the
 * generic PaymentGateway contract (see PayPalService javadoc). Order creation / capture
 * live in PayPalGateway and flow through PaymentServiceImpl, same as FreedomPay/VTB.
 *
 * Webhook events are parsed and signature-verified here (raw body + RSA, PayPal-specific),
 * then mapped to a GatewayCallbackResult and handed to PaymentService.applyExternalEvent() —
 * the same chokepoint FreedomPay/VTB use — so Payment/Order mutation and email/Telegram
 * notifications are not duplicated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalServiceImpl implements PayPalService {

    private static final String EVENT_CAPTURE_COMPLETED = "PAYMENT.CAPTURE.COMPLETED";
    private static final String EVENT_CAPTURE_DENIED    = "PAYMENT.CAPTURE.DENIED";
    private static final String EVENT_CAPTURE_REFUNDED  = "PAYMENT.CAPTURE.REFUNDED";

    private final PaymentRepository paymentRepository;
    private final OrderExpiryService orderExpiryService;
    private final PayPalWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @Value("${jwt.secret:}")
    private String jwtSecret;

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

        log.info("[PayPal] Processing webhook eventId={} eventType={}", eventId, eventType);

        PaymentStatus status = switch (eventType) {
            case EVENT_CAPTURE_COMPLETED -> PaymentStatus.SUCCEEDED;
            case EVENT_CAPTURE_DENIED    -> PaymentStatus.FAILED;
            case EVENT_CAPTURE_REFUNDED  -> PaymentStatus.REFUNDED;
            default -> null;
        };
        if (status == null) {
            log.info("[PayPal] Ignored unhandled event type: {}", eventType);
            return;
        }

        String orderId = extractOrderId(event);
        if (orderId == null) {
            log.warn("[PayPal] {}: could not find order_id in webhook resource, ignoring eventId={}",
                    eventType, eventId);
            return;
        }

        // Webhook events never re-validate amount (provider-reported amount is trusted
        // from the original capture, not re-derived here) — same as the pre-refactor behaviour.
        String rawPayload = event.resource() != null ? event.resource().toString() : null;
        GatewayCallbackResult result = new GatewayCallbackResult(orderId, status, null, rawPayload);
        paymentService.applyExternalEvent(PaymentProvider.PAYPAL, eventId, result);
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
        java.math.BigDecimal amount = payment.getAmount() == null
                ? java.math.BigDecimal.ZERO
                : payment.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);
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
