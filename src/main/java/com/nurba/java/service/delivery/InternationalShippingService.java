package com.nurba.java.service.delivery;

import java.math.BigDecimal;

/**
 * Стоимость международной доставки: страна → тарифная зона (countries.intl_zone) →
 * официальный тариф Казпочты по весовому порогу (intl_zone_tariffs). Цены только из
 * импортированной тарифной таблицы; вес — обязательный параметр расчёта.
 * <p>
 * Всегда авиаперевозка (AIR) — единственный способ международной доставки, который
 * предлагается покупателю.
 */
public interface InternationalShippingService {

    /**
     * @param countryIso2 страна назначения (ISO2)
     * @param weightKg    вес отправления, вычисленный на бэкенде (клиент вес не передаёт)
     */
    InternationalShippingQuote quote(String countryIso2, BigDecimal weightKg);
}
