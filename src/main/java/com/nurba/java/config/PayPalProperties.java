package com.nurba.java.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.payment.paypal")
@Data
public class PayPalProperties {

    /** "sandbox" or "live". Determines the base URL. */
    private String mode = "sandbox";

    /** OAuth2 client ID from the PayPal Developer Dashboard. */
    private String clientId = "";

    /** OAuth2 client secret from the PayPal Developer Dashboard. */
    private String clientSecret = "";

    /**
     * Webhook ID from the PayPal Developer Dashboard (required for
     * webhook signature verification via the verify-webhook-signature API).
     */
    private String webhookId = "";

    /** Resolved automatically from {@link #mode}. */
    public String getBaseUrl() {
        return "live".equalsIgnoreCase(mode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    /** True when credentials are absent — stub mode, no real PayPal API calls. */
    public boolean isStubMode() {
        return isPlaceholder(clientId) || isPlaceholder(clientSecret);
    }

    private static boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || value.startsWith("YOUR_");
    }
}
