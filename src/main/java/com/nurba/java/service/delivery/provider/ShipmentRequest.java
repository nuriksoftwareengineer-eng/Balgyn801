package com.nurba.java.service.delivery.provider;

import java.math.BigDecimal;
import java.util.List;

/**
 * Запрос на создание отправления, не зависящий от JPA-сущностей.
 * Бизнес-логика собирает его из заказа и адреса, провайдер (mock/real) исполняет.
 *
 * @param orderId          наш внутренний id заказа
 * @param recipientName    ФИО получателя
 * @param recipientPhone   телефон получателя (любой формат — провайдер нормализует)
 * @param recipientEmail   e-mail получателя (опционально)
 * @param toCity           город получателя (текст, снапшот)
 * @param toCityCode       код города СДЭК (если известен)
 * @param pvzCode          код ПВЗ СДЭК (если выбран пункт выдачи)
 * @param pvzAddress       адрес ПВЗ (снапшот)
 * @param tariffCode       тарифный код (null → провайдер использует дефолт из конфига)
 * @param weightGrams      суммарный вес заказа, г (сумма весов всех позиций)
 * @param lengthCm         длина посылки, см (максимальный среди товаров)
 * @param widthCm          ширина посылки, см (максимальная среди товаров)
 * @param heightCm         высота посылки, см (сумма высот всех позиций)
 * @param deliveryPrice    стоимость доставки (снапшот заказа)
 * @param items            позиции заказа для передачи СДЭК (name, wareKey, price, weight, qty)
 */
public record ShipmentRequest(
        Long orderId,
        String recipientName,
        String recipientPhone,
        String recipientEmail,
        String toCity,
        Integer toCityCode,
        String pvzCode,
        String pvzAddress,
        Integer tariffCode,
        int weightGrams,
        int lengthCm,
        int widthCm,
        int heightCm,
        BigDecimal deliveryPrice,
        List<OrderItemSnapshot> items
) {
}
