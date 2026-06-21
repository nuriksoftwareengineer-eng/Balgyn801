package com.nurba.java.domain;

import com.nurba.java.enums.GarmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Admin-editable physical weight of a garment type, in kilograms.
 * <p>
 * Keyed on the {@link GarmentType} itself — weight is a property of the garment type, not of any
 * specific design, so one row per type applies to every design that uses it. This is the
 * authoritative source for order-weight calculation; the enum's {@code defaultWeightKg} is only a
 * fallback when a row is absent.
 */
@Entity
@Table(name = "garment_type_weights")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GarmentTypeWeight {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "garment_type", length = 30, nullable = false)
    private GarmentType garmentType;

    @Column(name = "weight_kg", precision = 6, scale = 3, nullable = false)
    private BigDecimal weightKg;
}
