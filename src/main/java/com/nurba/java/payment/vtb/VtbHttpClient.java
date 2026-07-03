package com.nurba.java.payment.vtb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.config.VtbProperties;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.payment.vtb.dto.VtbOrderStatusResponse;
import com.nurba.java.payment.vtb.dto.VtbRegisterRequest;
import com.nurba.java.payment.vtb.dto.VtbRegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VtbHttpClient {

    private final VtbProperties props;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.stub.base-url:http://localhost:8080}")
    private String stubBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public VtbRegisterResponse register(VtbRegisterRequest request) {
        if (props.isStubMode()) {
            String stubMdOrder = "stub-vtb-" + UUID.randomUUID();
            String orderId = request.orderNumber().contains("_")
                    ? request.orderNumber().split("_")[0]
                    : request.orderNumber();
            String formUrl = stubBaseUrl + "/api/v1/payments/stub/vtb/" + orderId
                    + "?mdOrder=" + stubMdOrder;
            log.info("[PAYMENT-STUB] VTB running in stub mode. stubMdOrder={}", stubMdOrder);
            return new VtbRegisterResponse(stubMdOrder, formUrl, 0, null);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("userName", props.getUsername());
        params.put("password", props.getPassword());
        params.put("orderNumber", request.orderNumber());
        params.put("amount", String.valueOf(request.amount()));
        params.put("currency", String.valueOf(request.currency()));
        params.put("returnUrl", request.returnUrl());
        params.put("description", request.description() != null ? request.description() : "");
        if (request.callbackUrl() != null && !request.callbackUrl().isBlank()) {
            params.put("callbackUrl", request.callbackUrl());
        }

        try {
            String json = post(props.getApiUrl() + "/register.do", params);
            log.info("[VTB] register.do response: {}", json);
            VtbRegisterResponse response = objectMapper.readValue(json, VtbRegisterResponse.class);
            if (!response.isSuccess()) {
                throw new BusinessRuleException(
                        "VTB register.do failed: errorCode=" + response.errorCode()
                        + " message=" + response.errorMessage());
            }
            return response;
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("[VTB] register.do error: {}", e.getMessage(), e);
            throw new BusinessRuleException("VTB payment registration failed: " + e.getMessage());
        }
    }

    public VtbOrderStatusResponse getOrderStatus(String vtbOrderId) {
        if (props.isStubMode()) {
            // Stub: always report DEPOSITED (orderStatus=2)
            log.info("[PAYMENT-STUB] VTB getOrderStatus stub: vtbOrderId={} → DEPOSITED", vtbOrderId);
            return new VtbOrderStatusResponse(2, 0, null, null, null, null);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("userName", props.getUsername());
        params.put("password", props.getPassword());
        params.put("orderId", vtbOrderId);

        try {
            String json = post(props.getApiUrl() + "/getOrderStatusExtended.do", params);
            log.info("[VTB] getOrderStatusExtended.do response: {}", json);
            VtbOrderStatusResponse response = objectMapper.readValue(json, VtbOrderStatusResponse.class);
            if (!response.isSuccess()) {
                throw new BusinessRuleException(
                        "VTB getOrderStatus failed: errorCode=" + response.errorCode()
                        + " message=" + response.errorMessage());
            }
            return response;
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("[VTB] getOrderStatusExtended.do error for orderId={}: {}", vtbOrderId, e.getMessage(), e);
            throw new BusinessRuleException("VTB status check failed: " + e.getMessage());
        }
    }

    public void refund(String vtbOrderId, long amountMinorUnits) {
        if (props.isStubMode()) {
            log.info("[PAYMENT-STUB] VTB refund stub: orderId={} amount={}", vtbOrderId, amountMinorUnits);
            return;
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("userName", props.getUsername());
        params.put("password", props.getPassword());
        params.put("orderId", vtbOrderId);
        params.put("amount", String.valueOf(amountMinorUnits));
        try {
            String json = post(props.getApiUrl() + "/refund.do", params);
            log.info("[VTB] refund.do response: {}", json);
        } catch (Exception e) {
            log.error("[VTB] refund.do error: {}", e.getMessage(), e);
            throw new BusinessRuleException("VTB refund failed: " + e.getMessage());
        }
    }

    public void reverse(String vtbOrderId) {
        if (props.isStubMode()) {
            log.info("[PAYMENT-STUB] VTB reverse stub: orderId={}", vtbOrderId);
            return;
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("userName", props.getUsername());
        params.put("password", props.getPassword());
        params.put("orderId", vtbOrderId);
        try {
            String json = post(props.getApiUrl() + "/reverse.do", params);
            log.info("[VTB] reverse.do response: {}", json);
        } catch (Exception e) {
            log.error("[VTB] reverse.do error: {}", e.getMessage(), e);
            throw new BusinessRuleException("VTB reverse failed: " + e.getMessage());
        }
    }

    public void cancel(String vtbOrderId) {
        if (props.isStubMode()) {
            log.info("[PAYMENT-STUB] VTB cancel stub: orderId={}", vtbOrderId);
            return;
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("userName", props.getUsername());
        params.put("password", props.getPassword());
        params.put("orderId", vtbOrderId);
        try {
            String json = post(props.getApiUrl() + "/cancel.do", params);
            log.info("[VTB] cancel.do response: {}", json);
        } catch (Exception e) {
            log.error("[VTB] cancel.do error: {}", e.getMessage(), e);
            throw new BusinessRuleException("VTB cancel failed: " + e.getMessage());
        }
    }

    private String post(String url, Map<String, String> params) throws Exception {
        String body = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "VTB returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
