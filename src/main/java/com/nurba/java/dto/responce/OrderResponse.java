package com.nurba.java.dto.responce;

import com.nurba.java.enums.DeliveryType;
import com.nurba.java.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {
    private Long id;
    private String customerName;
    private String customerPhone;
    private OrderStatus status;
    private DeliveryType deliveryType;
    private BigDecimal totalPrice;
    private String comment;
    private List<OrderItemResponse> items;
    private DeliveryAddressResponse address;
    private CdekShipmentResponse cdekShipment;
    private LocalDateTime createdAt;
}
