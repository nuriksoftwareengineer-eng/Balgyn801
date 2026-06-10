package com.nurba.java.service.delivery;

import java.math.BigDecimal;

/**
 * Result of an international shipping calculation.
 *
 * @param feeKzt     total fee in KZT added to the order (base tariff + USD markup converted)
 * @param feeUsd     total fee expressed in USD (snapshotted for audit)
 * @param kztPerUsd  exchange rate used for the conversion (snapshotted for audit)
 */
public record InternationalShippingQuote(
        BigDecimal feeKzt,
        BigDecimal feeUsd,
        BigDecimal kztPerUsd
) {
}
