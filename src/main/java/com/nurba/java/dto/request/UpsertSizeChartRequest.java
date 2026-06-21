package com.nurba.java.dto.request;

import com.nurba.java.enums.GarmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertSizeChartRequest {
    @NotNull
    private GarmentType garmentType;

    @NotBlank
    private String imageUrl;

    private String title;
}
