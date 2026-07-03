package com.nurba.java.payment.gateway.vtb;

import com.nurba.java.config.VtbProperties;
import com.nurba.java.domain.Order;
import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.payment.gateway.GatewayCallbackResult;
import com.nurba.java.payment.gateway.GatewayInitResult;
import com.nurba.java.payment.gateway.PaymentGateway;
import com.nurba.java.payment.vtb.VtbCallbackVerifier;
import com.nurba.java.payment.vtb.VtbChecksumResult;
import com.nurba.java.payment.vtb.VtbCurrencyMapper;
import com.nurba.java.payment.vtb.VtbHttpClient;
import com.nurba.java.payment.vtb.VtbOrderStatus;
import com.nurba.java.payment.vtb.dto.VtbOrderStatusResponse;
import com.nurba.java.payment.vtb.dto.VtbRegisterRequest;
import com.nurba.java.payment.vtb.dto.VtbRegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VtbGateway implements PaymentGateway {

    private final VtbHttpClient vtbHttpClient;
    private final VtbCallbackVerifier checksumVerifier;
    private final VtbCurrencyMapper currencyMapper;
    private final VtbProperties props;

    @Override
    public PaymentProvider getProvider() { return PaymentProvider.VTB_KZ; }

    @Override
    public GatewayInitResult init(Order order, PaymentInitRequest request) {
        // VTB KZ is a KZT-primary provider; use order total as-is in KZT
        int isoNumeric = currencyMapper.mapCurrency("KZT", props);
        BigDecimal amount = order.getTotalPrice().setScale(2, RoundingMode.HALF_UP);
        long amountMinorUnits = VtbCurrencyMapper.toMinorUnits(amount);

        // orderNumber must be unique per transaction — suffix with timestamp to survive retries
        String orderNumber = order.getId() + "_" + System.currentTimeMillis();

        String returnUrl = (request.returnUrl() != null && !request.returnUrl().isBlank())
                ? request.returnUrl()
                : props.getReturnUrl();

        VtbRegisterRequest registerRequest = VtbRegisterRequest.builder()
                .orderNumber(orderNumber)
                .amount(amountMinorUnits)
                .currency(isoNumeric)
                .returnUrl(returnUrl)
                .description("Order #" + order.getId())
                .callbackUrl(props.getCallbackUrl())
                .build();

        VtbRegisterResponse response = vtbHttpClient.register(registerRequest);

        log.info("[VTB] Registered order: vtbOrderId={} for order #{}",
                response.orderId(), order.getId());

        return new GatewayInitResult(
                response.orderId(),
                response.formUrl(),
                amount,
                "KZT",
                null);
    }

    /**
     * Processes a VTB server callback (GET request from VTB).
     * Checksum result is logged only; getOrderStatusExtended is always the security gate.
     */
    @Override
    public GatewayCallbackResult handleCallback(Map<String, String> params) {
        String mdOrder = params.get("mdOrder");
        if (mdOrder == null || mdOrder.isBlank()) {
            throw new BusinessRuleException("Missing mdOrder in VTB callback params");
        }

        VtbChecksumResult checksumResult = checksumVerifier.verify(params, props.getHmacSecret());
        log.info("[VTB] Callback received: mdOrder={} checksumResult={}", mdOrder, checksumResult);

        VtbOrderStatusResponse statusResp = vtbHttpClient.getOrderStatus(mdOrder);
        return buildResult(mdOrder, statusResp, "vtb-callback");
    }

    /**
     * Verifies customer return from the VTB payment page.
     * VTB appends ?orderId={vtbGatewayUuid} to the returnUrl (confirmed from official plugin).
     */
    @Override
    public GatewayCallbackResult verifyReturn(Map<String, String> params) {
        String vtbOrderId = params.get("orderId");
        if (vtbOrderId == null || vtbOrderId.isBlank()) {
            throw new BusinessRuleException("Missing orderId in VTB return URL params");
        }

        log.info("[VTB] verifyReturn: vtbOrderId={}", vtbOrderId);
        VtbOrderStatusResponse statusResp = vtbHttpClient.getOrderStatus(vtbOrderId);
        return buildResult(vtbOrderId, statusResp, "vtb-return");
    }

    @Override
    public void refund(String providerPaymentId, BigDecimal amount) {
        VtbOrderStatusResponse status = vtbHttpClient.getOrderStatus(providerPaymentId);
        VtbOrderStatus vtbStatus = VtbOrderStatus.fromCode(
                status.orderStatus() != null ? status.orderStatus() : -1);

        if (vtbStatus == VtbOrderStatus.DEPOSITED) {
            vtbHttpClient.refund(providerPaymentId, VtbCurrencyMapper.toMinorUnits(amount));
        } else if (vtbStatus == VtbOrderStatus.PRE_AUTHORIZED) {
            vtbHttpClient.reverse(providerPaymentId);
        } else {
            throw new BusinessRuleException(
                    "Cannot refund VTB payment in status: " + vtbStatus);
        }
    }

    @Override
    public void cancel(String providerPaymentId) {
        VtbOrderStatusResponse status = vtbHttpClient.getOrderStatus(providerPaymentId);
        VtbOrderStatus vtbStatus = VtbOrderStatus.fromCode(
                status.orderStatus() != null ? status.orderStatus() : -1);

        if (vtbStatus == VtbOrderStatus.REGISTERED) {
            vtbHttpClient.cancel(providerPaymentId);
        } else {
            throw new BusinessRuleException(
                    "Cannot cancel VTB payment in status: " + vtbStatus);
        }
    }

    private GatewayCallbackResult buildResult(
            String orderId, VtbOrderStatusResponse statusResp, String source) {

        VtbOrderStatus vtbStatus = VtbOrderStatus.fromCode(
                statusResp.orderStatus() != null ? statusResp.orderStatus() : -1);
        PaymentStatus paymentStatus = vtbStatus.toPaymentStatus();

        // VTB amounts are in minor units (tiyn = KZT/100); convert to standard units.
        // Only validate amount when money actually moved (DEPOSITED or PRE_AUTHORIZED).
        // Declined/reversed/refunded payments report amount=0, which would fail the stored-amount
        // check in applyResult() — skip validation by leaving amount null for those statuses.
        BigDecimal amount = null;
        if (statusResp.amount() != null && statusResp.amount() > 0
                && (vtbStatus == VtbOrderStatus.DEPOSITED
                        || vtbStatus == VtbOrderStatus.PRE_AUTHORIZED)) {
            amount = BigDecimal.valueOf(statusResp.amount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        log.info("[VTB] {} orderId={} vtbStatus={} → paymentStatus={} amount={}",
                source, orderId, vtbStatus, paymentStatus, amount);

        return new GatewayCallbackResult(
                orderId, paymentStatus, amount, source + "-" + vtbStatus.name());
    }
}
