package com.nurba.java.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
import com.nurba.java.domain.ProcessedWebhookEvent;
import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.request.PaymentWebhookRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.repositories.ProcessedWebhookEventRepository;
import com.nurba.java.service.PaymentService;
import com.nurba.java.service.Impl.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    /** Maximum tolerated difference between webhook amount and stored amount (0.01 KZT). */
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final OrderExpiryService orderExpiryService;

    // ─────────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse initPayment(PaymentInitRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Пустой запрос инициализации оплаты");
        }

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + request.orderId()));

        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.EXPIRED) {
            throw new BusinessRuleException("Для этого заказа нельзя инициализировать оплату");
        }
        if (order.getTotalPrice() == null || order.getTotalPrice().signum() <= 0) {
            throw new BusinessRuleException("Некорректная сумма заказа для оплаты");
        }

        // Idempotency: return an existing PENDING payment for the same order+provider
        // instead of creating a duplicate. This makes the call safe to retry.
        return paymentRepository
                .findByOrderAndProviderAndStatus(order, request.provider(), PaymentStatus.PENDING)
                .map(PaymentServiceImpl::toResponse)
                .orElseGet(() -> toResponse(createNewPayment(order, request)));
    }

    private Payment createNewPayment(Order order, PaymentInitRequest request) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(request.provider());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(order.getTotalPrice().setScale(2, RoundingMode.HALF_UP));
        payment.setCurrency("KZT");
        payment.setProviderPaymentId("stub-" + request.provider().name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        saved.setPaymentUrl(buildStubPaymentUrl(saved, request.returnUrl()));
        saved.setUpdatedAt(LocalDateTime.now());
        return paymentRepository.save(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Webhook
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse handleWebhook(PaymentProvider provider, PaymentWebhookRequest request) {
        Payment payment = resolvePayment(request);
        if (payment.getProvider() != provider) {
            throw new BusinessRuleException("Webhook провайдера не совпадает с провайдером платежа");
        }

        String eventId = (request.eventId() != null) ? request.eventId().trim() : null;

        // Replay protection: if we already processed this event, return the current state unchanged.
        if (eventId != null && !eventId.isBlank()) {
            if (processedEventRepository.existsByProviderAndEventId(provider, eventId)) {
                return toResponse(payment);
            }
        }

        // Amount validation: if the webhook reports an amount, it must match what we expect.
        // Prevents attackers from claiming a $1 payment succeeded for a $1000 order.
        if (request.amount() != null) {
            validateAmount(payment, request.amount());
        }

        if (eventId != null && !eventId.isBlank()) {
            payment.setWebhookEventId(eventId);
        }
        payment.setLastWebhookPayload(serializePayload(request));

        PaymentStatus next = mapWebhookStatus(provider, request.status());
        payment.setStatus(next);
        payment.setUpdatedAt(LocalDateTime.now());

        if (next == PaymentStatus.SUCCEEDED) {
            Order order = payment.getOrder();
            // Idempotent: if already CONFIRMED, skip.
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.NEW) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            }
        }

        Payment saved = paymentRepository.save(payment);

        // Failed/cancelled payment: expire the order immediately so inventory is released at once
        // rather than waiting up to 60 minutes for the scheduled expiry job.
        if ((next == PaymentStatus.FAILED || next == PaymentStatus.CANCELLED)
                && saved.getOrder().getStatus() == OrderStatus.PENDING_PAYMENT) {
            orderExpiryService.expire(saved.getOrder());
        }

        // Record the processed event to prevent future replays.
        if (eventId != null && !eventId.isBlank()) {
            ProcessedWebhookEvent pwe = new ProcessedWebhookEvent();
            pwe.setProvider(provider);
            pwe.setEventId(eventId);
            pwe.setPayment(saved);
            pwe.setProcessedAt(LocalDateTime.now());
            processedEventRepository.save(pwe);
        }

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateAmount(Payment payment, BigDecimal webhookAmount) {
        BigDecimal stored   = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal received = webhookAmount.setScale(2, RoundingMode.HALF_UP);
        if (stored.subtract(received).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessRuleException(
                    "Сумма в webhook (" + received + ") не совпадает с суммой платежа (" + stored + ")");
        }
    }

    private Payment resolvePayment(PaymentWebhookRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Пустой webhook payload");
        }
        if (request.paymentId() != null) {
            return paymentRepository.findById(request.paymentId())
                    .orElseThrow(() -> new NotFoundException("Платёж не найден: " + request.paymentId()));
        }
        if (request.providerPaymentId() != null && !request.providerPaymentId().isBlank()) {
            return paymentRepository.findByProviderPaymentId(request.providerPaymentId().trim())
                    .orElseThrow(() -> new NotFoundException("Платёж не найден по providerPaymentId"));
        }
        throw new BusinessRuleException("Webhook должен содержать paymentId или providerPaymentId");
    }

    private static PaymentStatus mapWebhookStatus(PaymentProvider provider, String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return PaymentStatus.PENDING;
        }
        String s = rawStatus.trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case KASPI    -> mapKaspiStatus(s);
            case YOOKASSA -> mapYookassaStatus(s);
            case PAYPAL   -> mapPaypalStatus(s);
        };
    }

    private static PaymentStatus mapKaspiStatus(String status) {
        return switch (status) {
            case "succeeded", "success", "paid", "approved" -> PaymentStatus.SUCCEEDED;
            case "cancelled", "canceled", "declined"        -> PaymentStatus.CANCELLED;
            case "refunded"                                  -> PaymentStatus.REFUNDED;
            case "failed", "error"                           -> PaymentStatus.FAILED;
            default                                          -> PaymentStatus.PENDING;
        };
    }

    private static PaymentStatus mapYookassaStatus(String status) {
        return switch (status) {
            case "succeeded", "payment.succeeded"          -> PaymentStatus.SUCCEEDED;
            case "canceled", "cancelled", "payment.canceled" -> PaymentStatus.CANCELLED;
            case "refund.succeeded", "refunded"            -> PaymentStatus.REFUNDED;
            case "failed", "error"                         -> PaymentStatus.FAILED;
            default                                        -> PaymentStatus.PENDING;
        };
    }

    private static PaymentStatus mapPaypalStatus(String status) {
        return switch (status) {
            case "completed", "captured", "succeeded"      -> PaymentStatus.SUCCEEDED;
            case "voided", "denied", "cancelled", "canceled" -> PaymentStatus.CANCELLED;
            case "refunded"                                -> PaymentStatus.REFUNDED;
            case "failed", "error"                         -> PaymentStatus.FAILED;
            default                                        -> PaymentStatus.PENDING;
        };
    }

    private String serializePayload(PaymentWebhookRequest request) {
        try {
            return request == null ? null : objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return "{\"serializationError\":\"" + e.getMessage() + "\"}";
        }
    }

    private static String buildStubPaymentUrl(Payment payment, String returnUrl) {
        String fallback = "https://example.com/payments/" + payment.getProvider().name().toLowerCase(Locale.ROOT)
                + "/stub?orderId=" + payment.getOrder().getId();
        if (returnUrl == null || returnUrl.isBlank()) {
            return fallback;
        }
        String separator = returnUrl.contains("?") ? "&" : "?";
        return returnUrl.trim()
                + separator + "orderId=" + payment.getOrder().getId()
                + "&paymentId=" + payment.getId()
                + "&provider=" + encode(payment.getProvider().name())
                + "&status=" + encode(payment.getStatus().name())
                + "&providerPaymentId=" + encode(payment.getProviderPaymentId());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

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
                payment.getUpdatedAt()
        );
    }
}
