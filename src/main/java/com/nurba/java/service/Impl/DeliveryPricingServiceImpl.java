package com.nurba.java.service.Impl;

import com.nurba.java.dto.delivery.CdekTariffRequest;
import com.nurba.java.dto.delivery.CdekTariffResponse;
import com.nurba.java.dto.request.DeliveryAddressRequest;
import com.nurba.java.enums.DeliveryType;
import com.nurba.java.enums.ShippingZone;
import com.nurba.java.enums.TariffKind;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.service.CountryService;
import com.nurba.java.service.DeliverySettingService;
import com.nurba.java.service.ShippingTariffService;
import com.nurba.java.service.delivery.CdekDeliveryService;
import com.nurba.java.service.delivery.DeliveryPricingService;
import com.nurba.java.service.delivery.DeliveryQuote;
import com.nurba.java.service.delivery.InternationalShippingQuote;
import com.nurba.java.service.delivery.InternationalShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Backend delivery pricing. Resolves the zone from the destination country, enforces which methods
 * are allowed per zone, and computes the fee. Domestic (Kazakhstan) pricing is intentionally
 * trivial today (pickup free / flat delivery) but routed through this single point so future rule
 * changes — weight, city, thresholds — touch only the relevant branch.
 */
@Service
@RequiredArgsConstructor
public class DeliveryPricingServiceImpl implements DeliveryPricingService {

    private static final int MIN_SHIPMENT_GRAMS = 500;

    private final CountryService countryService;
    private final DeliverySettingService deliverySettingService;
    private final CdekDeliveryService cdekDeliveryService;
    private final ShippingTariffService shippingTariffService;
    private final InternationalShippingService internationalShippingService;

    @Override
    public DeliveryQuote quote(DeliveryType method,
                               String countryIso2,
                               DeliveryAddressRequest address,
                               BigDecimal weightKg) {
        if (method == null) {
            throw new BusinessRuleException("Тип доставки обязателен");
        }
        BigDecimal weight = weightKg == null ? BigDecimal.ZERO : weightKg;

        // PICKUP is always domestic and free — no country required.
        if (method == DeliveryType.PICKUP) {
            return new DeliveryQuote(zero(), ShippingZone.KAZAKHSTAN, weight, null, null, null);
        }

        ShippingZone zone = resolveZone(method, countryIso2);
        assertMethodAllowed(method, zone);

        return switch (zone) {
            case KAZAKHSTAN -> kazakhstanQuote(weight);          // TAXI → flat rate
            case CIS -> cisQuote(method, address, weight);       // CDEK or POSTAL
            case INTERNATIONAL -> internationalQuote(weight);    // single "International Shipping"
        };
    }

    // ── zone resolution & method matrix ─────────────────────────────────────────

    private ShippingZone resolveZone(DeliveryType method, String countryIso2) {
        if (countryIso2 != null && !countryIso2.isBlank()) {
            return countryService.resolveZone(countryIso2);   // validates the country is active
        }
        if (method == DeliveryType.TAXI) {
            return ShippingZone.KAZAKHSTAN;                    // domestic delivery default
        }
        throw new BusinessRuleException("Укажите страну доставки");
    }

    private void assertMethodAllowed(DeliveryType method, ShippingZone zone) {
        if (!allowedMethods(zone).contains(method)) {
            throw new BusinessRuleException("Способ доставки недоступен для выбранной страны");
        }
    }

    /** The zone→method matrix is backend-authoritative; the frontend can never bypass it. */
    private static Set<DeliveryType> allowedMethods(ShippingZone zone) {
        return switch (zone) {
            case KAZAKHSTAN -> EnumSet.of(DeliveryType.PICKUP, DeliveryType.TAXI);
            case CIS -> EnumSet.of(DeliveryType.CDEK, DeliveryType.POSTAL);
            case INTERNATIONAL -> EnumSet.of(DeliveryType.INTERNATIONAL);
        };
    }

    // ── per-zone pricing ────────────────────────────────────────────────────────

    private DeliveryQuote kazakhstanQuote(BigDecimal weight) {
        // Flat Kazakhstan delivery — same for every destination; no weight/distance/city pricing.
        BigDecimal fee = deliverySettingService.kzDeliveryFlatKzt().setScale(2, RoundingMode.HALF_UP);
        return new DeliveryQuote(fee, ShippingZone.KAZAKHSTAN, weight, null, null, null);
    }

    private DeliveryQuote cisQuote(DeliveryType method, DeliveryAddressRequest address, BigDecimal weight) {
        if (method == DeliveryType.POSTAL) {
            BigDecimal fee = shippingTariffService.baseFeeKzt(TariffKind.POSTAL, weight)
                    .setScale(2, RoundingMode.HALF_UP);
            return new DeliveryQuote(fee, ShippingZone.CIS, weight, null, null, null);
        }
        // CDEK: backend computes the tariff from destination city + weight.
        if (address == null || isBlank(address.getCity())) {
            throw new BusinessRuleException("Для доставки СДЭК укажите город");
        }
        int cityCode = resolveCdekCityCode(address.getCity());
        CdekTariffResponse tariff = cdekDeliveryService.calculate(
                new CdekTariffRequest(cityCode, toGrams(weight), null));
        BigDecimal fee = tariff.totalPrice().setScale(2, RoundingMode.HALF_UP);
        return new DeliveryQuote(fee, ShippingZone.CIS, weight, cityCode, null, null);
    }

    private DeliveryQuote internationalQuote(BigDecimal weight) {
        InternationalShippingQuote q = internationalShippingService.quote(weight);
        return new DeliveryQuote(
                q.feeKzt(), ShippingZone.INTERNATIONAL, weight, null, q.feeUsd(), q.kztPerUsd());
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private static int toGrams(BigDecimal weightKg) {
        int grams = weightKg.multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(grams, MIN_SHIPMENT_GRAMS);
    }

    private Integer resolveCdekCityCode(String rawCity) {
        if (isBlank(rawCity)) {
            throw new BusinessRuleException("Для СДЭК укажите город");
        }
        String cityName = rawCity.split(",")[0].trim();
        if (cityName.isBlank()) {
            throw new BusinessRuleException("Для СДЭК не удалось определить город");
        }
        var cities = cdekDeliveryService.searchCities(cityName, 10);
        return cities.stream()
                .filter(c -> c.city() != null)
                .filter(c -> c.city().trim().equalsIgnoreCase(cityName))
                .map(c -> c.code())
                .findFirst()
                .orElseGet(() -> cities.stream()
                        .filter(c -> c.city() != null)
                        .filter(c -> c.city().toLowerCase(Locale.ROOT)
                                .startsWith(cityName.toLowerCase(Locale.ROOT)))
                        .map(c -> c.code())
                        .findFirst()
                        .orElseThrow(() -> new BusinessRuleException(
                                "Город СДЭК не найден по адресу: " + cityName)));
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
