package com.nurba.java.dto.responce;

import com.nurba.java.enums.CdekShipmentStatus;

import java.time.LocalDate;

public class CdekShipmentResponse {
    private String trackingNumber;
    private CdekShipmentStatus status;
    private LocalDate estimatedDeliveryDate;
}
