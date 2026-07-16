package com.nurba.java.domain;

import com.nurba.java.enums.IntlShipKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Стоимость международной доставки за тарифную зону и тип перевозки.
 * Данные приходят из импортируемых таблиц тарифов («зона → стоимость») —
 * в коде цены не хардкодятся.
 */
@Entity
@Table(name = "intl_zone_tariffs",
        uniqueConstraints = @UniqueConstraint(name = "uq_intl_zone_tariffs", columnNames = {"zone", "kind"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntlZoneTariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone", length = 20, nullable = false)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 10, nullable = false)
    private IntlShipKind kind;

    @Column(name = "price_kzt", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceKzt;
}
