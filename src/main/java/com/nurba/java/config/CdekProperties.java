package com.nurba.java.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки клиента СДЭК API v2.
 *
 * <p>Базовые URL:
 * <ul>
 *   <li>sandbox: {@code https://api.edu.cdek.ru/v2}</li>
 *   <li>production: {@code https://api.cdek.ru/v2}</li>
 * </ul>
 *
 * <p>При пустых {@code clientId}/{@code clientSecret} клиент работает в «stub»-режиме:
 * вместо реальных вызовов отдаёт мок-данные. Это позволяет разрабатывать витрину/админку
 * до получения тестовых ключей у СДЭК.
 *
 * @param baseUrl       базовый URL API
 * @param clientId      OAuth client_id (он же account в кабинете СДЭК)
 * @param clientSecret  OAuth client_secret (он же secure_password)
 * @param senderCity    код города отправителя из справочника СДЭК (используется в расчёте тарифа)
 * @param defaultTariff тарифный код по умолчанию (например 136 — склад-склад)
 */
@ConfigurationProperties(prefix = "cdek")
public record CdekProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        Integer senderCity,
        Integer defaultTariff
) {
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
