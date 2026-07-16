package com.nurba.java.service.delivery;

import com.nurba.java.enums.IntlShipKind;

import java.math.BigDecimal;

/**
 * Стоимость международной доставки: страна → тарифная зона (countries.intl_zone) →
 * официальный тариф Казпочты по весовому порогу (intl_zone_tariffs). Цены только из
 * импортированной тарифной таблицы; вес — обязательный параметр расчёта.
 */
public interface InternationalShippingService {

    /**
     * @param countryIso2 страна назначения (ISO2)
     * @param kind        Авиа или Наземная
     * @param weightKg    вес отправления, вычисленный на бэкенде (клиент вес не передаёт)
     */
    InternationalShippingQuote quote(String countryIso2, IntlShipKind kind, BigDecimal weightKg);
}
