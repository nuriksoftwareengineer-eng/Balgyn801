package com.nurba.java.dto.request;

import com.nurba.java.enums.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDesignGarmentPriceRequest {

    @NotNull
    private Long designGarmentId;

    @NotNull
    private Currency currency;

    @NotNull
    @Positive
    private BigDecimal amount;
}
