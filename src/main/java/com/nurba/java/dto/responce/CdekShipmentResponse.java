package com.nurba.java.dto.responce;

import com.nurba.java.enums.CdekShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CdekShipmentResponse {
    private String trackingNumber;
    private CdekShipmentStatus status;
    private LocalDate estimatedDeliveryDate;
}
