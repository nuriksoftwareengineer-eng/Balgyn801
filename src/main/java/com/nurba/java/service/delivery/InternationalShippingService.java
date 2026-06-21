package com.nurba.java.service.delivery;

import java.math.BigDecimal;

/**
 * Computes international shipping cost. Exposed to the customer as a single "International Shipping"
 * method; internally it always uses the AIR tariff plus a fixed USD markup converted at the cached
 * rate. Air/zone/carrier terminology never leaves this layer.
 */
public interface InternationalShippingService {

    /** @param weightKg backend-computed order weight */
    InternationalShippingQuote quote(BigDecimal weightKg);
}
