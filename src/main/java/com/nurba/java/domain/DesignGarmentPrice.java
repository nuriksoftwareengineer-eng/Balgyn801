package com.nurba.java.domain;

import com.nurba.java.enums.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "design_garment_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignGarmentPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_garment_id", nullable = false)
    private DesignGarment designGarment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
