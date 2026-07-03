package com.nurba.java.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateDesignGarmentRequest {

    @NotNull
    private Long designId;

    @NotNull(message = "Укажите тип одежды (garmentProfileId)")
    private Long garmentProfileId;

    private List<Long> colorIds = new ArrayList<>();

    private List<Long> sizeIds = new ArrayList<>();
}
