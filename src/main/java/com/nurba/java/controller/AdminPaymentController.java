package com.nurba.java.controller;

import com.nurba.java.api.AdminPaymentApi;
import com.nurba.java.domain.Payment;
import com.nurba.java.dto.responce.PageResponse;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import com.nurba.java.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController implements AdminPaymentApi {

    private final PaymentRepository paymentRepository;

    @Override
    public List<PaymentResponse> list(PaymentProvider provider, PaymentStatus status) {
        List<Payment> payments;
        if (provider != null && status != null) {
            payments = paymentRepository.findByProviderAndStatusOrderByCreatedAtDesc(provider, status);
        } else if (provider != null) {
            payments = paymentRepository.findByProviderOrderByCreatedAtDesc(provider);
        } else if (status != null) {
            payments = paymentRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            payments = paymentRepository.findAllByOrderByCreatedAtDesc();
        }
        return payments.stream().map(AdminPaymentController::toResponse).toList();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<PaymentResponse> search(
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        var pageable = PageRequest.of(page, size);
        var result = paymentRepository.searchAdmin(provider, status, q.isBlank() ? null : q, pageable);
        return PageResponse.of(result.map(AdminPaymentController::toResponse));
    }

    private static PaymentResponse toResponse(Payment p) {
        BigDecimal amount = p.getAmount() == null
                ? BigDecimal.ZERO
                : p.getAmount().setScale(2, RoundingMode.HALF_UP);
        return new PaymentResponse(
                p.getId(),
                p.getOrder() != null ? p.getOrder().getId() : null,
                p.getProvider(),
                p.getStatus(),
                amount,
                p.getCurrency(),
                p.getProviderPaymentId(),
                p.getPaymentUrl(),
                p.getWebhookEventId(),
                p.getErrorMessage(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                null
        );
    }
}
