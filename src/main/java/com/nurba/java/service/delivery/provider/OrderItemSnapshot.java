package com.nurba.java.service.delivery.provider;

import java.math.BigDecimal;

/**
 * Снимок позиции заказа для передачи провайдеру СДЭК.
 * Не зависит от JPA-сущностей — только примитивные/String данные.
 *
 * @param name        название (дизайн + тип изделия), попадает в items[].name СДЭК
 * @param wareKey     артикул / SKU (уникальный в рамках заказа)
 * @param price       объявленная стоимость единицы в валюте контракта (KZT)
 * @param weightGrams вес единицы, г
 * @param quantity    количество единиц
 */
public record OrderItemSnapshot(
        String name,
        String wareKey,
        BigDecimal price,
        int weightGrams,
        int quantity
) {
    /** Суммарный вес позиции (единица × количество), г. */
    public int totalWeightGrams() {
        return weightGrams * quantity;
    }
}
