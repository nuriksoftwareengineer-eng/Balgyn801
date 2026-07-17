package com.nurba.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.repositories.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives POST /api/v1/auth/telegram and /telegram/link end-to-end (real HMAC-signed initData,
 * real DB, real JWTs) — no mocking, matching this codebase's integration-test convention.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.telegram.bot-token=test-bot-token-12345")
class TelegramAuthIntegrationTest {

    private static final String BOT_TOKEN = "test-bot-token-12345";
    private static final String TELEGRAM_LOGIN_URL = "/api/v1/auth/telegram";
    private static final String TELEGRAM_LINK_URL = "/api/v1/auth/telegram/link";
    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String ME_URL = "/api/v1/auth/me";

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AppUserRepository appUserRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        appUserRepository.deleteAll();
    }

    @Test
    void loginTelegram_newUser_createsAccountAndIssuesJwt() throws Exception {
        String initData = signedInitData(111111111L, "alice");

        MvcResult result = mockMvc.perform(post(TELEGRAM_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escape(initData) + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode auth = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = auth.get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        MvcResult meResult = mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode me = objectMapper.readTree(meResult.getResponse().getContentAsString());
        assertThat(me.get("email").asText()).startsWith("tg_111111111@");
        assertThat(me.get("telegramConnected").asBoolean()).isTrue();
        assertThat(me.get("telegramUsername").asText()).isEqualTo("alice");

        assertThat(appUserRepository.findByTelegramId(111111111L)).isPresent();
    }

    @Test
    void loginTelegram_sameTelegramId_reusesSameAccount() throws Exception {
        String firstLogin = signedInitData(222222222L, "bob");
        mockMvc.perform(post(TELEGRAM_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escape(firstLogin) + "\"}"))
                .andExpect(status().isOk());

        String secondLogin = signedInitData(222222222L, "bob");
        mockMvc.perform(post(TELEGRAM_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escape(secondLogin) + "\"}"))
                .andExpect(status().isOk());

        long matchingRows = appUserRepository.findAll().stream()
                .filter(u -> Long.valueOf(222222222L).equals(u.getTelegramId()))
                .count();
        assertThat(matchingRows).isEqualTo(1);
    }

    @Test
    void linkTelegram_existingJwtUser_setsTelegramIdAndReflectsInMe() throws Exception {
        String browserToken = registerAndGetToken("linker@test.com");
        String initData = signedInitData(333333333L, "carol");

        MvcResult linkResult = mockMvc.perform(post(TELEGRAM_LINK_URL)
                        .header("Authorization", "Bearer " + browserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escape(initData) + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode linkResponse = objectMapper.readTree(linkResult.getResponse().getContentAsString());
        assertThat(linkResponse.get("telegramConnected").asBoolean()).isTrue();
        assertThat(linkResponse.get("telegramUsername").asText()).isEqualTo("carol");
        assertThat(linkResponse.get("email").asText()).isEqualTo("linker@test.com");

        MvcResult meResult = mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer " + browserToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode me = objectMapper.readTree(meResult.getResponse().getContentAsString());
        assertThat(me.get("telegramConnected").asBoolean()).isTrue();

        // No second account was created — the browser user's own row was updated in place.
        assertThat(appUserRepository.findAll()).hasSize(1);
    }

    @Test
    void linkTelegram_alreadyLinkedToDifferentAccount_isRejected() throws Exception {
        String ownerToken = registerAndGetToken("owner@test.com");
        String initData = signedInitData(444444444L, "dave");
        mockMvc.perform(post(TELEGRAM_LINK_URL)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escape(initData) + "\"}"))
                .andExpect(status().isOk());

        String otherToken = registerAndGetToken("other@test.com");
        String sameTelegramAccount = signedInitData(444444444L, "dave");
        mockMvc.perform(post(TELEGRAM_LINK_URL)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escape(sameTelegramAccount) + "\"}"))
                .andExpect(status().isBadRequest());

        // The second user was never mutated.
        MvcResult meResult = mockMvc.perform(get(ME_URL)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode me = objectMapper.readTree(meResult.getResponse().getContentAsString());
        assertThat(me.get("telegramConnected").asBoolean()).isFalse();
    }

    @Test
    void loginTelegram_tamperedInitData_isRejectedAndCreatesNoUser() throws Exception {
        String initData = signedInitData(555555555L, "eve");
        String tampered = initData.replace("555555555", "666666666");

        mockMvc.perform(post(TELEGRAM_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escape(tampered) + "\"}"))
                .andExpect(status().isBadRequest());

        assertThat(appUserRepository.findAll()).isEmpty();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email) throws Exception {
        String body = """
                {"email": "%s", "password": "Password123!"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private static String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Builds a real, correctly HMAC-signed initData string exactly like Telegram's client does. */
    private static String signedInitData(long userId, String username) {
        String userJson = "{\"id\":" + userId + ",\"username\":\"" + username + "\",\"first_name\":\"Test\"}";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond()));
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
