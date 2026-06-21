package com.nurba.java.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.payment.webhook")
@Data
public class PaymentWebhookProperties {

    /** Max payment init calls per minute per remote IP. */
    private int initRateLimitPerMinute = 10;

    /** Max Freedom Pay callback calls per minute per remote IP. */
    private int webhookRateLimitPerMinute = 100;
}
