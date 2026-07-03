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
import com.nurba.java.payment.gateway.GatewayCallbackResult;
import com.nurba.java.payment.gateway.GatewayInitResult;
import com.nurba.java.payment.gateway.PaymentGatewayRegistry;
import com.nurba.java.repositories.OrderRepository;
import com.nurba.java.repositories.PaymentRepository;
import com.nurba.java.repositories.ProcessedWebhookEventRepository;
import com.nurba.java.service.EmailService;
import com.nurba.java.service.PaymentService;
import com.nurba.java.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nurba.java.service.CdekShipmentAutoService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.02");

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedEventRepository;
    private final OrderExpiryService orderExpiryService;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final EmailService emailService;
    private final TelegramNotificationService telegramNotificationService;
    private final CdekShipmentAutoService cdekShipmentAutoService;

    // ─── Init ─────────────────────────────────────────────────────────────────

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

        // Idempotency: return existing PENDING payment for same order + provider
        return paymentRepository
                .findByOrderAndProviderAndStatus(order, request.provider(), PaymentStatus.PENDING)
                .map(existing -> {
                    String cancelToken = gatewayRegistry.get(request.provider())
                            .generateCancelToken(existing.getProviderPaymentId());
                    return toResponseWithToken(existing, cancelToken);
                })
                .orElseGet(() -> {
                    GatewayInitResult initResult = gatewayRegistry.get(request.provider())
                            .init(order, request);
                    Payment saved = paymentRepository.save(buildPayment(order, request.provider(), initResult));
                    return toResponseWithToken(saved, initResult.cancelToken());
                });
    }

    // ─── Callbacks ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse handleCallback(PaymentProvider provider, Map<String, String> params) {
        GatewayCallbackResult result = gatewayRegistry.get(provider).handleCallback(params);
        return applyResult(provider, result, result.eventId());
    }

    @Override
    @Transactional
    public PaymentResponse verifyReturn(PaymentProvider provider, Map<String, String> params) {
        GatewayCallbackResult result = gatewayRegistry.get(provider).verifyReturn(params);
        return applyResult(provider, result, result.eventId());
    }

    @Override
    @Transactional
    public PaymentResponse capturePayment(PaymentProvider provider, String providerPaymentId) {
        GatewayCallbackResult result = gatewayRegistry.get(provider).capture(providerPaymentId);
        return applyResult(provider, result, result.eventId());
    }

    @Override
    @Transactional
    public PaymentResponse applyExternalEvent(PaymentProvider provider, String dedupEventId, GatewayCallbackResult result) {
        return applyResult(provider, result, dedupEventId);
    }

    // ─── Core: unified result application ─────────────────────────────────────

    /**
     * dedupEventId is normally the same as result.eventId() (the provider payment ID),
     * which is how FreedomPay/VTB work — one callback delivery per payment, no separate
     * event identity. PayPal webhooks are the exception: PayPal assigns each logical
     * webhook event its own ID, distinct from the PayPal order ID used to look up the
     * Payment row, so applyExternalEvent() passes that webhook event ID here while
     * result.eventId() still carries the order ID for the lookup.
     */
    private PaymentResponse applyResult(PaymentProvider provider, GatewayCallbackResult result, String dedupEventId) {
        String providerPaymentId = result.eventId();

        // Dedup: event already processed → return existing payment
        if (processedEventRepository.existsByProviderAndEventId(provider, dedupEventId)) {
            log.info("[{}] Event already processed: {}", provider, dedupEventId);
            return paymentRepository.findByProviderPaymentId(providerPaymentId)
                    .map(p -> toResponseWithToken(p, null))
                    .orElseThrow(() -> new NotFoundException(
                            "Payment not found for eventId: " + providerPaymentId));
        }

        // Pessimistic lock: prevents concurrent double-confirms on the same payment
        Payment payment = paymentRepository.findByProviderPaymentIdForUpdate(providerPaymentId)
                .orElseThrow(() -> new NotFoundException(
                        "Payment not found for providerPaymentId: " + providerPaymentId));

        // Idempotency: already in a terminal state
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("[{}] Payment {} already in status={}, skipping",
                    provider, providerPaymentId, payment.getStatus());
            return toResponseWithToken(payment, null);
        }

        // Amount validation (when the provider reports an amount)
        if (result.amount() != null && payment.getAmount() != null) {
            BigDecimal stored   = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal received = result.amount().setScale(2, RoundingMode.HALF_UP);
            if (stored.subtract(received).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
                throw new BusinessRuleException(
                        "Сумма в ответе провайдера (" + received
                        + ") не совпадает с суммой платежа (" + stored + ")");
            }
        }

        // Apply new status
        payment.setStatus(result.status());
        payment.setWebhookEventId(dedupEventId);
        if (result.rawPayload() != null) payment.setLastWebhookPayload(result.rawPayload());
        payment.setUpdatedAt(LocalDateTime.now());

        // Update order status
        Order order = payment.getOrder();
        if (result.status() == PaymentStatus.SUCCEEDED) {
            if (order.getStatus() == OrderStatus.CANCELLED
                    || order.getStatus() == OrderStatus.EXPIRED) {
                log.warn("[{}] Payment SUCCEEDED for already-{} order #{} — skipping confirmation",
                        provider, order.getStatus(), order.getId());
            } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT
                    || order.getStatus() == OrderStatus.NEW) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("[{}] Order #{} confirmed", provider, order.getId());
                if (order.getAppUser() != null) {
                    emailService.sendPaymentSuccessEmail(order.getAppUser().getEmail(), order);
                }
                telegramNotificationService.notifyPaymentSuccess(order);
                final long confirmedOrderId = order.getId();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cdekShipmentAutoService.triggerIfCdek(confirmedOrderId);
                    }
                });
            }
        } else if (result.status() == PaymentStatus.FAILED
                || result.status() == PaymentStatus.CANCELLED) {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                orderExpiryService.expire(order);
                if (order.getAppUser() != null) {
                    emailService.sendPaymentFailedEmail(order.getAppUser().getEmail(), order);
                }
                telegramNotificationService.notifyPaymentFailed(order);
            }
        }

        Payment saved = paymentRepository.save(payment);

        // Record event — UNIQUE(provider, eventId) guards against races
        ProcessedWebhookEvent pwe = new ProcessedWebhookEvent();
        pwe.setProvider(provider);
        pwe.setEventId(dedupEventId);
        pwe.setPayment(saved);
        pwe.setProcessedAt(LocalDateTime.now());
        try {
            processedEventRepository.save(pwe);
        } catch (DataIntegrityViolationException e) {
            // Another thread already recorded this event — that's fine; result is already saved
            log.info("[{}] ProcessedWebhookEvent race on {}: already recorded", provider, dedupEventId);
        }

        return toResponseWithToken(saved, null);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static Payment buildPayment(Order order, PaymentProvider provider, GatewayInitResult result) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(provider);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(result.amount());
        payment.setCurrency(result.currency());
        payment.setProviderPaymentId(result.providerPaymentId());
        payment.setPaymentUrl(result.redirectUrl());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    private static PaymentResponse toResponseWithToken(Payment payment, String cancelToken) {
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
                cancelToken
        );
    }
}
