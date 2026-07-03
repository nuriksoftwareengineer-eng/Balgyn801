package com.nurba.java.api;

import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.PaymentProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Payment", description = "Инициализация и управление оплатой (все провайдеры)")
@RequestMapping("/api/v1/payments")
public interface PaymentApi {

    @Operation(summary = "Инициализировать оплату — передать provider=FREEDOM_PAY|PAYPAL|VTB_KZ")
    @PostMapping("/init")
    PaymentResponse initPayment(@Valid @RequestBody PaymentInitRequest request);

    @Operation(summary = "Подтвердить (capture) ранее созданный платёж (используется для PayPal)")
    @PostMapping("/capture/{providerPaymentId}")
    PaymentResponse capturePayment(
            @RequestParam PaymentProvider provider,
            @PathVariable String providerPaymentId);
}
