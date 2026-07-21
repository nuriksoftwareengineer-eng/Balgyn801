package com.nurba.java.dto.responce;

import java.math.BigDecimal;

public record GarmentProfileResponse(
        Long id,
        String name,
        BigDecimal weightKg,
        Integer lengthCm,
        Integer widthCm,
        Integer heightCm,
        Integer sortOrder,
        String nameRu,
        String nameKk,
        String nameEn,
        String materialDescription
) {
}
