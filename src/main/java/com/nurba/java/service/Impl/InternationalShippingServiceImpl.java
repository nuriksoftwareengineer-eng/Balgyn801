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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * страна → тарифная зона (countries.intl_zone, "1".."5") → официальный тариф Казпочты
 * (intl_zone_tariffs: весовые пороги до 10 кг + надбавка за каждый доп. кг свыше 10 кг).
 * Цены только из импортированной тарифной таблицы — в коде не хардкодятся.
 */
@Service
@RequiredArgsConstructor
public class InternationalShippingServiceImpl implements InternationalShippingService {

    private final CountryService countryService;
    private final IntlZoneTariffRepository tariffRepository;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Transactional(readOnly = true)
    public InternationalShippingQuote quote(String countryIso2, IntlShipKind kind, BigDecimal weightKg) {
        if (kind == null) {
            throw new BusinessRuleException("Выберите тип международной доставки: Авиа или Наземная");
        }
        Country country = countryService.requireActiveByIso2(countryIso2);
        String zone = country.getIntlZone();
        if (zone == null || zone.isBlank()) {
            throw new BusinessRuleException(
                    "Доставка в страну " + country.getNameRu() + " пока недоступна — тарифная зона не задана");
        }

        BigDecimal weight = weightKg == null ? BigDecimal.ZERO : weightKg;
        BigDecimal feeKzt = resolvePrice(zone, kind, weight).setScale(2, RoundingMode.HALF_UP);

        // Cached rate (never a live call) — checkout keeps working even if the provider is down.
        BigDecimal rate = exchangeRateService.kztPerUsd();
        BigDecimal feeUsd = feeKzt.divide(rate, 2, RoundingMode.HALF_UP);

        return new InternationalShippingQuote(feeKzt, feeUsd, rate.setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * Тариф зоны/типа перевозки для заданного веса: первый весовой порог,
     * в который вес укладывается; если вес превышает максимальный заданный порог —
     * цена на максимальном пороге плюс надбавка за каждый дополнительный кг (округление вверх).
     */
    private BigDecimal resolvePrice(String zone, IntlShipKind kind, BigDecimal weightKg) {
        List<IntlZoneTariff> brackets =
                tariffRepository.findByZoneAndKindAndIncrementFalseOrderByUptoKgAsc(zone, kind);
        if (brackets.isEmpty()) {
            throw new BusinessRuleException("Тариф для зоны " + zone + " (" + kind + ") не задан");
        }
        for (IntlZoneTariff bracket : brackets) {
            if (weightKg.compareTo(bracket.getUptoKg()) <= 0) {
                return bracket.getPriceKzt();
            }
        }

        IntlZoneTariff maxBracket = brackets.get(brackets.size() - 1);
        IntlZoneTariff increment = tariffRepository.findByZoneAndKindAndIncrementTrue(zone, kind)
                .orElseThrow(() -> new BusinessRuleException(
                        "Надбавка за вес свыше " + maxBracket.getUptoKg() + " кг для зоны " + zone
                                + " (" + kind + ") не задана"));

        BigDecimal overKg = weightKg.subtract(maxBracket.getUptoKg());
        int extraWholeKg = overKg.setScale(0, RoundingMode.CEILING).intValueExact();
        return maxBracket.getPriceKzt().add(increment.getPriceKzt().multiply(BigDecimal.valueOf(extraWholeKg)));
    }
}
