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
import com.nurba.java.config.FreedomPayProperties;
import com.nurba.java.payment.FreedomPayHttpClient;
import com.nurba.java.payment.FreedomPayInitResult;
import com.nurba.java.payment.FreedomPaySignature;
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
    private final FreedomPayProperties freedomPayProps;

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
        log.info("[FreedomPay] handleFreedomPayCallback: pg_payment_id={} pg_result={} pg_amount={} pg_order_id={}",
                providerPaymentId, params.get("pg_result"), params.get("pg_amount"), params.get("pg_order_id"));
        if (providerPaymentId == null || providerPaymentId.isBlank()) {
            throw new BusinessRuleException("Отсутствует pg_payment_id в callback Freedom Pay");
        }

        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId.trim())
                .orElseThrow(() -> new NotFoundException(
                        "Платёж не найден по pg_payment_id: " + providerPaymentId));
        log.info("[FreedomPay] handleFreedomPayCallback: found payment id={} status={} orderId={}",
                payment.getId(), payment.getStatus(), payment.getOrder().getId());

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
    // Freedom Pay success-redirect verification (replaces check_payment.php)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the FreedomPay success-redirect signature and confirms the order.
     *
     * <p>FreedomPay signs the browser redirect to {@code pg_success_url} with:
     * <pre>
     *   MD5( lastSegment(pg_success_url) ; sorted_param_values ; secretKey )
     * </pre>
     * Example: successUrl = ".../payment-return" → scriptName = "payment-return"
     * The signed params include pg_order_id, pg_payment_id, pg_amount, pg_currency,
     * pg_result (1), pg_salt but NOT pg_sig itself.
     *
     * <p>Security: a forged URL with wrong pg_order_id, wrong pg_amount, or missing secretKey
     * will produce a mismatched MD5 and be rejected with 400. Replayed valid URLs are blocked
     * by ProcessedWebhookEvent (pg_payment_id deduplication).
     */
    @Override
    @Transactional
    public PaymentResponse verifyFreedomPayRedirect(Map<String, String> redirectParams) {
        String pgPaymentId = redirectParams.get("pg_payment_id");
        String pgOrderId   = redirectParams.get("pg_order_id");
        String pgSig       = redirectParams.get("pg_sig");
        String pgResult    = redirectParams.get("pg_result");
        String pgAmount    = redirectParams.get("pg_amount");
        String scriptName  = freedomPayProps.getSuccessScriptName();

        log.info("[FreedomPay] verifyFreedomPayRedirect: pg_order_id={} pg_payment_id={} " +
                 "pg_result={} pg_amount={} scriptName={} params={}",
                 pgOrderId, pgPaymentId, pgResult, pgAmount, scriptName, redirectParams.keySet());

        // Log every pg_* field for diagnosis
        redirectParams.forEach((k, v) -> {
            if (k.startsWith("pg_")) log.info("[FreedomPay] redirect param: {}={}", k, v);
        });

        if (pgPaymentId == null || pgPaymentId.isBlank()) {
            throw new BusinessRuleException("Отсутствует pg_payment_id в redirect-параметрах");
        }

        // ── Signature verification ─────────────────────────────────────────
        // Exact formula: MD5(scriptName ; val_sorted_by_key... ; secretKey)
        // All params except pg_sig are included (sorted by key, TreeMap order).
        // A tampered pg_order_id, pg_amount etc. changes the expected hash → rejected.
        if (freedomPayProps.getSecretKey().isBlank()) {
            log.warn("[FreedomPay] secretKey not configured — skipping sig verification in redirect");
        } else {
            String expected = FreedomPaySignature.sign(scriptName, redirectParams,
                    freedomPayProps.getSecretKey());
            boolean sigValid = FreedomPaySignature.verify(scriptName, redirectParams,
                    freedomPayProps.getSecretKey(), pgSig);
            if (!sigValid) {
                log.warn("[FreedomPay] INVALID pg_sig in success redirect! " +
                         "pg_order_id={} pg_payment_id={} scriptName={} " +
                         "received_sig={} expected_sig={}",
                         pgOrderId, pgPaymentId, scriptName, pgSig, expected);
                throw new BusinessRuleException(
                        "Неверная подпись FreedomPay в redirect (scriptName=" + scriptName
                        + "). Возможна подделка URL или неверный secretKey.");
            }
            log.info("[FreedomPay] pg_sig VALID: pg_order_id={} pg_payment_id={} scriptName={}",
                    pgOrderId, pgPaymentId, scriptName);
        }

        // ── Confirmed payment — delegate to standard callback handler ──────
        // pg_result may be absent in some FP redirect configurations.
        // Since a valid signature on pg_success_url is authoritative proof of success,
        // we inject pg_result=1 so handleFreedomPayCallback maps it to SUCCEEDED.
        java.util.HashMap<String, String> enriched = new java.util.HashMap<>(redirectParams);
        if (!"0".equals(pgResult)) {
            enriched.put("pg_result", "1");
        }
        return handleFreedomPayCallback(enriched);
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
