package com.nurba.java.api;

import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Payment", description = "Инициализация оплаты через Freedom Pay")
@RequestMapping("/api/v1/payments")
public interface PaymentApi {

    @Operation(summary = "Инициализировать оплату для заказа (Freedom Pay)")
    @PostMapping("/init")
    PaymentResponse initPayment(@Valid @RequestBody PaymentInitRequest request);
}
