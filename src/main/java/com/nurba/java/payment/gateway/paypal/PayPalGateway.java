package com.nurba.java.payment.gateway.paypal;

import com.nurba.java.domain.Order;
import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.payment.PayPalOrdersClient;
import com.nurba.java.payment.dto.PayPalCaptureResponse;
import com.nurba.java.payment.dto.PayPalCreateOrderResponse;
import com.nurba.java.payment.gateway.GatewayCallbackResult;
import com.nurba.java.payment.gateway.GatewayInitResult;
import com.nurba.java.payment.gateway.PaymentGateway;
import com.nurba.java.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * PayPal gateway — wraps PayPalOrdersClient directly to avoid double Payment entity creation.
 * PayPalService.createOrder() / captureOrder() also create DB entities, so this gateway
 * bypasses PayPalService and calls PayPalOrdersClient directly.
 *
 * Webhook events use a different path (RSA + raw body) and remain in PayPalWebhookController.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalGateway implements PaymentGateway {

    private final PayPalOrdersClient payPalOrdersClient;
    private final ExchangeRateService exchangeRateService;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public PaymentProvider getProvider() { return PaymentProvider.PAYPAL; }

    @Override
    public GatewayInitResult init(Order order, PaymentInitRequest request) {
        BigDecimal kztPerUsd = exchangeRateService.kztPerUsd();
        BigDecimal amountUsd = order.getTotalPrice()
                .divide(kztPerUsd, 2, RoundingMode.HALF_UP);

        PayPalCreateOrderResponse ppOrder = payPalOrdersClient.createOrder(
                amountUsd, "USD", request.returnUrl(), request.cancelUrl());

        String approvalUrl = ppOrder.approvalUrl();
        if (approvalUrl == null) {
            throw new BusinessRuleException(
                    "PayPal не вернул ссылку на подтверждение оплаты");
        }

        String cancelToken = generateCancelToken(ppOrder.id());

        log.info("[PayPal] Created order {} for order #{}, amountUsd={}",
                ppOrder.id(), order.getId(), amountUsd);

        return new GatewayInitResult(ppOrder.id(), approvalUrl, amountUsd, "USD", cancelToken);
    }

    /**
     * PayPal webhooks carry a raw JSON body + RSA signature headers — incompatible with
     * Map&lt;String,String&gt;. They stay in PayPalWebhookController → PayPalService.handleWebhook().
     */
    @Override
    public GatewayCallbackResult handleCallback(Map<String, String> params) {
        throw new UnsupportedOperationException(
                "PayPal webhooks are handled by the dedicated PayPalWebhookController endpoint");
    }

    @Override
    public GatewayCallbackResult capture(String providerPaymentId) {
        PayPalCaptureResponse capture = payPalOrdersClient.captureOrder(providerPaymentId);

        PaymentStatus status;
        BigDecimal amount = null;

        if ("COMPLETED".equalsIgnoreCase(capture.status())) {
            status = PaymentStatus.SUCCEEDED;
            amount = capture.capturedAmount();
        } else {
            status = PaymentStatus.FAILED;
            log.warn("[PayPal] Capture returned status={} for paypalOrderId={}",
                    capture.status(), providerPaymentId);
        }

        return new GatewayCallbackResult(
                providerPaymentId, status, amount,
                "paypal-capture-status=" + capture.status());
    }

    /** PayPal does not use return-URL verification; the frontend calls capture() instead. */
    @Override
    public GatewayCallbackResult verifyReturn(Map<String, String> params) {
        throw new UnsupportedOperationException(
                "PayPal does not support return-URL verification; use capture() instead");
    }

    /** Cancel token uses HMAC-SHA256(orderId + "|cancel") signed with jwtSecret. */
    @Override
    public String generateCancelToken(String providerPaymentId) {
        if (jwtSecret == null || jwtSecret.isBlank() || providerPaymentId == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(
                    (providerPaymentId + "|cancel").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Cannot generate PayPal cancel token", e);
        }
    }
}
