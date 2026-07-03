package com.nurba.java.dto.request;

import com.nurba.java.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
        @NotBlank String code,
        @NotNull DiscountType discountType,
        @NotNull @Positive BigDecimal discountValue,
        @DecimalMin("0") BigDecimal minOrderAmount,
        Integer maxUses,
        boolean active,
        LocalDateTime expiresAt
) {}
