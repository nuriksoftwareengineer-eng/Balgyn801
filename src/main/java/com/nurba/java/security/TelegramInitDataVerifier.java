package com.nurba.java.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies Telegram Mini App {@code initData} per the official algorithm
 * (https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app):
 * HMAC-SHA256 over the sorted key=value pairs, keyed by HMAC-SHA256("WebAppData", botToken),
 * compared in constant time — plus an auth_date freshness check as replay protection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramInitDataVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String WEBAPP_DATA_KEY = "WebAppData";

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.init-data-max-age-seconds:300}")
    private long maxAgeSeconds;

    private final ObjectMapper objectMapper;

    public TelegramUserData verify(String initData) {
        if (botToken == null || botToken.isBlank()) {
            throw new BusinessRuleException("Вход через Telegram временно недоступен");
        }
        if (initData == null || initData.isBlank()) {
            throw new BusinessRuleException("Отсутствуют данные Telegram initData");
        }

        Map<String, String> params = parseQueryString(initData);
        String receivedHash = params.remove("hash");
        params.remove("signature"); // ed25519 alt-verification field — excluded from the check-string, not used here
        if (receivedHash == null || receivedHash.isBlank()) {
            throw new BusinessRuleException("Отсутствует подпись Telegram initData");
        }

        String dataCheckString = buildDataCheckString(params);
        String computedHash = computeHash(dataCheckString);

        if (!constantTimeEquals(computedHash, receivedHash.toLowerCase())) {
            log.warn("[Telegram] initData signature mismatch");
            throw new BusinessRuleException("Неверная подпись Telegram initData");
        }

        long authDateEpochSeconds = parseAuthDate(params.get("auth_date"));
        Instant authDate = Instant.ofEpochSecond(authDateEpochSeconds);
        if (authDate.plusSeconds(maxAgeSeconds).isBefore(Instant.now())) {
            throw new BusinessRuleException("Данные Telegram устарели, откройте приложение заново");
        }

        String userJson = params.get("user");
        if (userJson == null || userJson.isBlank()) {
            throw new BusinessRuleException("Отсутствуют данные пользователя Telegram");
        }
        return parseUser(userJson);
    }

    private static Map<String, String> parseQueryString(String initData) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : initData.split("&")) {
            if (pair.isBlank()) continue;
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String rawValue = eq >= 0 ? pair.substring(eq + 1) : "";
            result.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(rawValue, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String buildDataCheckString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(key).append('=').append(params.get(key));
        }
        return sb.toString();
    }

    private String computeHash(String dataCheckString) {
        try {
            Mac secretKeyMac = Mac.getInstance(HMAC_SHA256);
            secretKeyMac.init(new SecretKeySpec(WEBAPP_DATA_KEY.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] secretKey = secretKeyMac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

            Mac dataMac = Mac.getInstance(HMAC_SHA256);
            dataMac.init(new SecretKeySpec(secretKey, HMAC_SHA256));
            byte[] hash = dataMac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (GeneralSecurityException e) {
            log.error("[Telegram] Failed to compute initData HMAC", e);
            throw new BusinessRuleException("Не удалось проверить подпись Telegram");
        }
    }

    private static long parseAuthDate(String authDateStr) {
        if (authDateStr == null || authDateStr.isBlank()) {
            throw new BusinessRuleException("Отсутствует auth_date в Telegram initData");
        }
        try {
            return Long.parseLong(authDateStr);
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Некорректный auth_date в Telegram initData");
        }
    }

    private static boolean constantTimeEquals(String computedHex, String receivedHex) {
        return MessageDigest.isEqual(
                computedHex.getBytes(StandardCharsets.UTF_8),
                receivedHex.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private TelegramUserData parseUser(String userJson) {
        try {
            JsonNode node = objectMapper.readTree(userJson);
            long id = node.path("id").asLong(-1);
            if (id <= 0) {
                throw new BusinessRuleException("Некорректный идентификатор пользователя Telegram");
            }
            return new TelegramUserData(
                    id,
                    node.hasNonNull("username") ? node.get("username").asText() : null,
                    node.hasNonNull("first_name") ? node.get("first_name").asText() : null,
                    node.hasNonNull("last_name") ? node.get("last_name").asText() : null,
                    node.hasNonNull("photo_url") ? node.get("photo_url").asText() : null);
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Telegram] Failed to parse initData user field: {}", e.getMessage());
            throw new BusinessRuleException("Не удалось разобрать данные пользователя Telegram");
        }
    }
}
