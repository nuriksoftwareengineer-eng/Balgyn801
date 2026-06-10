package com.nurba.java.dto.request;

import com.nurba.java.enums.DeliveryType;
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
    private String customerName;
    private String customerPhone;
    private String telegramUsername;
    private DeliveryType deliveryType;
    private String comment;
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
    private String countryIso2;

    /** Код ПВЗ СДЭК (если выбран пункт выдачи). Сохраняется в снапшот адреса. */
    private String pvzCode;
}
