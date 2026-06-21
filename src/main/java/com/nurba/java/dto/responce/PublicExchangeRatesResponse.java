package com.nurba.java.dto.responce;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Public (no-auth) snapshot of KZT exchange rates for frontend price display. */
public record PublicExchangeRatesResponse(
        BigDecimal kztPerUsd,
        BigDecimal kztPerEur,
        BigDecimal kztPerRub,
        LocalDateTime updatedAt
) {
}
