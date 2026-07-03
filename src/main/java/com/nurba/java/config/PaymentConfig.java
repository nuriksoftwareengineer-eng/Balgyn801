package com.nurba.java.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// VtbProperties is in the same package — no extra import needed

@Configuration
@EnableConfigurationProperties({PaymentWebhookProperties.class, FreedomPayProperties.class, PayPalProperties.class, VtbProperties.class})
public class PaymentConfig {
}
