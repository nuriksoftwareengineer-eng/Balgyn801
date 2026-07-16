package com.nurba.java.service.Impl;

import com.nurba.java.dto.delivery.CdekTariffRequest;
import com.nurba.java.dto.delivery.CdekTariffResponse;
import com.nurba.java.dto.delivery.DeliveryMethodResponse;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Backend delivery pricing. Resolves the zone from the destination country, enforces which methods
 * are allowed per zone, and computes the fee.
 *
 * <p><b>Zone → method matrix (authoritative):</b>
 * <ul>
 *   <li>KAZAKHSTAN → PICKUP, TAXI (Almaty only), POSTAL (Kazpost), CDEK</li>
 *   <li>CIS → CDEK only</li>
 *   <li>INTERNATIONAL → INTERNATIONAL only</li>
 * </ul>
 *
 * <p>PICKUP is available to any KZ customer ("pickup in Almaty by agreement") — no city restriction.
 * TAXI requires the delivery city to be Almaty.
 */
@Service
@RequiredArgsConstructor
public class DeliveryPricingServiceImpl implements DeliveryPricingService {

    private static final int MIN_SHIPMENT_GRAMS = 500;

    /** City name used for TAXI restriction. Defined here so it surfaces in the API response — never hardcoded client-side. */
    private static final String TAXI_CITY = "Алматы";

    private final CountryService countryService;
    private final DeliverySettingService deliverySettingService;
    private final CdekDeliveryService cdekDeliveryService;
    private final ShippingTariffService shippingTariffService;
    private final InternationalShippingService internationalShippingService;

    // ── quote ────────────────────────────────────────────────────────────────────

    @Override
    public DeliveryQuote quote(DeliveryType method,
                               String countryIso2,
                               DeliveryAddressRequest address,
                               BigDecimal weightKg,
                               com.nurba.java.enums.IntlShipKind intlKind) {
        if (method == null) {
            throw new BusinessRuleException("Тип доставки обязателен");
        }
        BigDecimal weight = weightKg == null ? BigDecimal.ZERO : weightKg;

        // PICKUP is always domestic and free — no country required, no city restriction.
        if (method == DeliveryType.PICKUP) {
            return new DeliveryQuote(zero(), ShippingZone.KAZAKHSTAN, weight, null, null, null);
        }

        ShippingZone zone = resolveZone(method, countryIso2);
        assertMethodAllowed(method, zone);

        // TAXI requires Almaty delivery address.
        if (method == DeliveryType.TAXI) {
            assertTaxiCity(address);
        }

        return switch (zone) {
            case KAZAKHSTAN -> kazakhstanQuote(method, address, weight);
            case CIS        -> cisQuote(address, weight);
            case INTERNATIONAL -> internationalQuote(countryIso2, intlKind, weight);
        };
    }

    // ── availableMethods ─────────────────────────────────────────────────────────

    @Override
    public List<DeliveryMethodResponse> availableMethods(String countryIso2) {
        if (isBlank(countryIso2)) {
            throw new BusinessRuleException("Укажите страну доставки");
        }
        ShippingZone zone = countryService.resolveZone(countryIso2.trim().toUpperCase(Locale.ROOT));
        Set<DeliveryType> allowed = allowedMethods(zone);

        List<DeliveryMethodResponse> result = new ArrayList<>();

        if (allowed.contains(DeliveryType.PICKUP)) {
            result.add(new DeliveryMethodResponse(
                    DeliveryType.PICKUP,
                    "Самовывоз",
                    false,   // requiresAddress
                    false,   // requiresCitySearch
                    false,   // requiresPvz
                    null,    // cityRestriction — none, any KZ customer
                    zero()   // free
            ));
        }
        if (allowed.contains(DeliveryType.TAXI)) {
            result.add(new DeliveryMethodResponse(
                    DeliveryType.TAXI,
                    "Курьер",
                    true,
                    false,
                    false,
                    TAXI_CITY,  // restriction surfaced from backend, never hardcoded client-side
                    deliverySettingService.kzDeliveryFlatKzt().setScale(2, RoundingMode.HALF_UP)
            ));
        }
        if (allowed.contains(DeliveryType.POSTAL)) {
            result.add(new DeliveryMethodResponse(
                    DeliveryType.POSTAL,
                    "Казпочта",
                    true,
                    false,
                    false,
                    null,
                    // Flat KZ rate (delivery_settings), same as courier — not weight-based
                    deliverySettingService.kzDeliveryFlatKzt().setScale(2, RoundingMode.HALF_UP)
            ));
        }
        if (allowed.contains(DeliveryType.CDEK)) {
            result.add(new DeliveryMethodResponse(
                    DeliveryType.CDEK,
                    "СДЭК",
                    true,
                    true,   // requiresCitySearch
                    true,   // requiresPvz
                    null,
                    null    // city+weight-dependent
            ));
        }
        if (allowed.contains(DeliveryType.INTERNATIONAL)) {
            result.add(new DeliveryMethodResponse(
                    DeliveryType.INTERNATIONAL,
                    "Международная доставка",
                    true,
                    false,
                    false,
                    null,
                    null    // weight-dependent
            ));
        }

        return result;
    }

