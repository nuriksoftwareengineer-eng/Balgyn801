package com.nurba.java.service.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.config.CdekProperties;
import com.nurba.java.dto.delivery.CdekCityDto;
import com.nurba.java.dto.delivery.CdekDeliveryPointDto;
import com.nurba.java.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Низкоуровневый клиент СДЭК API v2.
 *
 * <p>Знает только про HTTP, OAuth-кеш и сериализацию JSON. Бизнес-логику (выбор тарифа,
 * расчёт веса заказа, маппинг в наши DTO) делает {@link CdekDeliveryService}.
 *
 * <p>Если {@link CdekProperties#isConfigured()} == false — клиент считается «не настроенным»,
 * {@link #ensureConfigured()} кидает {@link BusinessRuleException} и сервис должен заранее
 * проверить флаг и подставить заглушку.
 */
@Slf4j
@Component
public class CdekClient {

    private final CdekProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient http;
    private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();

    @Autowired
    public CdekClient(CdekProperties props, ObjectMapper objectMapper) {
        this(props, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    CdekClient(CdekProperties props, ObjectMapper objectMapper, HttpClient http) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.http = http;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    /** Точно идентичный {@link CdekProperties#defaultTariff()} для удобства сервису. */
    public Integer defaultTariff() {
        return props.defaultTariff();
    }

    public Integer senderCity() {
        return props.senderCity();
    }

    /** Поиск города по подстроке. Возвращает топ-N результатов. */
    public List<CdekCityDto> searchCities(String query, int limit) {
        ensureConfigured();
        URI uri = UriComponentsBuilder.fromUriString(baseUrl() + "/location/cities")
                .queryParam("city", query)
                .queryParam("size", limit)
                .build(true)
                .toUri();
        return getJsonArray(uri, CdekCityDto.class);
    }

    /** Список ПВЗ/постаматов в указанном городе. */
    public List<CdekDeliveryPointDto> deliveryPoints(int cityCode) {
        ensureConfigured();
        URI uri = UriComponentsBuilder.fromUriString(baseUrl() + "/deliverypoints")
                .queryParam("city_code", cityCode)
                .build(true)
                .toUri();
        return getJsonArray(uri, CdekDeliveryPointDto.class);
    }

    /**
     * Расчёт тарифа: {@code POST /v2/calculator/tariff}. Тело — упрощённое (точка отправления —
     * наш склад, точка получения — город получателя, одно «грузовое место» с указанным весом).
     */
    public CdekTariffRaw calculateTariff(int fromCityCode, int toCityCode, int tariffCode, int weightGrams) {
        ensureConfigured();
        Map<String, Object> body = new HashMap<>();
        body.put("tariff_code", tariffCode);
        body.put("from_location", Map.of("code", fromCityCode));
        body.put("to_location", Map.of("code", toCityCode));
        body.put("packages", List.of(Map.of("weight", weightGrams)));
        return postJson(URI.create(baseUrl() + "/calculator/tariff"), body, CdekTariffRaw.class);
    }

    private <T> List<T> getJsonArray(URI uri, Class<T> elementType) {
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken())
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = send(req);
        if (resp.statusCode() / 100 != 2) {
            throw new BusinessRuleException("СДЭК " + uri.getPath() + ": HTTP " + resp.statusCode());
        }
        try {
            var listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, elementType);
            return objectMapper.readValue(resp.body(), listType);
        } catch (Exception e) {
            throw new BusinessRuleException("СДЭК: невалидный JSON ответа: " + e.getMessage());
        }
    }

    private <T> T postJson(URI uri, Object body, Class<T> responseType) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BusinessRuleException("СДЭК: не удалось сериализовать запрос: " + e.getMessage());
        }
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = send(req);
        if (resp.statusCode() / 100 != 2) {
            throw new BusinessRuleException("СДЭК " + uri.getPath() + ": HTTP " + resp.statusCode());
        }
        try {
            return objectMapper.readValue(resp.body(), responseType);
        } catch (Exception e) {
            throw new BusinessRuleException("СДЭК: невалидный JSON ответа: " + e.getMessage());
        }
    }

    private HttpResponse<String> send(HttpRequest req) {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessRuleException("СДЭК недоступен: " + e.getMessage());
        }
    }

    /** Возвращает живой OAuth access_token, при необходимости обновляя его. */
    private String accessToken() {
        CachedToken cached = tokenCache.get();
        if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return cached.token();
        }
        synchronized (this) {
            cached = tokenCache.get();
            if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
                return cached.token();
            }
            CachedToken fresh = requestNewToken();
            tokenCache.set(fresh);
            return fresh.token();
        }
    }

    private CachedToken requestNewToken() {
        String form = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(props.clientId(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(props.clientSecret(), StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/oauth/token?parameters"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = send(req);
        if (resp.statusCode() / 100 != 2) {
            throw new BusinessRuleException("СДЭК OAuth: HTTP " + resp.statusCode());
        }
        try {
            CdekTokenResponse parsed = objectMapper.readValue(resp.body(), CdekTokenResponse.class);
            long ttl = parsed.expiresIn() == null ? 3300 : parsed.expiresIn();
            return new CachedToken(parsed.accessToken(), Instant.now().plusSeconds(ttl));
        } catch (Exception e) {
            throw new BusinessRuleException("СДЭК OAuth: невалидный ответ: " + e.getMessage());
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new BusinessRuleException(
                    "Интеграция СДЭК не настроена: задайте CDEK_CLIENT_ID и CDEK_CLIENT_SECRET");
        }
    }

    private String baseUrl() {
        String url = props.baseUrl();
        if (url == null || url.isBlank()) {
            throw new BusinessRuleException("СДЭК: не задан cdek.base-url");
        }
        return url.replaceAll("/+$", "");
    }

    /** Кешированный OAuth-токен с моментом протухания. */
    private record CachedToken(String token, Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CdekTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {
    }

    /**
     * «Сырой» ответ калькулятора. Документация СДЭК возвращает много полей, нам нужны цена и срок;
     * валюта зависит от настроек договора СДЭК (RU — RUB, KZ — KZT и т.д.).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CdekTariffRaw(
            @JsonProperty("delivery_sum") java.math.BigDecimal deliverySum,
            @JsonProperty("total_sum") java.math.BigDecimal totalSum,
            @JsonProperty("currency") String currency,
            @JsonProperty("period_min") Integer periodMin,
            @JsonProperty("period_max") Integer periodMax,
            @JsonProperty("tariff_code") Integer tariffCode
    ) {
    }
}
