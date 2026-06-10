package com.nurba.java.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Admin request to set the flat Kazakhstan delivery fee (KZT). */
public record SetKzDeliveryFlatRequest(
        @NotNull(message = "Укажите стоимость доставки")
        @DecimalMin(value = "0.0", message = "Стоимость не может быть отрицательной")
        BigDecimal flatKzt
) {
}
