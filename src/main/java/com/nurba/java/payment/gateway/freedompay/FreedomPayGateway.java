package com.nurba.java.payment.gateway.freedompay;

import com.nurba.java.config.FreedomPayProperties;
import com.nurba.java.domain.Order;
import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.payment.FreedomPayHttpClient;
import com.nurba.java.payment.FreedomPayInitResult;
import com.nurba.java.payment.FreedomPaySignature;
import com.nurba.java.payment.PaymentProviderException;
import com.nurba.java.payment.gateway.GatewayCallbackResult;
import com.nurba.java.payment.gateway.GatewayInitResult;
import com.nurba.java.payment.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreedomPayGateway implements PaymentGateway {

    private final FreedomPayHttpClient freedomPayClient;
    private final FreedomPayProperties props;

    @Override
    public PaymentProvider getProvider() { return PaymentProvider.FREEDOM_PAY; }

    @Override
    public GatewayInitResult init(Order order, PaymentInitRequest request) {
        BigDecimal amount = order.getTotalPrice().setScale(2, RoundingMode.HALF_UP);
        FreedomPayInitResult result = freedomPayClient.initPayment(
                order.getId(), amount, "Order #" + order.getId());

        if (!result.success()) {
            throw new PaymentProviderException(
                    "Freedom Pay init_payment.php failed for order #" + order.getId() + ": "
                    + (result.errorDescription() != null ? result.errorDescription() : "unknown error"));
        }

        return new GatewayInitResult(
                result.providerPaymentId(),
                result.redirectUrl(),
                amount,
                "KZT",
                null);
    }

    /**
     * Processes a FreedomPay server callback.
     * The MD5 signature is verified by FreedomPayCallbackController before this is called.
     */
    @Override
    public GatewayCallbackResult handleCallback(Map<String, String> params) {
        String pgPaymentId = params.get("pg_payment_id");
        if (pgPaymentId == null || pgPaymentId.isBlank()) {
            throw new BusinessRuleException("Отсутствует pg_payment_id в callback Freedom Pay");
        }

        String pgResult = params.get("pg_result");
        PaymentStatus status = mapResult(pgResult);

        BigDecimal amount = null;
        String pgAmount = params.get("pg_amount");
        if (pgAmount != null && !pgAmount.isBlank()) {
            try {
                amount = new BigDecimal(pgAmount.trim());
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("Некорректный pg_amount в callback: " + pgAmount);
            }
        }

        log.info("[FreedomPay] handleCallback: pg_payment_id={} pg_result={} → {}",
                pgPaymentId, pgResult, status);

        return new GatewayCallbackResult(pgPaymentId.trim(), status, amount, params.toString());
    }

    /**
     * Verifies the FreedomPay browser success-redirect signature and maps result.
     * FreedomPayCheckController does NOT verify the signature, so this method must.
     */
    @Override
    public GatewayCallbackResult verifyReturn(Map<String, String> params) {
        String pgPaymentId = params.get("pg_payment_id");
        String pgOrderId   = params.get("pg_order_id");
        String pgSig       = params.get("pg_sig");
        String pgResult    = params.get("pg_result");
        String pgAmount    = params.get("pg_amount");
        String scriptName  = props.getSuccessScriptName();

        log.info("[FreedomPay] verifyReturn: pg_order_id={} pg_payment_id={} pg_result={} scriptName={}",
                pgOrderId, pgPaymentId, pgResult, scriptName);

        if (pgPaymentId == null || pgPaymentId.isBlank()) {
            throw new BusinessRuleException("Отсутствует pg_payment_id в redirect-параметрах");
        }

        if (!props.getSecretKey().isBlank()) {
            boolean sigValid = FreedomPaySignature.verify(
                    scriptName, params, props.getSecretKey(), pgSig);
            if (!sigValid) {
                String expected = FreedomPaySignature.sign(scriptName, params, props.getSecretKey());
                log.warn("[FreedomPay] INVALID pg_sig in redirect! expected={} received={}",
                        expected, pgSig);
                throw new BusinessRuleException(
                        "Неверная подпись FreedomPay в redirect (scriptName=" + scriptName + ")");
            }
            log.info("[FreedomPay] pg_sig VALID: pg_order_id={} pg_payment_id={}", pgOrderId, pgPaymentId);
        } else {
            log.warn("[FreedomPay] secretKey not configured — skipping sig verification in redirect");
        }

        // Valid signature on pg_success_url is authoritative proof of success.
        // If pg_result is absent or not "0", treat as SUCCEEDED.
        PaymentStatus status = "0".equals(pgResult) ? PaymentStatus.FAILED : PaymentStatus.SUCCEEDED;

        BigDecimal amount = null;
        if (pgAmount != null && !pgAmount.isBlank()) {
            try { amount = new BigDecimal(pgAmount.trim()); } catch (NumberFormatException ignored) {}
        }

        return new GatewayCallbackResult(pgPaymentId.trim(), status, amount, params.toString());
    }

    private static PaymentStatus mapResult(String pgResult) {
        if (pgResult == null) return PaymentStatus.PENDING;
        return switch (pgResult.trim()) {
            case "1"  -> PaymentStatus.SUCCEEDED;
            case "0"  -> PaymentStatus.FAILED;
            default   -> PaymentStatus.PENDING;
        };
    }
}
