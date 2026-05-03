package com.nurba.java.dto.request;

import com.nurba.java.enums.DeliveryType;

import java.util.List;

public class CreateOrderRequest {
    private String customerName;
    private String customerPhone;
    private String telegramUsername;
    private DeliveryType deliveryType;
    private String comment;
    private List<OrderItemRequest> items;
    private DeliveryAddressRequest address;
}
