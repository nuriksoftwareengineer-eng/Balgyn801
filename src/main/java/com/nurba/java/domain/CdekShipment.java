package com.nurba.java.domain;

import com.nurba.java.enums.CdekShipmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cdek_shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdekShipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String cdekOrderUuid;
    private String trackingNumber;

    private Integer tariffCode;

    private String fromCity;
    private String toCity;

    private LocalDate estimatedDeliveryDate;

    @Enumerated(EnumType.STRING)
    private CdekShipmentStatus status;

    // ── CDEK shipment details (V18) ──────────────────────────────────────────────

    /** Код выбранного ПВЗ СДЭК (снапшот на отправлении). */
    @Column(name = "delivery_point_code", length = 64)
    private String deliveryPointCode;

    /** Адрес выбранного ПВЗ (человекочитаемый снапшот). */
    @Column(name = "delivery_point_address", length = 512)
    private String deliveryPointAddress;

    /** Стоимость доставки СДЭК по этому отправлению. */
    @Column(name = "delivery_price", precision = 10, scale = 2)
    private BigDecimal deliveryPrice;

    /** Режим доставки СДЭК (например "warehouse-pvz"); тариф — в {@link #tariffCode}. */
    @Column(name = "cdek_delivery_mode", length = 32)
    private String cdekDeliveryMode;

    /** Ссылка на накладную (PDF). В mock-режиме — mock-URL. */
    @Column(name = "invoice_url", length = 512)
    private String invoiceUrl;

    /** Ссылка на штрихкод/ярлык (PDF). В mock-режиме — mock-URL. */
    @Column(name = "barcode_url", length = 512)
    private String barcodeUrl;

    /** Полный сырой ответ провайдера (аудит). В mock-режиме — mock-payload. */
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}