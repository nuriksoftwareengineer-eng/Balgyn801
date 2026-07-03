package com.nurba.java.controller;

import com.nurba.java.dto.responce.CouponValidateResponse;
import com.nurba.java.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponPublicController {

    private final CouponService couponService;

    @GetMapping("/validate")
    public CouponValidateResponse validate(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal) {
        return couponService.validate(code, orderTotal);
    }
}
