package com.nurba.java.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.config.PayPalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Fetches and caches PayPal OAuth2 access tokens.
 * Token is refreshed automatically 60 seconds before expiry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayPalTokenClient {

    private static final int EXPIRY_BUFFER_SECONDS = 60;

    private final PayPalProperties props;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;
    private final Object lock = new Object();

    public String getAccessToken() {
        if (props.isStubMode()) {
            return "stub-access-token";
        }
        if (cachedToken != null && Instant.now().plusSeconds(EXPIRY_BUFFER_SECONDS).isBefore(tokenExpiry)) {
            return cachedToken;
        }
        synchronized (lock) {
            if (cachedToken != null && Instant.now().plusSeconds(EXPIRY_BUFFER_SECONDS).isBefore(tokenExpiry)) {
                return cachedToken;
            }
            return fetchNewToken();
        }
    }

    private String fetchNewToken() {
        String tokenUrl = props.getBaseUrl() + "/v1/oauth2/token";
        log.info("[PayPal] Fetching OAuth token from {} (mode={})", tokenUrl, props.getMode());
        String credentials = Base64.getEncoder().encodeToString(
                (props.getClientId() + ":" + props.getClientSecret()).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .timeout(Duration.ofSeconds(15))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("[PayPal] OAuth token request failed: HTTP {} body={}",
                        response.statusCode(), response.body());
                throw new PayPalApiException("PayPal OAuth token request failed: HTTP " + response.statusCode()
                        + " body=" + response.body());
            }
            TokenResponse token = objectMapper.readValue(response.body(), TokenResponse.class);
            cachedToken = token.accessToken();
            tokenExpiry = Instant.now().plusSeconds(token.expiresIn());
            log.info("[PayPal] OAuth token acquired, expires in {}s", token.expiresIn());
            return cachedToken;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[PayPal] OAuth token transport error to {}: {}", tokenUrl, e.toString());
            throw new PayPalApiException("PayPal OAuth token request failed: " + e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {}
}
