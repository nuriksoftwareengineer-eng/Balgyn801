package com.nurba.java;

import com.nurba.java.config.PaymentWebhookProperties;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.security.webhook.WebhookSignatureService;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тест безопасности webhook-подписи (без Spring-контекста).
 * Доказывает фикс: при пустом секрете неподписанный webhook ОТКЛОНЯЕТСЯ по умолчанию,
 * а bypass возможен только при явном allow-unsigned=true.
 */
class WebhookSignatureServiceTest {

    private static final String SECRET = "unit-test-webhook-secret-32chars!!";

    @Test
    void emptySecret_rejectedByDefault() {
        PaymentWebhookProperties props = new PaymentWebhookProperties(); // allowUnsigned=false
        WebhookSignatureService svc = new WebhookSignatureService(props);

        boolean ok = svc.isValid(PaymentProvider.KASPI, "{}".getBytes(StandardCharsets.UTF_8), null);

        assertThat(ok).as("неподписанный webhook при пустом секрете должен отклоняться").isFalse();
    }

    @Test
    void emptySecret_bypassedOnlyWhenAllowUnsignedTrue() {
        PaymentWebhookProperties props = new PaymentWebhookProperties();
        props.setAllowUnsigned(true);
        WebhookSignatureService svc = new WebhookSignatureService(props);

        boolean ok = svc.isValid(PaymentProvider.KASPI, "{}".getBytes(StandardCharsets.UTF_8), null);

        assertThat(ok).as("в dev/stub при allow-unsigned=true bypass разрешён").isTrue();
    }

    @Test
    void configuredSecret_validAccepted_invalidAndMissingRejected() throws Exception {
        PaymentWebhookProperties props = new PaymentWebhookProperties();
        props.getSecrets().put(PaymentProvider.KASPI.name(), SECRET);
        WebhookSignatureService svc = new WebhookSignatureService(props);

        byte[] body = "{\"paymentId\":1,\"status\":\"succeeded\"}".getBytes(StandardCharsets.UTF_8);
        String validSig = hmac(body, SECRET);

        assertThat(svc.isValid(PaymentProvider.KASPI, body, "hmac_sha256=" + validSig)).isTrue();
        assertThat(svc.isValid(PaymentProvider.KASPI, body, "hmac_sha256=deadbeef")).isFalse();
        assertThat(svc.isValid(PaymentProvider.KASPI, body, null)).isFalse();
    }

    private static String hmac(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}
