package com.nurba.java.dto.responce;

import com.nurba.java.enums.DeliveryType;
import com.nurba.java.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String customerName;
    private String customerPhone;
    private OrderStatus status;
    private DeliveryType deliveryType;
    private BigDecimal totalPrice;
    /** Сумма доставки, если была передана при оформлении (иначе null). */
    private BigDecimal deliveryFee;
    private String comment;
    private List<OrderItemResponse> items;
    private DeliveryAddressResponse address;
    private CdekShipmentResponse cdekShipment;
    /** Tracking number assigned by the carrier. Null until admin sets it on shipment. */
    private String trackingNumber;
    private String couponCode;
    private BigDecimal discountAmount;
    private LocalDateTime createdAt;
}
