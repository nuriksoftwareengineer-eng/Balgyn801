package com.nurba.java.dto.delivery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Запрос на расчёт стоимости/срока доставки СДЭК.
 * Минимум полей для витрины: куда и сколько весит посылка.
 *
 * @param toCityCode   код города получателя из справочника СДЭК
 * @param weightGrams  суммарный вес заказа, граммы
 * @param tariffCode   код тарифа из доки СДЭК (необязательный — если null, возьмём дефолт)
 */
public record CdekTariffRequest(
        @NotNull @Positive Integer toCityCode,
        @NotNull @Positive Integer weightGrams,
        Integer tariffCode
) {
}
