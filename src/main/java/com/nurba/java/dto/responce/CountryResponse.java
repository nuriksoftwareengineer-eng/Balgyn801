package com.nurba.java.dto.responce;

/**
 * Public, customer-facing country option.
 * <p>
 * Intentionally omits the shipping zone — the customer must never see zone or carrier internals.
 */
public record CountryResponse(
        String iso2,
        String nameRu,
        String nameEn
) {
}
