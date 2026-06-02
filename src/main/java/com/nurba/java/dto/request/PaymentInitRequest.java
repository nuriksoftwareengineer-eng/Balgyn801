package com.nurba.java.dto.request;

import com.nurba.java.enums.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentInitRequest(
        @NotNull @Positive Long orderId,
        @NotNull PaymentProvider provider,
        String returnUrl
) {
}
