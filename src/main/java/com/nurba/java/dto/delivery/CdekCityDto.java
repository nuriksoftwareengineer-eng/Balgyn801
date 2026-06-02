package com.nurba.java.dto.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Город из справочника СДЭК {@code GET /v2/location/cities}.
 * Нам достаточно кода города (используется в расчёте/ПВЗ) и человеческого названия.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CdekCityDto(
        Integer code,
        String city,
        String region,
        String country,
        String countryCode
) {
}
