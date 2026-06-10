package com.nurba.java.dto.responce;

import java.math.BigDecimal;

/** Admin view of editable delivery settings. */
public record DeliverySettingsResponse(
        BigDecimal kzDeliveryFlatKzt
) {
}
