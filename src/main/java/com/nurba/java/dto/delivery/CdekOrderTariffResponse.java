package com.nurba.java.dto.delivery;

import java.math.BigDecimal;

/**
 * Расчёт СДЭК по корзине с прозрачной разбивкой суммы заказа.
 *
 * @param deliveryPrice         стоимость доставки СДЭК
 * @param itemsTotal            сумма товаров (без доставки)
 * @param orderTotal            итог заказа (товары + доставка)
 * @param estimatedWeightGrams  вес, с которым был рассчитан тариф
 * @param currency              код валюты
 * @param minDays               минимальный срок доставки, дней
 * @param maxDays               максимальный срок доставки, дней
 * @param tariffCode            использованный тариф
 * @param sourcedFromStub       true, если расчёт сделан заглушкой
 */
public record CdekOrderTariffResponse(
        BigDecimal deliveryPrice,
        BigDecimal itemsTotal,
        BigDecimal orderTotal,
        Integer estimatedWeightGrams,
        String currency,
        Integer minDays,
        Integer maxDays,
        Integer tariffCode,
        boolean sourcedFromStub
) {
}
