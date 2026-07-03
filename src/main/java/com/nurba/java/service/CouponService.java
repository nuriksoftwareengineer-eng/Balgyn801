package com.nurba.java.service;

import com.nurba.java.dto.request.CouponRequest;
import com.nurba.java.dto.responce.CouponResponse;
import com.nurba.java.dto.responce.CouponValidateResponse;
import com.nurba.java.domain.Coupon;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface CouponService {
    Page<CouponResponse> list(String q, int page, int size);
    CouponResponse create(CouponRequest req);
    CouponResponse update(Long id, CouponRequest req);
    void delete(Long id);

    CouponValidateResponse validate(String code, BigDecimal orderTotal);

    Coupon findValidCoupon(String code, BigDecimal orderTotal);
    void incrementUsage(Long couponId);
}
