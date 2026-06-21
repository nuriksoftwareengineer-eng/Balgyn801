package com.nurba.java.service.delivery;

import com.nurba.java.dto.delivery.CdekCityDto;
import com.nurba.java.dto.delivery.CdekDeliveryPointDto;
import com.nurba.java.dto.delivery.CdekOrderItemRequest;
import com.nurba.java.dto.delivery.CdekOrderTariffRequest;
import com.nurba.java.dto.delivery.CdekOrderTariffResponse;
import com.nurba.java.dto.delivery.CdekTariffRequest;
import com.nurba.java.dto.delivery.CdekTariffResponse;
import com.nurba.java.domain.DesignGarment;
import com.nurba.java.domain.DesignGarmentPrice;
import com.nurba.java.enums.Currency;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.repositories.DesignGarmentPriceRepository;
import com.nurba.java.repositories.DesignGarmentRepository;
import com.nurba.java.repositories.ProductRepository;
import com.nurba.java.service.GarmentWeightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Высокоуровневый сервис над {@link CdekClient}, который отдаёт нашим контроллерам уже
 * нормализованные DTO. При отсутствии ключей СДЭК работает в режиме «заглушки», чтобы
 * фронт можно было разрабатывать параллельно.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdekDeliveryService {

    /** Тариф «склад-склад» — мы отправляем со склада, клиент забирает в ПВЗ. */
    private static final int FALLBACK_TARIFF = 136;
    private static final int WEIGHT_BASE_GRAMS = 250;
    private static final int WEIGHT_PER_UNIT_GRAMS = 150;
    private static final int WEIGHT_MIN_GRAMS = 100;

    /** Алматы (код по справочнику СДЭК), используется в stub-режиме для предсказуемых ответов. */
    private static final int STUB_SENDER_CITY = 270;

    private final CdekClient client;
    private final ProductRepository productRepository;
    private final DesignGarmentRepository designGarmentRepository;
    private final DesignGarmentPriceRepository designGarmentPriceRepository;
    private final GarmentWeightService garmentWeightService;

    public List<CdekCityDto> searchCities(String query, Integer limit) {
        String q = query == null ? "" : query.trim();
        int size = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);
        if (q.isBlank()) {
            return List.of();
        }
        if (!client.useRealApi()) {
            return stubCities(q, size);
        }
        return client.searchCities(q, size);
    }

    public List<CdekDeliveryPointDto> deliveryPoints(int cityCode) {
        if (!client.useRealApi()) {
            return stubDeliveryPoints(cityCode);
        }
        return client.deliveryPoints(cityCode);
    }

    public CdekTariffResponse calculate(CdekTariffRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Пустой запрос расчёта доставки");
        }
        Integer tariff = request.tariffCode() != null ? request.tariffCode() : defaultTariff();
        if (!client.useRealApi()) {
            return stubTariff(request.weightGrams(), tariff);
        }
        Integer senderCity = client.senderCity();
        if (senderCity == null) {
            throw new BusinessRuleException(
                    "СДЭК: не задан город отправителя (cdek.sender-city)");
        }
        var raw = client.calculateTariff(senderCity, request.toCityCode(), tariff, request.weightGrams());
        BigDecimal price = raw.totalSum() != null ? raw.totalSum() : raw.deliverySum();
        if (price == null) {
            throw new BusinessRuleException("СДЭК: пустой ответ калькулятора");
        }
        return new CdekTariffResponse(
                price.setScale(2, RoundingMode.HALF_UP),
                raw.currency() == null ? "KZT" : raw.currency().toUpperCase(Locale.ROOT),
                raw.periodMin(),
                raw.periodMax(),
                raw.tariffCode() == null ? tariff : raw.tariffCode(),
                false);
    }

    public CdekOrderTariffResponse calculateOrder(CdekOrderTariffRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Пустой запрос расчёта доставки");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessRuleException("Добавьте товары в корзину перед расчётом доставки");
        }

        BigDecimal itemsTotal = BigDecimal.ZERO;
        int totalWeightGrams = 0;
        // Legacy product items don't carry an exact garment weight; tracked separately for the
        // heuristic formula (existing behaviour for product-only carts is preserved exactly).
        int legacyProductQty = 0;

        for (CdekOrderItemRequest item : request.items()) {
            boolean isDesign  = item.designGarmentId() != null && item.designGarmentId() > 0;
            boolean isProduct = item.productId()       != null && item.productId()       > 0;

            if (!isDesign && !isProduct) {
                throw new BusinessRuleException("Некорректный товар в запросе расчёта доставки");
            }

            int qty = item.quantity() == null ? 0 : item.quantity();
            if (qty <= 0) {
                throw new BusinessRuleException("Количество товара для расчёта должно быть больше 0");
            }

            if (isDesign) {
                DesignGarment garment = designGarmentRepository.findById(item.designGarmentId())
                        .orElseThrow(() -> new NotFoundException(
                                "Вариант дизайна не найден: id=" + item.designGarmentId()));
                DesignGarmentPrice price = designGarmentPriceRepository
                        .findByDesignGarment_IdAndCurrency(garment.getId(), Currency.KZT)
                        .orElseThrow(() -> new BusinessRuleException(
                                "Цена в KZT не задана для варианта: id=" + item.designGarmentId()));
                itemsTotal = itemsTotal.add(price.getAmount().multiply(BigDecimal.valueOf(qty)));
                // Weight uses the same source as order creation (GarmentWeightService),
                // not the legacy heuristic, so pre-order estimates match actual order weight.
                BigDecimal unitKg = garmentWeightService.weightForType(garment.getGarmentType());
                totalWeightGrams += unitKg
                        .multiply(BigDecimal.valueOf(qty))
                        .multiply(BigDecimal.valueOf(1000))
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            } else {
                var product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new NotFoundException("Товар не найден: id=" + item.productId()));
                if (product.getPrice() == null) {
                    throw new BusinessRuleException("У товара не задана цена: id=" + item.productId());
                }
                itemsTotal = itemsTotal.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
                legacyProductQty += qty;
            }
        }

        // Legacy product items use the existing heuristic; design items contribute exact garment weight.
        if (legacyProductQty > 0) {
            totalWeightGrams += estimateWeightGrams(legacyProductQty);
        }
        // CDEK rejects weight ≤ 0; guard with the minimum constant.
        int finalWeightGrams = Math.max(WEIGHT_MIN_GRAMS, totalWeightGrams);

        CdekTariffResponse tariff = calculate(new CdekTariffRequest(
                request.toCityCode(),
                finalWeightGrams,
                request.tariffCode()
        ));
        BigDecimal itemsTotalScaled = itemsTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal orderTotal = itemsTotalScaled.add(tariff.totalPrice()).setScale(2, RoundingMode.HALF_UP);

        return new CdekOrderTariffResponse(
                tariff.totalPrice(),
                itemsTotalScaled,
                orderTotal,
                finalWeightGrams,
                tariff.currency(),
                tariff.minDays(),
                tariff.maxDays(),
                tariff.tariffCode(),
                tariff.sourcedFromStub()
        );
    }

    private Integer defaultTariff() {
        Integer cfg = client.defaultTariff();
        return cfg != null ? cfg : FALLBACK_TARIFF;
    }

    private int estimateWeightGrams(int totalQty) {
        int q = Math.max(0, totalQty);
        return Math.max(WEIGHT_MIN_GRAMS, WEIGHT_BASE_GRAMS + q * WEIGHT_PER_UNIT_GRAMS);
    }

    private List<CdekCityDto> stubCities(String query, int size) {
        log.debug("СДЭК stub: searchCities('{}', {})", query, size);
        return List.of(
                new CdekCityDto(270, "Алматы", "Алматинская область", "Казахстан", "KZ"),
                new CdekCityDto(591, "Астана", "Акмолинская область", "Казахстан", "KZ"),
                new CdekCityDto(44, "Москва", "Москва", "Россия", "RU"),
                new CdekCityDto(137, "Санкт-Петербург", "Санкт-Петербург", "Россия", "RU")
        ).stream()
                .filter(c -> c.city() != null
                        && c.city().toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT)))
                .limit(size)
                .toList();
    }

    private List<CdekDeliveryPointDto> stubDeliveryPoints(int cityCode) {
        log.debug("СДЭК stub: deliveryPoints({})", cityCode);
        return List.of(
                new CdekDeliveryPointDto(
                        "STUB-" + cityCode + "-1",
                        "Тестовый ПВЗ №1",
                        "ул. Тестовая, 1",
                        76.94,
                        43.24,
                        "Пн-Пт 10:00-19:00",
                        "PVZ"),
                new CdekDeliveryPointDto(
                        "STUB-" + cityCode + "-2",
                        "Тестовый ПВЗ №2",
                        "пр. Абая, 10",
                        76.93,
                        43.25,
                        "Ежедневно 9:00-21:00",
                        "PVZ")
        );
    }

    private CdekTariffResponse stubTariff(int weightGrams, int tariff) {
        log.debug("СДЭК stub: calculate(weight={}, tariff={})", weightGrams, tariff);
        BigDecimal base = new BigDecimal("1500");
        BigDecimal perKg = new BigDecimal("400");
        BigDecimal kilos = BigDecimal.valueOf(weightGrams).divide(new BigDecimal("1000"), 2, RoundingMode.UP);
        BigDecimal price = base.add(perKg.multiply(kilos)).setScale(2, RoundingMode.HALF_UP);
        return new CdekTariffResponse(price, "KZT", 2, 5, tariff, true);
    }

    @SuppressWarnings("unused")
    private static int senderForStub() {
        return STUB_SENDER_CITY;
    }
}
