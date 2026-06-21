package com.nurba.java.service.delivery;

import com.nurba.java.dto.delivery.CdekOrderTariffRequest;
import com.nurba.java.dto.delivery.CdekOrderTariffResponse;
import com.nurba.java.dto.delivery.CdekTariffRequest;
import com.nurba.java.dto.delivery.CdekTariffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Калькулятор тарифов СДЭК. Тонкий фасад над {@link CdekDeliveryService}, который уже умеет
 * mock/real (по {@code cdek.provider}) и возвращает цену, сроки и {@code sourcedFromStub}.
 * Выделен как именованный сервис интеграционного слоя; существующая логика расчёта не дублируется.
 */
@Service
@RequiredArgsConstructor
public class CdekCalculatorService {

    private final CdekDeliveryService delegate;

    /** Расчёт по весу/городу. */
    public CdekTariffResponse calculate(CdekTariffRequest request) {
        return delegate.calculate(request);
    }

    /** Расчёт по корзине (вес считается через GarmentWeightService на бэкенде). */
    public CdekOrderTariffResponse calculateOrder(CdekOrderTariffRequest request) {
        return delegate.calculateOrder(request);
    }
}
