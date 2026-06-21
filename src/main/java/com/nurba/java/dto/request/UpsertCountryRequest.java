package com.nurba.java.dto.request;

import com.nurba.java.enums.ShippingZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin request to create or update a country. */
public record UpsertCountryRequest(
        @NotBlank(message = "Укажите ISO2-код страны")
        @Size(min = 2, max = 2, message = "ISO2-код должен состоять из 2 символов")
        String iso2,

        @NotBlank(message = "Укажите название (RU)")
        String nameRu,

        @NotBlank(message = "Укажите название (EN)")
        String nameEn,

        @NotNull(message = "Укажите зону доставки")
        ShippingZone shippingZone,

        Boolean active
) {
}
