package com.nurba.java.service.delivery;

import com.nurba.java.dto.delivery.DeliveryMethodResponse;
import com.nurba.java.dto.request.DeliveryAddressRequest;
import com.nurba.java.enums.DeliveryType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Computes the delivery fee entirely on the backend from the destination country, chosen method,
 * and order weight. The frontend supplies only {@code countryIso2} + method + address; it never
 * supplies the fee, weight, zone, exchange rate, or which methods are allowed.
 */
public interface DeliveryPricingService {

    /**
     * @param method      chosen delivery method
     * @param countryIso2 destination country ISO2 (required for all methods except PICKUP)
     * @param address     resolved delivery address (null for PICKUP)
     * @param weightKg    backend-computed total order weight
     * @return a fully backend-derived {@link DeliveryQuote}
     * @throws com.nurba.java.exception.BusinessRuleException if the country is unknown/inactive or
     *                                                        the method is not allowed for its zone
     */
    DeliveryQuote quote(DeliveryType method, String countryIso2, DeliveryAddressRequest address, BigDecimal weightKg);

    /**
     * Returns the delivery methods available for a given destination country.
     * The frontend renders only what this list contains — no method names, labels,
     * or city restrictions are hardcoded on the client side.
     *
     * @param countryIso2 ISO 3166-1 alpha-2 country code (e.g. "KZ", "RU")
     * @return ordered list of available delivery options with display metadata
     * @throws com.nurba.java.exception.BusinessRuleException if the country is unknown or inactive
     */
    List<DeliveryMethodResponse> availableMethods(String countryIso2);
}
