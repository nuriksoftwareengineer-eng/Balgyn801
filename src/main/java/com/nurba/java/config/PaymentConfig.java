package com.nurba.java.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PaymentWebhookProperties.class, FreedomPayProperties.class, PayPalProperties.class})
public class PaymentConfig {
}
