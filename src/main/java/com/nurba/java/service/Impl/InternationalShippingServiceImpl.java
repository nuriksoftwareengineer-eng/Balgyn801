package com.nurba.java.service.Impl;

import com.nurba.java.domain.Country;
import com.nurba.java.domain.IntlZoneTariff;
import com.nurba.java.enums.IntlShipKind;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.repositories.IntlZoneTariffRepository;
import com.nurba.java.service.CountryService;
import com.nurba.java.service.ExchangeRateService;
import com.nurba.java.service.delivery.InternationalShippingQuote;
import com.nurba.java.service.delivery.InternationalShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * страна → тарифная зона (countries.intl_zone) → цена (intl_zone_tariffs, Авиа/Наземная).
 * Цены берутся ТОЛЬКО из импортированных таблиц тарифов — никаких захардкоженных значений
 * и никакой зависимости от веса.
 */
@Service
@RequiredArgsConstructor
public class InternationalShippingServiceImpl implements InternationalShippingService {

    private final CountryService countryService;
    private final IntlZoneTariffRepository tariffRepository;
    private final ExchangeRateService exchangeRateService;

    @Override
    public InternationalShippingQuote quote(String countryIso2, IntlShipKind kind) {
        if (kind == null) {
            throw new BusinessRuleException("Выберите тип международной доставки: Авиа или Наземная");
        }
        Country country = countryService.requireActiveByIso2(countryIso2);
        String zone = country.getIntlZone();
        if (zone == null || zone.isBlank()) {
            throw new BusinessRuleException(
                    "Доставка в страну " + country.getNameRu() + " пока недоступна — тарифы не заданы");
        }
        IntlZoneTariff tariff = tariffRepository.findByZoneAndKind(zone, kind)
                .orElseThrow(() -> new BusinessRuleException(
                        "Тариф для зоны " + zone + " (" + kind + ") не задан"));

        BigDecimal feeKzt = tariff.getPriceKzt().setScale(2, RoundingMode.HALF_UP);

        // Cached rate (never a live call) — checkout keeps working even if the provider is down.
        BigDecimal rate = exchangeRateService.kztPerUsd();
        BigDecimal feeUsd = feeKzt.divide(rate, 2, RoundingMode.HALF_UP);

        return new InternationalShippingQuote(feeKzt, feeUsd, rate.setScale(4, RoundingMode.HALF_UP));
    }
}
