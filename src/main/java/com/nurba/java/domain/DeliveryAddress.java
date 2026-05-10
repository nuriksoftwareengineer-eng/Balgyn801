package com.nurba.java.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "delivery_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String city;           // VARCHAR
    private String street;         // VARCHAR
    private String apartment;      // VARCHAR
    private String postalCode;     // VARCHAR (не Integer! бывает "050000")
    private String recipientName;  // VARCHAR
    private String recipientPhone; // VARCHAR
}