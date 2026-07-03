package com.nurba.java.dto.responce;

import com.nurba.java.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        Integer maxUses,
        int usedCount,
        boolean active,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
