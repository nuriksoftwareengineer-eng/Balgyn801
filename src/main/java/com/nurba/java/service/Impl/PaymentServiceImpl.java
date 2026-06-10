package com.nurba.java.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
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
import com.nurba.java.service.PaymentService;
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

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentResponse initPayment(PaymentInitRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Пустой запрос инициализации оплаты");
        }

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + request.orderId()));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Для этого заказа нельзя инициализировать оплату");
        }
        if (order.getTotalPrice() == null || order.getTotalPrice().signum() <= 0) {
            throw new BusinessRuleException("Некорректная сумма заказа для оплаты");
        }

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
        return toResponse(paymentRepository.save(saved));
    }

    @Override
    @Transactional
    public PaymentResponse handleWebhook(PaymentProvider provider, PaymentWebhookRequest request) {
        Payment payment = resolvePayment(request);
        if (payment.getProvider() != provider) {
            throw new BusinessRuleException("Webhook провайдера не совпадает с провайдером платежа");
        }

        if (request != null && request.eventId() != null && !request.eventId().isBlank()) {
            payment.setWebhookEventId(request.eventId().trim());
        }
        payment.setLastWebhookPayload(serializePayload(request));

        PaymentStatus next = mapWebhookStatus(provider, request == null ? null : request.status());
        payment.setStatus(next);
        payment.setUpdatedAt(LocalDateTime.now());

        if (next == PaymentStatus.SUCCEEDED) {
            Order order = payment.getOrder();
            // Successful payment makes a pending order visible to admin. NEW is also accepted for
            // manually-created/legacy orders. The transition is idempotent: a duplicate webhook
            // finds the order already CONFIRMED and does nothing.
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.NEW) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            }
            // Inventory was already reserved (deducted under lock) at order creation — no action here.
        }
        return toResponse(paymentRepository.save(payment));
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
            case KASPI -> mapKaspiStatus(s);
            case YOOKASSA -> mapYookassaStatus(s);
            case PAYPAL -> mapPaypalStatus(s);
        };
    }

    private static PaymentStatus mapKaspiStatus(String status) {
        return switch (status) {
            case "succeeded", "success", "paid", "approved" -> PaymentStatus.SUCCEEDED;
            case "cancelled", "canceled", "declined" -> PaymentStatus.CANCELLED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "failed", "error" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private static PaymentStatus mapYookassaStatus(String status) {
        return switch (status) {
            case "succeeded", "payment.succeeded" -> PaymentStatus.SUCCEEDED;
            case "canceled", "cancelled", "payment.canceled" -> PaymentStatus.CANCELLED;
            case "refund.succeeded", "refunded" -> PaymentStatus.REFUNDED;
            case "failed", "error" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private static PaymentStatus mapPaypalStatus(String status) {
        return switch (status) {
            case "completed", "captured", "succeeded" -> PaymentStatus.SUCCEEDED;
            case "voided", "denied", "cancelled", "canceled" -> PaymentStatus.CANCELLED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "failed", "error" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
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
