package com.nurba.java.dto.delivery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Запрос стоимости международной доставки: страна + позиции корзины. Международная
 * доставка — всегда авиаперевозка (AIR), выбор типа перевозки покупателю не предлагается.
 * Вес считается на бэкенде из designGarmentId — фронтенд вес не передаёт.
 */
public record IntlQuoteRequest(
        @NotBlank String countryIso2,
        @Valid List<Item> items
) {

    public record Item(
            @NotNull Long designGarmentId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
