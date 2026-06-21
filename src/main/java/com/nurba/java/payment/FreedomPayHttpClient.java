package com.nurba.java.payment;

import com.nurba.java.config.FreedomPayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
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

/**
 * HTTP client for Freedom Pay Merchant API.
 * When {@code app.payment.freedompay.merchant-id} is blank, all calls use stub mode
 * (no network request is made, a fake result is returned).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreedomPayHttpClient {

    private final FreedomPayProperties props;

    @Value("${app.payment.stub.base-url:http://localhost:8080}")
    private String stubBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Initialises a payment via Freedom Pay's {@code init_payment.php}.
     * Returns stub data when merchantId is blank.
     */
    public FreedomPayInitResult initPayment(Long orderId, BigDecimal amount, String description) {
        if (props.isStubMode()) {
            return stub(orderId);
        }
        Map<String, String> params = buildInitParams(orderId, amount, description);
        params.put("pg_sig", FreedomPaySignature.sign("init_payment.php", params, props.getSecretKey()));
        try {
            String xml = postForm(props.getApiUrl() + "/init_payment.php", params);
            log.info("[FreedomPay] init_payment.php response for orderId={}: {}", orderId, xml);
            return parseAndVerify(xml, props.getSecretKey());
        } catch (Exception e) {
            log.error("[FreedomPay] init_payment.php call failed for orderId={}: {}", orderId, e.getMessage(), e);
            return new FreedomPayInitResult(null, null, false,
                    "Freedom Pay connection error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private FreedomPayInitResult stub(Long orderId) {
        log.info("[PAYMENT-STUB] FreedomPay running in stub mode — no real API call. orderId={}", orderId);
        String approveUrl = stubBaseUrl + "/api/v1/payments/stub/freedom-pay/approve?orderId=" + orderId;
        return new FreedomPayInitResult("stub-fp-" + orderId, approveUrl, true, null);
    }

    private Map<String, String> buildInitParams(Long orderId, BigDecimal amount, String description) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pg_merchant_id", props.getMerchantId());
        params.put("pg_order_id", orderId.toString());
        params.put("pg_amount", amount.toPlainString());
        params.put("pg_currency", "KZT");
        params.put("pg_description", description != null ? description : "Order #" + orderId);
        params.put("pg_salt", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        if (!props.getResultUrl().isBlank()) {
            params.put("pg_result_url", props.getResultUrl());
        }
        if (!props.getSuccessUrl().isBlank()) {
            params.put("pg_success_url", props.getSuccessUrl());
        }
        if (!props.getFailureUrl().isBlank()) {
            params.put("pg_failure_url", props.getFailureUrl());
        }
        if (props.isTestingMode()) {
            params.put("pg_testing_mode", "1");
        }
        return params;
    }

    private String postForm(String url, Map<String, String> params) throws Exception {
        String body = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Freedom Pay returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Parses the XML response from init_payment.php and verifies Freedom Pay's pg_sig.
     * Package-private to allow direct unit testing without an HTTP server.
     *
     * <p>Freedom Pay signs its own response with the same MD5 algorithm used for requests:
     * {@code MD5(init_payment.php ; sorted_values ; secretKey)}.
     * A mismatched signature indicates a configuration error or, in the worst case, a MITM.
     */
    static FreedomPayInitResult parseAndVerify(String xml, String secretKey) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            Map<String, String> fields = extractResponseFields(doc);
            String status = fields.get("pg_status");

            // Freedom Pay ERROR responses are NOT signed — they contain no pg_sig element
            // (verified against the live api.freedompay.kz: an error yields only pg_status,
            // pg_error_code, pg_error_description). Check status FIRST and surface the real
            // error. Verifying a missing signature here would always fail and mask the actual
            // cause as a bogus "pg_sig mismatch in response".
            if (!"ok".equalsIgnoreCase(status)) {
                String code  = fields.get("pg_error_code");
                String error = fields.get("pg_error_description");
                if (error == null) error = fields.get("pg_description");
                log.warn("[FreedomPay] init_payment.php returned pg_status={} pg_error_code={} description={}",
                        status, code, error);
                String msg = error != null ? error
                        : ("Freedom Pay error" + (code != null ? " " + code : ""));
                return new FreedomPayInitResult(null, null, false, msg);
            }

            // Success responses ARE signed — verify before trusting pg_payment_id / pg_redirect_url.
            String receivedSig = fields.get("pg_sig");
            if (!FreedomPaySignature.verify("init_payment.php", fields, secretKey, receivedSig)) {
                log.warn("[FreedomPay] pg_sig mismatch in successful init_payment.php response " +
                         "— possible MITM or secret key mismatch");
                return new FreedomPayInitResult(null, null, false,
                        "Invalid response signature from Freedom Pay");
            }
            return new FreedomPayInitResult(
                    fields.get("pg_payment_id"),
                    fields.get("pg_redirect_url"),
                    true, null);

        } catch (Exception e) {
            log.error("[FreedomPay] Failed to parse/verify init_payment.php response", e);
            return new FreedomPayInitResult(null, null, false,
                    "Failed to parse Freedom Pay response");
        }
    }

    /** Extracts all direct child elements of {@code <response>} into a Map (insertion order). */
    private static Map<String, String> extractResponseFields(Document doc) {
        Map<String, String> result = new LinkedHashMap<>();
        org.w3c.dom.NodeList children = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node node = children.item(i);
            if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                result.put(node.getNodeName(), node.getTextContent());
            }
        }
        return result;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
