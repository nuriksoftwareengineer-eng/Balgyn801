package com.nurba.java.payment.vtb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Optional SHA-256 checksum verification for VTB Kazakhstan callbacks.
 *
 * Both official CMS plugins (WordPress and Magento) skip checksum verification and rely solely
 * on getOrderStatusExtended.do as the security gate. This verifier is defense-in-depth only:
 * its result is logged but never blocks processing.
 */
@Slf4j
@Component
public class VtbCallbackVerifier {

    /**
     * Verifies the checksum when callbackSecret is configured.
     * Never throws — result is informational only. API verification is always the security gate.
     */
    public VtbChecksumResult verify(Map<String, String> params, String callbackSecret) {
        if (callbackSecret == null || callbackSecret.isBlank()) {
            return VtbChecksumResult.SKIPPED;
        }

        String receivedChecksum = params.get("checksum");
        if (receivedChecksum == null || receivedChecksum.isBlank()) {
            return VtbChecksumResult.ABSENT;
        }

        // Sort params by key, exclude checksum itself
        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("checksum");

        // SHA256(sorted_values_semicolons ; callbackSecret)
        StringBuilder sb = new StringBuilder();
        for (String value : sorted.values()) {
            if (!sb.isEmpty()) sb.append(";");
            sb.append(value != null ? value : "");
        }
        sb.append(";").append(callbackSecret);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            String computed = toHex(hash);
            boolean valid = computed.equalsIgnoreCase(receivedChecksum);
            if (!valid) {
                log.debug("[VTB] Checksum mismatch: computed={} received={}", computed, receivedChecksum);
            }
            return valid ? VtbChecksumResult.VALID : VtbChecksumResult.INVALID;
        } catch (NoSuchAlgorithmException e) {
            log.error("[VTB] SHA-256 not available", e);
            return VtbChecksumResult.SKIPPED;
        }
    }

    private static String toHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
