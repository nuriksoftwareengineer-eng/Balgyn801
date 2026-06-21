package com.nurba.java.payment;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for pg_sig verification of the init_payment.php response.
 *
 * Freedom Pay signs its own response with the same MD5 algorithm used for requests.
 * We verify the signature before trusting any field in the response (pg_payment_id, pg_redirect_url).
 * Tests call FreedomPayHttpClient.parseAndVerify() directly — no HTTP server needed.
 */
class FreedomPayResponseVerificationTest {

    private static final String SECRET       = "test-response-secret-key-xyz!!!";
    private static final String SCRIPT_NAME  = "init_payment.php";

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Returns the five fields a typical Freedom Pay ok-response contains (no pg_sig yet). */
    private Map<String, String> okFields() {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("pg_status",            "ok");
        f.put("pg_payment_id",        "98765");
        f.put("pg_redirect_url",      "https://api.freedompay.kz/pay.html?token=abc");
        f.put("pg_redirect_url_type", "need data");
        f.put("pg_salt",              "testsalt12345678");
        return f;
    }

    /** Builds a valid XML response that includes a correctly computed pg_sig. */
    private String signedXml(Map<String, String> fields, String secret) {
        String sig = FreedomPaySignature.sign(SCRIPT_NAME, fields, secret);
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><response>");
        fields.forEach((k, v) -> sb.append('<').append(k).append('>').append(v).append("</").append(k).append('>'));
        sb.append("<pg_sig>").append(sig).append("</pg_sig></response>");
        return sb.toString();
    }

    /** Builds XML with an arbitrary (wrong) pg_sig. */
    private String xmlWithSig(Map<String, String> fields, String wrongSig) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><response>");
        fields.forEach((k, v) -> sb.append('<').append(k).append('>').append(v).append("</").append(k).append('>'));
        sb.append("<pg_sig>").append(wrongSig).append("</pg_sig></response>");
        return sb.toString();
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test
    void validSignature_returnsSuccess() {
        String xml = signedXml(okFields(), SECRET);

        FreedomPayInitResult result = FreedomPayHttpClient.parseAndVerify(xml, SECRET);

        assertThat(result.success()).isTrue();
        assertThat(result.providerPaymentId()).isEqualTo("98765");
        assertThat(result.redirectUrl()).isEqualTo("https://api.freedompay.kz/pay.html?token=abc");
        assertThat(result.errorDescription()).isNull();
    }

    @Test
    void tamperedSig_allZeros_rejected() {
        String xml = xmlWithSig(okFields(), "00000000000000000000000000000000");

        FreedomPayInitResult result = FreedomPayHttpClient.parseAndVerify(xml, SECRET);

        assertThat(result.success()).isFalse();
        assertThat(result.providerPaymentId()).isNull();
        assertThat(result.redirectUrl()).isNull();
        assertThat(result.errorDescription()).contains("signature");
    }

    @Test
    void wrongSecretKey_rejected() {
        // Freedom Pay signed with their key; we verify with a different secret → mismatch
        String xml = signedXml(okFields(), "attacker-does-not-know-real-secret");

        FreedomPayInitResult result = FreedomPayHttpClient.parseAndVerify(xml, SECRET);

        assertThat(result.success()).isFalse();
        assertThat(result.errorDescription()).contains("signature");
    }

    @Test
    void missingSig_rejected() {
        // XML has no pg_sig element at all
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response>" +
                     "<pg_status>ok</pg_status>" +
                     "<pg_payment_id>98765</pg_payment_id>" +
                     "<pg_salt>testsalt</pg_salt>" +
                     "</response>";

        FreedomPayInitResult result = FreedomPayHttpClient.parseAndVerify(xml, SECRET);

        assertThat(result.success()).isFalse();
    }

    @Test
    void errorResponse_withoutSig_surfacesRealError_notSigMismatch() {
        // Real Freedom Pay error responses (verified against live api.freedompay.kz) carry NO
        // pg_sig element — only pg_status / pg_error_code / pg_error_description. The previous
        // code verified the signature first, so verify(null) failed and masked every error as
        // "Invalid response signature". We must surface the actual error instead.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response>" +
                     "<pg_status>error</pg_status>" +
                     "<pg_error_code>9998</pg_error_code>" +
                     "<pg_error_description>Некорректная подпись запроса</pg_error_description>" +
                     "</response>";

        FreedomPayInitResult result = FreedomPayHttpClient.parseAndVerify(xml, SECRET);

        assertThat(result.success()).isFalse();
        // The REAL Freedom Pay error is surfaced verbatim (it legitimately mentions "подпись"),
        // NOT replaced by our generic "Invalid response signature from Freedom Pay" masking text.
        assertThat(result.errorDescription()).isEqualTo("Некорректная подпись запроса");
        assertThat(result.errorDescription()).isNotEqualTo("Invalid response signature from Freedom Pay");
    }

    @Test
    void errorStatus_withValidSig_returnsFailure() {
        // Freedom Pay can return pg_status=error with a valid signature
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("pg_status",      "error");
        fields.put("pg_description", "Merchant not found");
        fields.put("pg_salt",        "errorsalt999");

        String xml = signedXml(fields, SECRET);

        FreedomPayInitResult result = FreedomPayHttpClient.parseAndVerify(xml, SECRET);

        assertThat(result.success()).isFalse();
        assertThat(result.errorDescription()).isEqualTo("Merchant not found");
    }

    @Test
    void sigVerification_isCaseSensitiveOnValues() {
        // Altering a value (redirect URL uppercase) invalidates the sig
        Map<String, String> fields = okFields();
        String xml = signedXml(fields, SECRET);

        // Tamper: replace the redirect URL in the XML after signing
        String tampered = xml.replace("https://api.freedompay.kz/pay.html?token=abc",
                                      "https://EVIL.example.com/steal");

        FreedomPayInitResult result = FreedomPayHttpClient.parseAndVerify(tampered, SECRET);

        assertThat(result.success()).isFalse();
    }
}
