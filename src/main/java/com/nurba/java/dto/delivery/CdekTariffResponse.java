package com.nurba.java.dto.delivery;

import java.math.BigDecimal;

/**
 * Нормализованный ответ расчёта тарифа СДЭК для фронта.
 * Минимум: цена, валюта, срок «от-до» в днях.
 *
 * @param totalPrice           итоговая стоимость доставки
 * @param currency             код валюты (KZT/RUB и т.п.)
 * @param minDays              минимальный срок доставки, дней
 * @param maxDays              максимальный срок доставки, дней
 * @param tariffCode           использованный тарифный код
 * @param sourcedFromStub      true, если данные сгенерированы локальной заглушкой (нет ключей СДЭК)
 */
public record CdekTariffResponse(
        BigDecimal totalPrice,
        String currency,
        Integer minDays,
        Integer maxDays,
        Integer tariffCode,
        boolean sourcedFromStub
) {
}
