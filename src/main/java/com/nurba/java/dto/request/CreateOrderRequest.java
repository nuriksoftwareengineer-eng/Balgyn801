package com.nurba.java.dto.request;

import com.nurba.java.enums.DeliveryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private DeliveryAddressRequest address;

    /**
     * Стоимость доставки (СДЭК и т.п.), добавляется к сумме товаров.
     * Для {@link DeliveryType#CDEK} обязательна положительная сумма после расчёта на витрине.
     */
    private BigDecimal deliveryFee;
}
