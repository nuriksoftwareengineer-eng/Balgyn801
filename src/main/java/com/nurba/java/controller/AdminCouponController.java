package com.nurba.java.controller;

import com.nurba.java.dto.request.CouponRequest;
import com.nurba.java.dto.responce.CouponResponse;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    public PageResponse<CouponResponse> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CouponResponse> p = couponService.list(q, page, size);
        return PageResponse.of(p);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CouponRequest req) {
        return couponService.create(req);
    }

    @PutMapping("/{id}")
    public CouponResponse update(@PathVariable Long id, @Valid @RequestBody CouponRequest req) {
        return couponService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        couponService.delete(id);
    }
}
