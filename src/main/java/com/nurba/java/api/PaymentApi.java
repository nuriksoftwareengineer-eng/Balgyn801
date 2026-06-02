package com.nurba.java.api;

import com.nurba.java.dto.request.PaymentInitRequest;
import com.nurba.java.dto.request.PaymentWebhookRequest;
import com.nurba.java.dto.responce.PaymentResponse;
import com.nurba.java.enums.PaymentProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Payment", description = "Инициализация оплаты и обработка вебхуков")
@RequestMapping("/api/v1/payments")
public interface PaymentApi {

    @Operation(summary = "Инициализировать оплату для заказа")
    @PostMapping("/init")
    PaymentResponse initPayment(@Valid @RequestBody PaymentInitRequest request);

    @Operation(summary = "Принять вебхук от провайдера оплаты")
    @PostMapping("/webhook/{provider}")
    PaymentResponse webhook(
            @PathVariable PaymentProvider provider,
            @RequestBody(required = false) PaymentWebhookRequest request
    );
}
