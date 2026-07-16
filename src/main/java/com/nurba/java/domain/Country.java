package com.nurba.java.domain;

import com.nurba.java.enums.ShippingZone;
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

/**
 * Admin-managed list of countries the store ships to.
 * <p>
 * The {@link #shippingZone} is the authoritative, backend-only classification used to determine
 * allowed delivery methods and pricing. The customer only ever selects a country by {@link #iso2};
 * the zone is never sent by, nor shown to, the frontend.
 */
@Entity
@Table(name = "countries", uniqueConstraints = @UniqueConstraint(name = "uk_countries_iso2", columnNames = "iso2"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ISO 3166-1 alpha-2 code, uppercase (e.g. "KZ", "RU", "US"). */
    @Column(name = "iso2", length = 2, nullable = false, unique = true)
    private String iso2;

    @Column(name = "name_ru", length = 100, nullable = false)
    private String nameRu;

    @Column(name = "name_en", length = 100, nullable = false)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_zone", length = 20, nullable = false)
    private ShippingZone shippingZone;

    /**
     * Тарифная зона международной доставки (таблица «страна → зона» из тарифов Казпочты).
     * NULL — тарифы для страны ещё не импортированы, международная доставка недоступна.
     */
    @Column(name = "intl_zone", length = 20)
    private String intlZone;

    @Column(nullable = false)
    private Boolean active = true;
}
