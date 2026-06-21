package com.nurba.java.api;

import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.PaymentProvider;
import com.nurba.java.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Admin / Payments")
@RequestMapping("/api/v1/admin/payments")
public interface AdminPaymentApi {

    @Operation(summary = "List all payments, newest first. Optionally filter by provider and/or status.")
    @GetMapping
    List<PaymentResponse> list(
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) PaymentStatus status
    );
}
