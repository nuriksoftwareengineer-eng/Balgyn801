package com.nurba.java.dto.delivery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Позиция корзины для расчёта доставки СДЭК на бэкенде.
 * Ровно одно из двух полей должно быть задано: {@code productId} (устаревший путь) или
 * {@code designGarmentId} (новый каталог). Сервис проверяет это программно.
 */
public record CdekOrderItemRequest(
        /** ID устаревшего Product. Null для позиций нового каталога. */
        @Positive Long productId,
        /** ID DesignGarment из каталога. Null для устаревших позиций. */
        @Positive Long designGarmentId,
        @NotNull @Positive Integer quantity
) {
}
