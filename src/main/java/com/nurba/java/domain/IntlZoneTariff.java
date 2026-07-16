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
 * Стоимость международной доставки за тарифную зону, тип перевозки и весовой порог.
 * Данные приходят из официальной тарифной таблицы Казпочты (импорт миграцией) —
 * в коде цены не хардкодятся.
 */
@Entity
@Table(name = "intl_zone_tariffs",
        uniqueConstraints = @UniqueConstraint(name = "uq_intl_zone_tariffs", columnNames = {"zone", "kind", "upto_kg"}))
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

    /** Верхняя граница весового порога (кг), включительно. Не используется как порог
     *  на строке-надбавке ({@link #increment} = true) — хранит сентинел 999.000. */
    @Column(name = "upto_kg", nullable = false, precision = 7, scale = 3)
    private BigDecimal uptoKg;

    @Column(name = "price_kzt", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceKzt;

    /** TRUE: priceKzt — надбавка за каждый дополнительный кг сверх максимального порога
     *  зоны/типа перевозки (строка «+1 кг» тарифной таблицы), а не цена за вес. */
    @Column(name = "is_increment", nullable = false)
    private Boolean increment = false;
}
