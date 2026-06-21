package com.nurba.java.service.delivery.provider;

import java.math.BigDecimal;

/**
 * Запрос на создание отправления, не зависящий от JPA-сущностей.
 * Бизнес-логика собирает его из заказа и адреса, провайдер (mock/real) исполняет.
 *
 * @param orderId        наш внутренний id заказа (номер у нас)
 * @param recipientName  получатель
 * @param recipientPhone телефон получателя
 * @param toCity         город получателя (текст, снапшот)
 * @param toCityCode     код города СДЭК (если известен)
 * @param pvzCode        код ПВЗ СДЭК (если выбран пункт выдачи)
 * @param pvzAddress     адрес ПВЗ (снапшот)
 * @param tariffCode     тарифный код
 * @param weightGrams    суммарный вес, г (считается на бэкенде через GarmentWeightService)
 * @param deliveryPrice  стоимость доставки (снапшот заказа)
 */
public record ShipmentRequest(
        Long orderId,
        String recipientName,
        String recipientPhone,
        String toCity,
        Integer toCityCode,
        String pvzCode,
        String pvzAddress,
        Integer tariffCode,
        int weightGrams,
        BigDecimal deliveryPrice
) {
}
