package com.nurba.java.dto.delivery;

import com.nurba.java.enums.DeliveryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One available delivery option returned by GET /api/v1/delivery/methods.
 * Frontend renders only what this response contains — no delivery logic or city names are hardcoded client-side.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryMethodResponse {

    /** Delivery type enum value — sent back as-is in CreateOrderRequest. */
    private DeliveryType type;

    /** Russian display label for storefront UI. */
    private String labelRu;

    /** Whether a delivery address is required for this method. False only for PICKUP. */
    private boolean requiresAddress;

    /** Whether the CDEK city autocomplete widget should be shown. */
    private boolean requiresCitySearch;

    /** Whether a CDEK PVZ (pickup point) selector should be shown. */
    private boolean requiresPvz;

    /**
     * If non-null, this method is restricted to the named city.
     * Frontend displays: "Доступно только в: {cityRestriction}".
     * Never hardcode city names in the frontend.
     */
    private String cityRestriction;

    /**
     * Flat estimated fee in KZT, or null if the fee depends on weight/destination (CDEK, POSTAL, INTERNATIONAL).
     * 0 means free (PICKUP).
     */
    private BigDecimal estimatedFeeKzt;
}
