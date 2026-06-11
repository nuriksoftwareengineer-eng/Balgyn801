package com.nurba.java.config;

import com.nurba.java.enums.PaymentProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ConfigurationProperties("app.payment.webhook")
@Data
public class PaymentWebhookProperties {

    /** Per-provider HMAC-SHA256 signing secret. Empty value = bypass check (dev/stub mode). */
    private Map<String, String> secrets = new HashMap<>();

    /** Max payment init calls per minute per remote IP. */
    private int initRateLimitPerMinute = 10;

    /** Max webhook calls per minute per remote IP. */
    private int webhookRateLimitPerMinute = 100;

    public Optional<String> getSecret(PaymentProvider provider) {
        String secret = secrets.get(provider.name());
        return (secret == null || secret.isBlank()) ? Optional.empty() : Optional.of(secret);
    }
}
