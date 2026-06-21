package com.nurba.java.service.delivery;

import com.nurba.java.config.CdekProperties;
import com.nurba.java.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Авторизация в СДЭК (OAuth client_credentials). Сам жизненный цикл токена (получение, кэш,
 * обновление) живёт в {@link CdekClient}; этот сервис — точка входа для проверки готовности
 * и прогрева токена (healthcheck/диагностика), чтобы провайдеры и админка могли спросить
 * «настроена ли интеграция» без знания деталей HTTP.
 */
@Service
@RequiredArgsConstructor
public class CdekAuthService {

    private final CdekProperties props;
    private final CdekClient client;

    /** Заданы ли credentials (CLIENT_ID/CLIENT_SECRET). */
    public boolean isConfigured() {
        return props.isConfigured();
    }

    /** Базовый URL API (sandbox/production) — задаётся только через ENV. */
    public String baseUrl() {
        return props.baseUrl();
    }

    /**
     * Прогрев OAuth-токена. Бросает {@link BusinessRuleException}, если интеграция не настроена
     * или СДЭК недоступен. Полезно для проверки ключей сразу после их подключения.
     */
    public void warmUp() {
        client.warmUpToken();
    }
}
