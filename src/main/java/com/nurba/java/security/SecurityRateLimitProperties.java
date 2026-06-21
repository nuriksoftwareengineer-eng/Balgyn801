package com.nurba.java.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Лимиты запросов (в минуту, на один IP) для чувствительных публичных endpoint'ов:
 * вход, регистрация и публичная отправка кастом-дизайна. Применяются
 * {@link SensitiveEndpointRateLimiterFilter}.
 */
@ConfigurationProperties("app.security.rate-limit")
@Data
public class SecurityRateLimitProperties {

    /** Лимит для POST /api/v1/auth/login и POST /api/v1/auth/register. */
    private int authPerMinute = 10;

    /** Лимит для POST /api/v1/custom-design. */
    private int customDesignPerMinute = 5;

    /** Лимит для POST /api/v1/order — анти-спам создания заказов. */
    private int orderPerMinute = 15;

    /** Лимит для POST /api/v1/media/upload (ADMIN). */
    private int uploadPerMinute = 5;

    /**
     * false (дефолт): X-Forwarded-For игнорируется, используется remoteAddr.
     *                 Безопасен без обратного прокси — клиент не может подменить IP.
     * true: берётся самый правый IP из XFF — добавленный доверенным прокси (nginx/Cloudflare).
     *       Включать только при наличии реального прокси перед приложением.
     */
    private boolean trustProxy = false;
}
