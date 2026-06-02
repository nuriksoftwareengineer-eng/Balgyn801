package com.nurba.java.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        /** Срок жизни access-токена (короткий; по умолчанию 15 мин). */
        long expirationMs,
        /** Срок жизни refresh-токена (длинный; по умолчанию 14 суток). */
        long refreshExpirationMs
) {}
