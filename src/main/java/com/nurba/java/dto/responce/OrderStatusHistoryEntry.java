package com.nurba.java.dto.responce;

import com.nurba.java.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryEntry {
    private OrderStatus status;
    private Date occurredAt;
}
