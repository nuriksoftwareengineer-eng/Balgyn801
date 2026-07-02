package com.nurba.java.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties("app.payment.vtb")
@Data
public class VtbProperties {

    /** VTB API username (login ending in -api). Blank = stub mode. */
    private String username = "";

    /** VTB API password. Blank = stub mode. */
    private String password = "";

    /** true = sandbox API (vtbkz.rbsuat.com), false = production (payment.vtb.kz). */
    private boolean sandbox = true;

    /** Publicly accessible callback URL — VTB will GET this on payment events. */
    private String callbackUrl = "";

    /** Return URL base — VTB appends ?orderId={vtbUuid} and redirects the buyer here. */
    private String returnUrl = "";

    /** HMAC secret for optional callback checksum verification. Blank = skip checksum. */
    private String hmacSecret = "";

    /**
     * Comma-separated ISO 4217 numeric codes this merchant supports.
     * Default: "398" (KZT only). Set "398,840,978" for multi-currency agreement.
     */
    private String supportedCurrencies = "398";

    /**
     * If true, silently fall back to KZT for unsupported currencies.
     * Default: false — throw BusinessRuleException instead (explicit, no silent conversion).
     */
    private boolean fallbackToKzt = false;

    public boolean isStubMode() {
        return isPlaceholder(username) || isPlaceholder(password);
    }

    public String getApiUrl() {
        return sandbox
                ? "https://vtbkz.rbsuat.com/payment/rest"
                : "https://payment.vtb.kz/payment/rest";
    }

    public Set<Integer> getSupportedCurrencyCodes() {
        return Arrays.stream(supportedCurrencies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    private static boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || value.startsWith("YOUR_");
    }
}
