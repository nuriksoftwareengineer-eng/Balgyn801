package com.nurba.java.dto.request;

import com.nurba.java.enums.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    @Size(max = 200)
    private String customerName;
    @Size(max = 50)
    private String customerPhone;
    @Size(max = 100)
    private String telegramUsername;
    @NotNull
    private DeliveryType deliveryType;
    @Size(max = 2000)
    private String comment;
    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    /**
     * Inline delivery address for anonymous checkout or one-off addresses.
     * Mutually exclusive with {@link #userAddressId}.
     */
    private DeliveryAddressRequest address;

    /**
     * ID of a saved {@code UserAddress} to use as the delivery address.
     * The fields are copied into an immutable {@code DeliveryAddress} snapshot at order creation time;
     * no FK is created between the snapshot and the saved address.
     * Mutually exclusive with {@link #address}.
     * Only the authenticated owner of the saved address may use it.
     */
    private Long userAddressId;

    /**
     * ISO2 код страны доставки (например "KZ", "RU", "US"). Обязателен для всех способов, кроме
     * самовывоза. Бэкенд по нему определяет зону, допустимые способы и стоимость — фронт никогда
     * не передаёт стоимость доставки, вес, зону или курс.
     */
    @Pattern(regexp = "[A-Za-z]{2}", message = "countryIso2 должен быть двухбуквенным ISO-кодом страны")
    private String countryIso2;

    /** Тип международной перевозки (обязателен при deliveryType=INTERNATIONAL): AIR или GROUND. */
    private com.nurba.java.enums.IntlShipKind intlShippingKind;

    /** Код ПВЗ СДЭК (если выбран пункт выдачи). Сохраняется в снапшот адреса. */
    @Size(max = 50)
    private String pvzCode;

    /** Промокод (опционально). Проверяется и применяется бэкендом. */
    @Size(max = 50)
    private String couponCode;
}
