package com.nurba.java.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Best-effort source of the KZT-per-USD rate, used only by the scheduled updater — never by
 * checkout. Implementations must be defensive: any failure (network down, parse error, timeout)
 * returns {@link Optional#empty()} so the last known-good cached rate is preserved.
 */
public interface ExchangeRateProvider {

    /** @return current KZT per 1 USD, or empty if the source is unavailable. */
    Optional<BigDecimal> fetchKztPerUsd();
}
