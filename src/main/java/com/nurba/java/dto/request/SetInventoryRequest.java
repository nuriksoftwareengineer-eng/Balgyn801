package com.nurba.java.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetInventoryRequest {

    @NotNull
    private Long designGarmentId;

    @NotNull
    private Long colorId;

    @NotNull
    private Long sizeId;

    @NotNull
    @Min(0)
    private Integer quantity;
}
