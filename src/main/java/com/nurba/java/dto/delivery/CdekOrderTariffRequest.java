package com.nurba.java.dto.delivery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Запрос расчёта СДЭК по корзине:
 * бэкенд сам вычисляет сумму товаров и оценочный вес.
 *
 * @param countryIso2 ISO2-код страны получателя (необязательный) — определяет надбавку
 *                    к стоимости доставки (RU +10%, KZ +3%), см. CdekDeliveryService
 */
public record CdekOrderTariffRequest(
        @NotNull @Positive Integer toCityCode,
        @NotEmpty List<@Valid CdekOrderItemRequest> items,
        Integer tariffCode,
        @Pattern(regexp = "[A-Za-z]{2}", message = "countryIso2 должен быть двухбуквенным ISO-кодом страны")
        String countryIso2
) {
}
