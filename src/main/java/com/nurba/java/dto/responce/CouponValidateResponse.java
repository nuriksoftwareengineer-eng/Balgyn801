package com.nurba.java.dto.responce;

import com.nurba.java.enums.DiscountType;

import java.math.BigDecimal;

public record CouponValidateResponse(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal finalTotal
) {}
