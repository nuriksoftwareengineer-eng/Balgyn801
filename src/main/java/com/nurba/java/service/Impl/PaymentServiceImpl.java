package com.nurba.java.service.Impl;

import com.nurba.java.domain.Order;
import com.nurba.java.domain.Payment;
import com.nurba.java.domain.ProcessedWebhookEvent;
import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.OrderStatus;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.payment.FreedomPayHttpClient;
import com.nurba.java.payment.FreedomPayInitResult;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.repositories.ProcessedWebhookEventRepository;
import com.nurba.java.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedEventRepository;
    private final OrderExpiryService orderExpiryService;
    private final FreedomPayHttpClient freedomPayClient;

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

        // Idempotency: return existing PENDING payment for the same order+provider
        return paymentRepository
                .findByOrderAndProviderAndStatus(order, PaymentProvider.FREEDOM_PAY, PaymentStatus.PENDING)
                .map(PaymentServiceImpl::toResponse)
                .orElseGet(() -> toResponse(createNewPayment(order, request)));
    }

    private Payment createNewPayment(Order order, PaymentInitRequest request) {
        BigDecimal amount = order.getTotalPrice().setScale(2, RoundingMode.HALF_UP);
        String description = "Order #" + order.getId();

        FreedomPayInitResult result = freedomPayClient.initPayment(order.getId(), amount, description);

        if (!result.success()) {
            throw new BusinessRuleException(
                    "Freedom Pay отказал в инициализации оплаты: "
                    + (result.errorDescription() != null ? result.errorDescription() : "unknown error"));
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.FREEDOM_PAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setCurrency("KZT");
        payment.setProviderPaymentId(result.providerPaymentId());
        payment.setPaymentUrl(result.redirectUrl());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Freedom Pay callback
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse handleFreedomPayCallback(Map<String, String> params) {
        String providerPaymentId = params.get("pg_payment_id");
        if (providerPaymentId == null || providerPaymentId.isBlank()) {
            throw new BusinessRuleException("Отсутствует pg_payment_id в callback Freedom Pay");
        }

        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId.trim())
                .orElseThrow(() -> new NotFoundException(
                        "Платёж не найден по pg_payment_id: " + providerPaymentId));

        if (payment.getProvider() != PaymentProvider.FREEDOM_PAY) {
            throw new BusinessRuleException("Callback Freedom Pay не совпадает с провайдером платежа");
        }

        // Replay protection: pg_payment_id is unique per payment — process only once.
        if (processedEventRepository.existsByProviderAndEventId(
                PaymentProvider.FREEDOM_PAY, providerPaymentId.trim())) {
            log.info("[FreedomPay] Callback replay ignored: pg_payment_id={}", providerPaymentId);
            return toResponse(payment);
        }

        // Amount validation
        String pgAmount = params.get("pg_amount");
        if (pgAmount != null && !pgAmount.isBlank()) {
            try {
                validateAmount(payment, new BigDecimal(pgAmount.trim()));
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("Некорректный pg_amount в callback: " + pgAmount);
            }
        }

        // Map pg_result to our PaymentStatus
        String pgResult = params.get("pg_result");
        PaymentStatus next = mapFreedomPayResult(pgResult);

        payment.setStatus(next);
        payment.setLastWebhookPayload(params.toString());
        payment.setUpdatedAt(LocalDateTime.now());

        if (next == PaymentStatus.SUCCEEDED) {
            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatus.CANCELLED
                    || order.getStatus() == OrderStatus.EXPIRED) {
                log.warn("[FreedomPay] Payment SUCCEEDED for already-{} order #{} — skipping confirmation",
                        order.getStatus(), order.getId());
            } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT
                    || order.getStatus() == OrderStatus.NEW) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("[FreedomPay] Order #{} confirmed via payment pg_payment_id={}",
                        order.getId(), providerPaymentId);
            }
        }

        Payment saved = paymentRepository.save(payment);

        if ((next == PaymentStatus.FAILED || next == PaymentStatus.CANCELLED)
                && saved.getOrder().getStatus() == OrderStatus.PENDING_PAYMENT) {
            orderExpiryService.expire(saved.getOrder());
        }

        // Record event to block replays
        ProcessedWebhookEvent pwe = new ProcessedWebhookEvent();
        pwe.setProvider(PaymentProvider.FREEDOM_PAY);
        pwe.setEventId(providerPaymentId.trim());
        pwe.setPayment(saved);
        pwe.setProcessedAt(LocalDateTime.now());
        processedEventRepository.save(pwe);

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static PaymentStatus mapFreedomPayResult(String pgResult) {
        if (pgResult == null) return PaymentStatus.PENDING;
        return switch (pgResult.trim()) {
            case "1"  -> PaymentStatus.SUCCEEDED;
            case "0"  -> PaymentStatus.FAILED;
            default   -> PaymentStatus.PENDING;   // "2" = in progress / pending
        };
    }

    private void validateAmount(Payment payment, BigDecimal callbackAmount) {
        BigDecimal stored   = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal received = callbackAmount.setScale(2, RoundingMode.HALF_UP);
        if (stored.subtract(received).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessRuleException(
                    "Сумма в callback (" + received + ") не совпадает с суммой платежа (" + stored + ")");
        }
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
                payment.getUpdatedAt(),
                null
        );
    }
}
