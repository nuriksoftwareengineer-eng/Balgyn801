package com.nurba.java.security.webhook;

import com.nurba.java.config.PaymentWebhookProperties;
import com.nurba.java.enums.PaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Verifies HMAC-SHA256 webhook signatures for each payment provider.
 *
 * <p>Header: {@code X-Webhook-Signature: hmac_sha256=<hex>}<br>
 * The HMAC is computed over the raw UTF-8 request body using the per-provider secret
 * configured in {@code app.payment.webhook.secrets.<PROVIDER>}.</p>
 *
 * <p>If no secret is configured for a provider the webhook is rejected by default
 * (prod-safe). Unsigned webhooks are only accepted when {@code app.payment.webhook.allow-unsigned=true}
 * is explicitly set — for local development and stub testing without real provider credentials.</p>
 */
@Service
@RequiredArgsConstructor
public class WebhookSignatureService {

    public static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "hmac_sha256=";

    private final PaymentWebhookProperties properties;

    /**
     * Returns {@code true} only if the signature is valid. If no secret is configured,
     * returns {@code allow-unsigned} (false in prod → reject). Returns {@code false} if a
     * secret is configured but the header is absent/wrong.
     */
    public boolean isValid(PaymentProvider provider, byte[] body, String signatureHeader) {
        Optional<String> secret = properties.getSecret(provider);
        if (secret.isEmpty()) {
            // Секрет не настроен: в проде (allow-unsigned=false) отклоняем неподписанный webhook,
            // чтобы исключить подделку «оплачено». Bypass возможен только явным opt-in в dev/stub.
            return properties.isAllowUnsigned();
        }
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }
        String received = signatureHeader.substring(PREFIX.length()).trim();
        String expected = computeHmac(body, secret.get());
        return MessageDigest.isEqual(
                received.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private String computeHmac(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute webhook HMAC", e);
        }
    }
}
