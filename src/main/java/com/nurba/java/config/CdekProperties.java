package com.nurba.java.config;

import com.nurba.java.enums.CdekProviderMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/**
 * Настройки клиента СДЭК API v2.
 *
 * <p>Базовые URL:
 * <ul>
 *   <li>sandbox: {@code https://api.edu.cdek.ru/v2}</li>
 *   <li>production: {@code https://api.cdek.ru/v2}</li>
 * </ul>
 *
 * <p>Переключение mock/real — только через {@code cdek.provider} (AUTO|MOCK|REAL):
 * <ul>
 *   <li>AUTO (по умолчанию): real при заданных {@code clientId}/{@code clientSecret}, иначе mock;</li>
 *   <li>MOCK: всегда заглушки (даже при наличии ключей);</li>
 *   <li>REAL: всегда реальный API (требует ключей).</li>
 * </ul>
 *
 * @param baseUrl       базовый URL API
 * @param clientId      OAuth client_id (account в кабинете СДЭК)
 * @param clientSecret  OAuth client_secret (secure_password)
 * @param senderCity    код города отправителя из справочника СДЭК (для расчёта тарифа)
 * @param defaultTariff тарифный код по умолчанию (например 136 — склад-склад)
 * @param provider      режим провайдера: auto|mock|real (по умолчанию auto)
 */
@ConfigurationProperties(prefix = "cdek")
public record CdekProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        Integer senderCity,
        Integer defaultTariff,
        String provider
) {
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    /** Разобранный режим провайдера; невалидное/пустое значение трактуется как AUTO. */
    public CdekProviderMode resolveMode() {
        if (provider == null || provider.isBlank()) {
            return CdekProviderMode.AUTO;
        }
        try {
            return CdekProviderMode.valueOf(provider.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CdekProviderMode.AUTO;
        }
    }

    /** Использовать ли реальный CDEK API (с учётом режима и наличия ключей). */
    public boolean useRealApi() {
        return switch (resolveMode()) {
            case REAL -> true;
            case MOCK -> false;
            case AUTO -> isConfigured();
        };
    }
}
