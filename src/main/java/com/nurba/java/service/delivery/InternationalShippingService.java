package com.nurba.java.service.delivery;

import com.nurba.java.enums.IntlShipKind;

/**
 * Стоимость международной доставки: страна → тарифная зона (countries.intl_zone) →
 * цена за тип перевозки (intl_zone_tariffs). Цены только из импортированных таблиц
 * тарифов; вес в расчёте не участвует.
 */
public interface InternationalShippingService {

    /**
     * @param countryIso2 страна назначения (ISO2)
     * @param kind        Авиа или Наземная
     */
    InternationalShippingQuote quote(String countryIso2, IntlShipKind kind);
}
