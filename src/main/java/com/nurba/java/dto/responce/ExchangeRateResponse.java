package com.nurba.java.dto.responce;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Admin/diagnostic view of the cached KZT-per-USD rate. */
public record ExchangeRateResponse(
        BigDecimal kztPerUsd,
        String source,
        boolean frozen,
        LocalDateTime updatedAt
) {
}