    // ── zone resolution & method matrix ─────────────────────────────────────────

    private ShippingZone resolveZone(DeliveryType method, String countryIso2) {
        if (countryIso2 != null && !countryIso2.isBlank()) {
            return countryService.resolveZone(countryIso2);
        }
        if (method == DeliveryType.TAXI) {
            return ShippingZone.KAZAKHSTAN;
        }
        throw new BusinessRuleException("Укажите страну доставки");
    }

    private void assertMethodAllowed(DeliveryType method, ShippingZone zone) {
        if (!allowedMethods(zone).contains(method)) {
            throw new BusinessRuleException("Способ доставки недоступен для выбранной страны");
        }
    }

    private void assertTaxiCity(DeliveryAddressRequest address) {
        if (address == null || isBlank(address.getCity())) {
            throw new BusinessRuleException("Для курьерской доставки укажите город");
        }
        String city = address.getCity().trim();
        if (!city.toLowerCase(Locale.ROOT).contains(TAXI_CITY.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("Курьерская доставка доступна только по " + TAXI_CITY);
        }
    }

    /**
     * Zone → allowed delivery methods matrix.
     * KAZAKHSTAN: PICKUP (any city), TAXI (Almaty only, enforced in assertTaxiCity), POSTAL (Kazpost), CDEK.
     * CIS: CDEK only (POSTAL removed per business requirements).
     * INTERNATIONAL: INTERNATIONAL only (Kazpost-backed, customer-facing label only).
     */
    private static Set<DeliveryType> allowedMethods(ShippingZone zone) {
        return switch (zone) {
            case KAZAKHSTAN  -> EnumSet.of(DeliveryType.PICKUP, DeliveryType.TAXI,
                                           DeliveryType.POSTAL, DeliveryType.CDEK);
            case CIS         -> EnumSet.of(DeliveryType.CDEK);
            case INTERNATIONAL -> EnumSet.of(DeliveryType.INTERNATIONAL);
        };
    }

    // ── per-zone pricing ────────────────────────────────────────────────────────

    private DeliveryQuote kazakhstanQuote(DeliveryType method, DeliveryAddressRequest address, BigDecimal weight) {
        return switch (method) {
            case PICKUP -> new DeliveryQuote(zero(), ShippingZone.KAZAKHSTAN, weight, null, null, null);
            case TAXI   -> {
                BigDecimal fee = deliverySettingService.kzDeliveryFlatKzt().setScale(2, RoundingMode.HALF_UP);
                yield new DeliveryQuote(fee, ShippingZone.KAZAKHSTAN, weight, null, null, null);
            }
            case POSTAL -> {
                // Kazpost within KZ is a flat rate (delivery_settings.KZ_DELIVERY_FLAT_KZT),
                // not weight-based: goods + flat fee = total.
                BigDecimal fee = deliverySettingService.kzDeliveryFlatKzt().setScale(2, RoundingMode.HALF_UP);
                yield new DeliveryQuote(fee, ShippingZone.KAZAKHSTAN, weight, null, null, null);
            }
            case CDEK -> {
                if (address == null || isBlank(address.getCity())) {
                    throw new BusinessRuleException("Для доставки СДЭК укажите город");
                }
                int cityCode = resolveCdekCityCode(address.getCity());
                CdekTariffResponse tariff = cdekDeliveryService.calculate(
                        new CdekTariffRequest(cityCode, toGrams(weight), null));
                BigDecimal fee = tariff.totalPrice().setScale(2, RoundingMode.HALF_UP);
                yield new DeliveryQuote(fee, ShippingZone.KAZAKHSTAN, weight, cityCode, null, null);
            }
            default -> throw new BusinessRuleException("Способ доставки недоступен для Казахстана: " + method);
        };
    }

    private DeliveryQuote cisQuote(DeliveryAddressRequest address, BigDecimal weight) {
        // CIS zone: CDEK only. POSTAL was removed from CIS per business requirements.
        if (address == null || isBlank(address.getCity())) {
            throw new BusinessRuleException("Для доставки СДЭК укажите город");
        }
        int cityCode = resolveCdekCityCode(address.getCity());
        CdekTariffResponse tariff = cdekDeliveryService.calculate(
                new CdekTariffRequest(cityCode, toGrams(weight), null));
        BigDecimal fee = tariff.totalPrice().setScale(2, RoundingMode.HALF_UP);
        return new DeliveryQuote(fee, ShippingZone.CIS, weight, cityCode, null, null);
    }

    private DeliveryQuote internationalQuote(String countryIso2, com.nurba.java.enums.IntlShipKind intlKind, BigDecimal weight) {
        InternationalShippingQuote q = internationalShippingService.quote(countryIso2, intlKind);
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
