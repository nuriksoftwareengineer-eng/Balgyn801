package com.nurba.java.dto.delivery;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Пункт выдачи (ПВЗ) или постамат СДЭК {@code GET /v2/deliverypoints}.
 *
 * <p>CDEK вкладывает адрес и координаты в объект {@code location} (а не в корень ответа),
 * поэтому десериализация идёт через {@link #fromCdek} — иначе адрес/координаты приходили бы null.
 * Наружу отдаём плоскую структуру: {@code address} — максимально полный адрес «город, улица, дом».
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

    /** Десериализация реального ответа CDEK: собирает полный адрес из вложенного {@code location}. */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static CdekDeliveryPointDto fromCdek(
            @JsonProperty("code") String code,
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("work_time") String workTime,
            @JsonProperty("location") Location location
    ) {
        String address = null;
        Double longitude = null;
        Double latitude = null;
        if (location != null) {
            // «город, улица+дом» (как location.address) — максимально полный и читаемый адрес;
            // запасные варианты: полный адрес CDEK, затем name.
            address = firstNonBlank(
                    join(location.city(), location.address()),
                    location.addressFull(),
                    name);
            longitude = location.longitude();
            latitude = location.latitude();
        }
        if (isBlank(address)) {
            address = name;
        }
        return new CdekDeliveryPointDto(code, name, address, longitude, latitude, workTime, type);
    }

    /** Вложенный объект {@code location} из ответа CDEK. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Location(
            @JsonProperty("address") String address,
            @JsonProperty("address_full") String addressFull,
            @JsonProperty("city") String city,
            @JsonProperty("longitude") Double longitude,
            @JsonProperty("latitude") Double latitude
    ) {
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String join(String a, String b) {
        if (isBlank(a)) return b;
        if (isBlank(b)) return a;
        return a + ", " + b;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
