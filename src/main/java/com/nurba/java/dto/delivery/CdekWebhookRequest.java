package com.nurba.java.dto.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Вебхук CDEK API v2 (тип ORDER_STATUS и др.). Минимально необходимые поля:
 * {@code uuid} — UUID заказа в СДЭК, {@code attributes.code} — код статуса,
 * {@code attributes.cdek_number} — трек-номер. Неизвестные поля игнорируются.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CdekWebhookRequest(
        String type,
        String uuid,
        Attributes attributes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Attributes(
            @JsonProperty("cdek_number") String cdekNumber,
            String code,
            @JsonProperty("status_code") String statusCode,
            @JsonProperty("status_date_time") String statusDateTime
    ) {
    }
}
