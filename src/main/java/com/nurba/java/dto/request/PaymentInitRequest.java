package com.nurba.java.dto.request;

import com.nurba.java.enums.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentInitRequest(
        @NotNull @Positive Long orderId,
        @NotNull PaymentProvider provider,
        @Size(max = 500)
        @Pattern(regexp = "^(https?)://[\\w.-].*", message = "returnUrl должен быть валидным http/https URL")
        String returnUrl,
        @Size(max = 500)
        @Pattern(regexp = "^(https?)://[\\w.-].*", message = "cancelUrl должен быть валидным http/https URL")
        String cancelUrl
) {
}
