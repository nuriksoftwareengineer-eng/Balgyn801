package com.nurba.java.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Freedom Pay MD5 signature utility.
 *
 * Algorithm: MD5(scriptName;sortedVal1;sortedVal2;...;sortedValN;secretKey)
 *  - Parameters are sorted alphabetically by key name.
 *  - Only pg_sig is excluded from the sorted parameter list.
 *    pg_salt IS included — it is a regular parameter that makes each signature unique.
 *  - The secret key is always appended last.
 *  - The entire string is MD5-hashed as UTF-8 bytes.
 *
 * Reference: Freedom Pay Merchant API docs + official PHP SDK (ksort, unset pg_sig only).
 */
public final class FreedomPaySignature {

    private FreedomPaySignature() {}

    public static String sign(String scriptName, Map<String, String> params, String secretKey) {
        StringBuilder sb = new StringBuilder(scriptName);
        new TreeMap<>(params).forEach((key, value) -> {
            if (!"pg_sig".equals(key)) {           // exclude ONLY pg_sig; pg_salt IS included
                sb.append(';').append(value);
            }
        });
        sb.append(';').append(secretKey);
        return md5(sb.toString());
    }

    /** Constant-time comparison to prevent timing attacks. */
    public static boolean verify(String scriptName, Map<String, String> params,
                                  String secretKey, String received) {
        if (received == null || received.isBlank()) return false;
        String expected = sign(scriptName, params, secretKey);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
