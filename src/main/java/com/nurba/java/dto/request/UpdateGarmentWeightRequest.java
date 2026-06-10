package com.nurba.java.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Admin request to set the weight (kg) of a garment type. */
public record UpdateGarmentWeightRequest(
        @NotNull(message = "Укажите вес")
        @DecimalMin(value = "0.0", inclusive = false, message = "Вес должен быть больше 0")
        @DecimalMax(value = "999.999", message = "Вес слишком большой")
        BigDecimal weightKg
) {
}
