package com.nurba.java.dto.delivery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Позиция корзины для расчёта доставки СДЭК на бэкенде.
 */
public record CdekOrderItemRequest(
        @NotNull @Positive Long productId,
        @NotNull @Positive Integer quantity
) {
}
