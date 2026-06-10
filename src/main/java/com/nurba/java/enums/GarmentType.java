package com.nurba.java.enums;

import java.math.BigDecimal;

/**
 * Garment variants a design can be printed on.
 * <p>
 * Each constant carries a {@code defaultWeightKg} used <strong>only</strong> as a safety-net
 * fallback. The authoritative, admin-editable weight lives in the {@code garment_type_weights}
 * table (see {@code GarmentTypeWeight}). The fallback guarantees weight calculation never crashes
 * checkout if a row is missing (e.g. a fresh DB before seeding, or an accidentally deleted row).
 */
public enum GarmentType {
    T_SHIRT(new BigDecimal("0.400")),
    OVERSIZE_TSHIRT(new BigDecimal("0.500")),
    LONGSLEEVE(new BigDecimal("0.500")),
    SWEATSHIRT(new BigDecimal("0.800")),
    HOODIE(new BigDecimal("1.000")),
    ZIP_HOODIE(new BigDecimal("1.100"));

    private final BigDecimal defaultWeightKg;

    GarmentType(BigDecimal defaultWeightKg) {
        this.defaultWeightKg = defaultWeightKg;
    }

    /** Fallback weight in kilograms, used when no {@code garment_type_weights} row exists. */
    public BigDecimal getDefaultWeightKg() {
        return defaultWeightKg;
    }
}
