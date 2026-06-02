package com.nurba.java.dto.delivery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Запрос расчёта СДЭК по корзине:
 * бэкенд сам вычисляет сумму товаров и оценочный вес.
 */
public record CdekOrderTariffRequest(
        @NotNull @Positive Integer toCityCode,
        @NotEmpty List<@Valid CdekOrderItemRequest> items,
        Integer tariffCode
) {
}
