package com.nurba.java.service;

import com.nurba.java.domain.Country;
import com.nurba.java.dto.request.UpsertCountryRequest;
import com.nurba.java.dto.responce.AdminCountryResponse;
import com.nurba.java.dto.responce.CountryResponse;
import com.nurba.java.enums.ShippingZone;

import java.util.List;

/**
 * Backend-controlled country list and shipping-zone resolution.
 * <p>
 * The customer chooses a country by ISO2 only; the backend owns the zone classification and
 * decides allowed delivery methods and pricing from it.
 */
public interface CountryService {

    /** Active countries for the storefront selector (no zone exposed). */
    List<CountryResponse> listActive();

    /** All countries with zone + active flag (admin). */
    List<AdminCountryResponse> listAll();

    AdminCountryResponse create(UpsertCountryRequest request);

    AdminCountryResponse update(Long id, UpsertCountryRequest request);

    void delete(Long id);

    /**
     * Resolves the active country for a customer-supplied ISO2 code.
     *
     * @throws com.nurba.java.exception.BusinessRuleException if the country is unknown or inactive.
     */
    Country requireActiveByIso2(String iso2);

    /** Backend-only zone lookup for an active country. */
    ShippingZone resolveZone(String iso2);
}
