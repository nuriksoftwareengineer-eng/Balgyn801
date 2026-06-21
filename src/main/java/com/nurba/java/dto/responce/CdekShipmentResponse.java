package com.nurba.java.dto.responce;

import com.nurba.java.enums.CdekShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CdekShipmentResponse {
    private Long orderId;
    private String cdekOrderUuid;
    private String trackingNumber;
    private CdekShipmentStatus status;
    private LocalDate estimatedDeliveryDate;
    private Integer tariffCode;
    private String cdekDeliveryMode;
    private String deliveryPointCode;
    private String deliveryPointAddress;
    private BigDecimal deliveryPrice;
    private String invoiceUrl;
    private String barcodeUrl;
    /** true, если отправление создано mock-провайдером (для бейджа «mock» в админке). */
    private boolean mock;
}
