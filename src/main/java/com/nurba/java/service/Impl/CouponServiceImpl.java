package com.nurba.java.service.Impl;

import com.nurba.java.domain.Coupon;
import com.nurba.java.dto.request.CouponRequest;
import com.nurba.java.dto.responce.CouponResponse;
import com.nurba.java.dto.responce.CouponValidateResponse;
import com.nurba.java.enums.DiscountType;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.repositories.CouponRepository;
import com.nurba.java.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponse> list(String q, int page, int size) {
        return couponRepository.search(q, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public CouponResponse create(CouponRequest req) {
        if (couponRepository.findByCodeIgnoreCase(req.code()).isPresent()) {
            throw new BusinessRuleException("Промокод с таким кодом уже существует");
        }
        Coupon c = new Coupon();
        applyRequest(c, req);
        return toResponse(couponRepository.save(c));
    }

    @Override
    @Transactional
    public CouponResponse update(Long id, CouponRequest req) {
        Coupon c = findById(id);
        if (couponRepository.findByCodeIgnoreCaseAndIdNot(req.code(), id).isPresent()) {
            throw new BusinessRuleException("Промокод с таким кодом уже существует");
        }
        applyRequest(c, req);
        return toResponse(couponRepository.save(c));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        couponRepository.delete(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidateResponse validate(String code, BigDecimal orderTotal) {
        Coupon c = findValidCoupon(code, orderTotal);
        BigDecimal discount = computeDiscount(c, orderTotal);
        BigDecimal finalTotal = orderTotal.subtract(discount).max(BigDecimal.ZERO);
        return new CouponValidateResponse(c.getCode(), c.getDiscountType(), c.getDiscountValue(), discount, finalTotal);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon findValidCoupon(String code, BigDecimal orderTotal) {
        Coupon c = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessRuleException("Промокод не найден"));
        if (!c.isActive()) throw new BusinessRuleException("Промокод неактивен");
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Срок действия промокода истёк");
        }
        if (c.getMaxUses() != null && c.getUsedCount() >= c.getMaxUses()) {
            throw new BusinessRuleException("Лимит использования промокода исчерпан");
        }
        if (orderTotal.compareTo(c.getMinOrderAmount()) < 0) {
            throw new BusinessRuleException(
                    "Минимальная сумма заказа для этого промокода: " + c.getMinOrderAmount() + " ₸");
        }
        return c;
    }

    @Override
    @Transactional
    public void incrementUsage(Long couponId) {
        int updated = couponRepository.incrementUsageIfAllowed(couponId);
        if (updated == 0) {
            throw new BusinessRuleException("Лимит использования промокода исчерпан");
        }
    }

    private BigDecimal computeDiscount(Coupon c, BigDecimal total) {
        if (c.getDiscountType() == DiscountType.PERCENTAGE) {
            return total.multiply(c.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return c.getDiscountValue().min(total);
    }

    private Coupon findById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Промокод не найден: " + id));
    }

    private void applyRequest(Coupon c, CouponRequest req) {
        c.setCode(req.code().toUpperCase());
        c.setDiscountType(req.discountType());
        c.setDiscountValue(req.discountValue());
        c.setMinOrderAmount(req.minOrderAmount() != null ? req.minOrderAmount() : BigDecimal.ZERO);
        c.setMaxUses(req.maxUses());
        c.setActive(req.active());
        c.setExpiresAt(req.expiresAt());
    }

    private CouponResponse toResponse(Coupon c) {
        return new CouponResponse(
                c.getId(), c.getCode(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinOrderAmount(), c.getMaxUses(), c.getUsedCount(), c.isActive(),
                c.getExpiresAt(), c.getCreatedAt()
        );
    }
}
