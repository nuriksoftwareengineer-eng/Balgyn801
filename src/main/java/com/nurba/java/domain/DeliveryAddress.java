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

    /** Код ПВЗ СДЭК (если выбран пункт выдачи). Снапшот на момент оформления. */
    @Column(name = "pvz_code", length = 64)
    private String pvzCode;

    /** ISO2-код страны доставки (снапшот). */
    @Column(name = "country_iso2", length = 2)
    private String countryIso2;

    /** Код города СДЭК (снапшот), если применимо. */
    @Column(name = "city_code")
    private Integer cityCode;
}