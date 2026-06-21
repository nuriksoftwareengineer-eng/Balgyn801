package com.nurba.java.enums;

/**
 * Backend-only delivery zone derived from the destination country.
 * <p>
 * Never exposed to the customer. The frontend sends only {@code countryIso2}; the backend
 * resolves the zone from the {@code countries} table and uses it to decide allowed delivery
 * methods and pricing.
 */
public enum ShippingZone {
    /** Domestic Kazakhstan: pickup (free) or flat-rate delivery. */
    KAZAKHSTAN,
    /** CIS countries: CDEK or postal delivery. */
    CIS,
    /** Everything else: a single "International Shipping" method (AIR tariff internally). */
    INTERNATIONAL
}
