package com.nurba.java.service;

import com.nurba.java.dto.responce.ExchangeRateResponse;
import com.nurba.java.dto.responce.PublicExchangeRatesResponse;

import java.math.BigDecimal;

/**
 * KZT↔USD rate used for international shipping. Checkout reads {@link #kztPerUsd()} from the cached
 * DB value only — it never calls an external API — so orders place fine when the provider is down.
 */
public interface ExchangeRateService {

    /** Current KZT per 1 USD (cached DB value, or cold-start bootstrap if absent). */
    BigDecimal kztPerUsd();

    /** Admin/diagnostic view of the cached rate. */
    ExchangeRateResponse current();

    /** Public snapshot of all KZT rates (USD, EUR, RUB) for frontend currency display. */
    PublicExchangeRatesResponse publicRates();

    /** Admin: set the rate manually and optionally freeze it against scheduled overwrites. */
    ExchangeRateResponse setManualRate(BigDecimal kztPerUsd, boolean frozen);

    /** Scheduled best-effort refresh from the provider; no-op if frozen or the provider is down. */
    void refreshFromProvider();
}
