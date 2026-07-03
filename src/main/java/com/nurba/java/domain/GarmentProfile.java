package com.nurba.java.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DB-driven garment type definition: name, physical dimensions, and weight.
 * Used by CDEK shipment creation to compute real package dimensions.
 * Adding a new garment type requires only an INSERT into garment_profiles — no code change.
 */
@Entity
@Table(name = "garment_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GarmentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Folded weight in kilograms (used for shipping weight calculation). */
    @Column(name = "weight_kg", precision = 6, scale = 3, nullable = false)
    private java.math.BigDecimal weightKg;

    /** Folded length in centimetres (used for CDEK package dimensions). */
    @Column(name = "length_cm", nullable = false)
    private Integer lengthCm;

    /** Folded width in centimetres. */
    @Column(name = "width_cm", nullable = false)
    private Integer widthCm;

    /** Folded height in centimetres (stack height when multiple items). */
    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "name_ru", length = 100)
    private String nameRu;

    @Column(name = "name_kk", length = 100)
    private String nameKk;

    @Column(name = "name_en", length = 100)
    private String nameEn;
}
