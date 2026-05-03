package com.nurba.java.domain;

import com.nurba.java.enums.CdekShipmentStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cdek_shipments")
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}