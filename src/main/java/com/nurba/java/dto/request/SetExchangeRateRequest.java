package com.nurba.java.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Admin request to set the KZT-per-USD rate manually. */
public record SetExchangeRateRequest(
        @NotNull(message = "Укажите курс")
        @DecimalMin(value = "0.0", inclusive = false, message = "Курс должен быть больше 0")
        BigDecimal kztPerUsd,

        /** When true, the scheduled updater will not overwrite this value. */
        Boolean frozen
) {
}
