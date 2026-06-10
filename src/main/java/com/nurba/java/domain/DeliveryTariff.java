package com.nurba.java.domain;

import com.nurba.java.enums.TariffKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Admin-editable weight-bracket tariff. A bracket applies to shipments up to {@link #uptoKg}
 * kilograms (inclusive); the lookup picks the smallest matching bracket for a given weight.
 */
@Entity
@Table(name = "delivery_tariffs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 16, nullable = false)
    private TariffKind kind;

    @Column(name = "upto_kg", precision = 7, scale = 3, nullable = false)
    private BigDecimal uptoKg;

    @Column(name = "base_kzt", precision = 12, scale = 2, nullable = false)
    private BigDecimal baseKzt;
}
