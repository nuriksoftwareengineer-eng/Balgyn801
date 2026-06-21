package com.nurba.java.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.payment.freedompay")
@Data
public class FreedomPayProperties {

    /**
     * Freedom Pay merchant ID. When blank, stub mode is used:
     * no real API call is made, and a fake redirect URL is returned.
     */
    private String merchantId = "";

    /**
     * Secret key for MD5 signature generation and verification.
     * Required for both init and callback signature verification.
     */
    private String secretKey = "";

    /** Freedom Pay API base URL. */
    private String apiUrl = "https://api.freedompay.kz";

    /**
     * URL Freedom Pay will POST the payment result to (our callback endpoint).
     * Should be publicly reachable: e.g. https://api.example.com/api/v1/payments/callback/freedom-pay
     */
    private String resultUrl = "";

    /** URL to redirect the user to after a successful payment. */
    private String successUrl = "";

    /** URL to redirect the user to after a failed payment. */
    private String failureUrl = "";

    /**
     * Script name used in MD5 signature for our callback endpoint.
     * Must match the last path segment of resultUrl (e.g. "freedom-pay").
     * Freedom Pay uses this as the first component when computing pg_sig.
     */
    private String callbackScriptName = "freedom-pay";

    /** Set to true to send pg_testing_mode=1 to Freedom Pay (sandbox testing). */
    private boolean testingMode = false;

    /** True when credentials are absent — stub mode, no real FreedomPay API calls. */
    public boolean isStubMode() {
        return isPlaceholder(merchantId) || isPlaceholder(secretKey);
    }

    private static boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || value.startsWith("YOUR_");
    }
}
