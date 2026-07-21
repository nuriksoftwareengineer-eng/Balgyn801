package com.nurba.java.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateGarmentProfileRequest(

        @NotBlank(message = "Укажите название типа одежды")
        @Size(max = 100)
        String name,

        @NotNull(message = "Укажите вес")
        @DecimalMin(value = "0.001", message = "Вес должен быть больше 0")
        @Digits(integer = 3, fraction = 3)
        BigDecimal weightKg,

        @NotNull(message = "Укажите длину (см)")
        @Min(value = 1, message = "Длина должна быть больше 0")
        @Max(value = 9999)
        Integer lengthCm,

        @NotNull(message = "Укажите ширину (см)")
        @Min(value = 1, message = "Ширина должна быть больше 0")
        @Max(value = 9999)
        Integer widthCm,

        @NotNull(message = "Укажите высоту (см)")
        @Min(value = 1, message = "Высота должна быть больше 0")
        @Max(value = 9999)
        Integer heightCm,

        Integer sortOrder,

        @Size(max = 100)
        String nameRu,

        @Size(max = 100)
        String nameKk,

        @Size(max = 100)
        String nameEn,

        String materialDescription
) {
}
