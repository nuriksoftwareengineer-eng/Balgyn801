package com.nurba.java.domain;

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
    private Order order;           // FK -> orders

    private String cdekOrderUuid;  // VARCHAR (UUID из СДЭК)
    private String trackingNumber; // VARCHAR

    private Integer tariffCode;    // INTEGER (136, 137 и т.д.)

    private String fromCity;       // VARCHAR
    private String toCity;         // VARCHAR

    private LocalDate estimatedDeliveryDate; // DATE

    @Enumerated(EnumType.STRING)
    private CdekShipmentStatus status; // VARCHAR (enum)

    private LocalDateTime createdAt;  // TIMESTAMP
    private LocalDateTime updatedAt;  // TIMESTAMP
}