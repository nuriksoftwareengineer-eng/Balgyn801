package com.nurba.java.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Validates Freedom Pay configuration at startup in production.
 * Blank merchantId means stub mode — customers get a fake payment URL.
 * This bean prevents the application from starting with such misconfiguration.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class FreedomPayStartupValidator {

    private final FreedomPayProperties props;

    @PostConstruct
    public void validate() {
        if (props.getMerchantId() == null || props.getMerchantId().isBlank()) {
            throw new IllegalStateException(
                    "FREEDOMPAY_MERCHANT_ID must not be blank in production. " +
                    "A blank merchant ID activates stub mode — no real API calls are made " +
                    "and customers receive a fake payment URL. Set FREEDOMPAY_MERCHANT_ID in .env.prod.");
        }
        if (props.getSecretKey() == null || props.getSecretKey().isBlank()) {
            throw new IllegalStateException(
                    "FREEDOMPAY_SECRET_KEY must not be blank in production.");
        }
        if (props.getResultUrl() == null || props.getResultUrl().isBlank()) {
            throw new IllegalStateException(
                    "FREEDOMPAY_RESULT_URL must not be blank in production.");
        }
        log.info("[FreedomPay] Production config validated: merchantId={}, resultUrl={}",
                props.getMerchantId(), props.getResultUrl());
    }
}
