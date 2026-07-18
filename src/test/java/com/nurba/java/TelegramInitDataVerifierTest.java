package com.nurba.java;

import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.security.TelegramInitDataVerifier;
import com.nurba.java.security.TelegramUserData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies TelegramInitDataVerifier against real HMAC-SHA256-signed payloads, built here the
 * same way Telegram's client SDK builds them — no mocking, matching this codebase's convention
 * of exercising the real algorithm end-to-end rather than stubbing it out.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.telegram.bot-token=test-bot-token-12345")
class TelegramInitDataVerifierTest {

    private static final String BOT_TOKEN = "test-bot-token-12345";

    @Autowired
    private TelegramInitDataVerifier verifier;

    @Test
    void verify_validSignatureAndFreshAuthDate_extractsUser() {
        String initData = signedInitData(Instant.now().getEpochSecond(), 123456789L, "john_doe");

        TelegramUserData result = verifier.verify(initData);

        assertThat(result.telegramId()).isEqualTo(123456789L);
        assertThat(result.username()).isEqualTo("john_doe");
        assertThat(result.firstName()).isEqualTo("John");
    }

    @Test
    void verify_initDataWithSignatureField_stillValidates() {
        // Regression test: real Telegram initData always carries a "signature" field (Ed25519,
        // a separate bot-token-free verification mechanism) alongside "hash". Per Telegram's own
        // spec, the HMAC data-check-string used here excludes only "hash" — "signature" must be
        // included as just another field. This test signs the payload WITH signature included in
        // the data-check-string, exactly like Telegram's real client does; if the verifier were
        // to exclude "signature" when building its own data-check-string (as it incorrectly did
        // before this fix), the hashes would no longer match and this test would fail with a
        // BusinessRuleException instead of returning the user.
        String initData = signedInitDataWithSignature(Instant.now().getEpochSecond(), 555555555L, "sig_field_user");

        TelegramUserData result = verifier.verify(initData);

        assertThat(result.telegramId()).isEqualTo(555555555L);
        assertThat(result.username()).isEqualTo("sig_field_user");
    }

    @Test
    void verify_tamperedPayload_isRejected() {
        String initData = signedInitData(Instant.now().getEpochSecond(), 123456789L, "john_doe");
        // Flip the user id after signing — the signature no longer matches the payload.
        String tampered = initData.replace("123456789", "999999999");

        assertThatThrownBy(() -> verifier.verify(tampered))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void verify_staleAuthDate_isRejectedAsReplay() {
        long tenMinutesAgo = Instant.now().getEpochSecond() - 600; // default max age is 300s
        String initData = signedInitData(tenMinutesAgo, 123456789L, "john_doe");

        assertThatThrownBy(() -> verifier.verify(initData))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void verify_missingHash_isRejected() {
        String noHash = "auth_date=" + Instant.now().getEpochSecond() + "&user=%7B%7D";

        assertThatThrownBy(() -> verifier.verify(noHash))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void verify_blankInitData_isRejected() {
        assertThatThrownBy(() -> verifier.verify("")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> verifier.verify(null)).isInstanceOf(BusinessRuleException.class);
    }

    /** Builds a real, correctly HMAC-signed initData string exactly like Telegram's client does. */
    private static String signedInitData(long authDateEpochSeconds, long userId, String username) {
        String userJson = "{\"id\":" + userId + ",\"username\":\"" + username + "\",\"first_name\":\"John\"}";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDateEpochSeconds));
        params.put("query_id", "AAHtest");
        params.put("user", userJson);

        String hash = hmacHex(buildDataCheckString(params));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        sb.append("&hash=").append(hash);
        return sb.toString();
    }

    /** Same as {@link #signedInitData}, but includes an Ed25519 "signature" field in both the
     *  wire string and the HMAC data-check-string, matching what real Telegram clients send. */
    private static String signedInitDataWithSignature(long authDateEpochSeconds, long userId, String username) {
        String userJson = "{\"id\":" + userId + ",\"username\":\"" + username + "\",\"first_name\":\"John\"}";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDateEpochSeconds));
        params.put("query_id", "AAHtest");
        params.put("signature", "not-a-real-ed25519-signature-just-needs-to-be-present-and-hashed");
        params.put("user", userJson);

        String hash = hmacHex(buildDataCheckString(params));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        sb.append("&hash=").append(hash);
        return sb.toString();
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

    private static String hmacHex(String dataCheckString) {
        try {
            Mac secretKeyMac = Mac.getInstance("HmacSHA256");
            secretKeyMac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] secretKey = secretKeyMac.doFinal(BOT_TOKEN.getBytes(StandardCharsets.UTF_8));

            Mac dataMac = Mac.getInstance("HmacSHA256");
            dataMac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] hash = dataMac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
