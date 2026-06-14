package com.nurba.java.dto.delivery;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Пункт выдачи (ПВЗ) или постамат СДЭК {@code GET /v2/deliverypoints}.
 *
 * <p>CDEK кладёт адрес и координаты во вложенный объект {@code location} (не в корень ответа),
 * поэтому десериализация идёт через {@link #fromCdek}. Наружу отдаём плоский {@code address} —
 * полный адрес «город, улица, дом» (источник: {@code city + location.address}; у CDEK поле
 * {@code location.address} содержит улицу с номером дома/корпуса/строения). Запас —
 * {@code location.address_full}, затем {@code name}.
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
            @JsonProperty("location") Map<String, Object> location
    ) {
        String addressShort = str(location, "address");
        String addressFull = str(location, "address_full");
        String city = str(location, "city");
        // Чистый формат «город, улица, дом» — без индекса/страны и дубля региона.
        String address = firstNonBlank(join(city, addressShort), addressFull, name);
        return new CdekDeliveryPointDto(
                code, name, address,
                dbl(location, "longitude"), dbl(location, "latitude"),
                workTime, type);
    }

    private static String str(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Double dbl(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : null;
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
