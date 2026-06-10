package com.nurba.java.dto.responce;

import com.nurba.java.enums.ShippingZone;

/** Admin view of a country, including the backend-only shipping zone and active flag. */
public record AdminCountryResponse(
        Long id,
        String iso2,
        String nameRu,
        String nameEn,
        ShippingZone shippingZone,
        boolean active
) {
}
