package com.nurba.java.dto.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Пункт выдачи (ПВЗ) или постамат СДЭК {@code GET /v2/deliverypoints}.
 * В оригинальном ответе ещё много полей (работающее время, фото и т.п.),
 * для checkout-страницы достаточно адреса и координат.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CdekDeliveryPointDto(
        String code,
        String name,
        String address,
        Double longitude,
        Double latitude,
        String workTime,
        String type
) {
}
