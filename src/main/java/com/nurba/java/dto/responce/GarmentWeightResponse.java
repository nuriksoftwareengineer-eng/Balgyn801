package com.nurba.java.dto.responce;

import com.nurba.java.enums.GarmentType;

import java.math.BigDecimal;

/** Admin view of a garment type's configured weight (kg). */
public record GarmentWeightResponse(
        GarmentType garmentType,
        BigDecimal weightKg
) {
}
