package com.nurba.java.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayPalCreateOrderRequest(
        Long orderId,
        String returnUrl,
        String cancelUrl
) {}
