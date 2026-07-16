package com.nurba.java.dto.delivery;

import com.nurba.java.enums.IntlShipKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Запрос стоимости международной доставки: страна + тип перевозки + позиции корзины.
 * Вес считается на бэкенде из designGarmentId — фронтенд вес не передаёт.
 */
public record IntlQuoteRequest(
        @NotBlank String countryIso2,
        @NotNull IntlShipKind kind,
        @Valid List<Item> items
) {

    public record Item(
            @NotNull Long designGarmentId,
            @NotNull @Min(1) Integer quantity
    ) {
    }
}
