package com.nurba.java.dto.delivery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Запрос на расчёт стоимости/срока доставки СДЭК.
 * Минимум полей для витрины: куда и сколько весит посылка.
 *
 * @param toCityCode   код города получателя из справочника СДЭК
 * @param weightGrams  суммарный вес заказа, граммы
 * @param tariffCode   код тарифа из доки СДЭК (необязательный — если null, возьмём дефолт)
 * @param countryIso2  ISO2-код страны получателя (необязательный) — определяет надбавку
 *                     к стоимости доставки (RU +10%, KZ +3%), см. CdekDeliveryService
 */
public record CdekTariffRequest(
        @NotNull @Positive Integer toCityCode,
        @NotNull @Positive Integer weightGrams,
        Integer tariffCode,
        @Pattern(regexp = "[A-Za-z]{2}", message = "countryIso2 должен быть двухбуквенным ISO-кодом страны")
        String countryIso2
) {
}
