package com.nurba.java.dto.responce;

import com.nurba.java.enums.DeliveryType;
import com.nurba.java.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderTrackingResponse {
    private Long orderId;
    private OrderStatus orderStatus;
    private DeliveryType deliveryType;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    /** Status change audit trail, newest first. */
    private List<OrderStatusHistoryEntry> statusHistory;
    /**
     * Consolidated carrier tracking number.
     * For CDEK orders: populated from cdekShipment.trackingNumber.
     * For all carriers: populated from order.trackingNumber (set by admin).
     * Null if not yet assigned.
     */
    private String trackingNumber;
    /** CDEK shipment details — null if no CDEK shipment was created. */
    private CdekShipmentResponse cdekShipment;
}
