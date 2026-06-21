package com.nurba.java.enums;

/**
 * Internal weight-bracket tariff tables. Never exposed to the customer — the storefront only ever
 * sees a single "International Shipping" option; the AIR/POSTAL distinction is backend-only.
 */
public enum TariffKind {
    /** International air tariff (internally drives the single "International Shipping" method). */
    AIR,
    /** CIS postal tariff. */
    POSTAL
}
