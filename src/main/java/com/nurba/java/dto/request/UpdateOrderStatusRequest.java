package com.nurba.java.dto.request;

import com.nurba.java.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Статус обязателен")
    private OrderStatus status;

    /** Optional carrier tracking number. When present, saved to order.trackingNumber. */
    private String trackingNumber;
}
