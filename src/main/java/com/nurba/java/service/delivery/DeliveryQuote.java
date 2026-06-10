package com.nurba.java.service.delivery;

import com.nurba.java.enums.ShippingZone;

import java.math.BigDecimal;

/**
 * Backend-computed delivery quote — the single source of truth for what gets added to an order.
 * All values are derived server-side; nothing here originates from the client.
 *
 * @param feeKzt              delivery fee in KZT added to the order total
 * @param zone               resolved shipping zone (snapshotted onto the order)
 * @param weightKg           total order weight used for the calculation
 * @param cdekCityCode       resolved CDEK city code, if applicable (else null)
 * @param feeUsd             USD component for international shipping (else null)
 * @param exchangeRateKztUsd KZT→USD rate used for international shipping (else null)
 */
public record DeliveryQuote(
        BigDecimal feeKzt,
        ShippingZone zone,
        BigDecimal weightKg,
        Integer cdekCityCode,
        BigDecimal feeUsd,
        BigDecimal exchangeRateKztUsd
) {
}
