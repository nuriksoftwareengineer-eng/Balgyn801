package com.nurba.java.dto.responce;

import com.nurba.java.enums.Currency;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DesignGarmentPriceResponse {
    private Long id;
    private Long designGarmentId;
    private Currency currency;
    private BigDecimal amount;
}
