package com.nurba.java.service.delivery;

import com.nurba.java.dto.delivery.CdekCityDto;
import com.nurba.java.dto.delivery.CdekDeliveryPointDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Работа с ПВЗ/городами СДЭК: поиск города, список ПВЗ. Тонкий фасад над
 * {@link CdekDeliveryService}, который уже умеет mock/real (по {@code cdek.provider}).
 * Выбор/сохранение pvzCode и адреса ПВЗ происходит на checkout (DeliveryAddress) —
 * фронтенд не меняется при переключении на реальный API.
 */
@Service
@RequiredArgsConstructor
public class CdekDeliveryPointService {

    private final CdekDeliveryService delegate;

    /** Поиск города по подстроке (для автодополнения на checkout). */
    public List<CdekCityDto> searchCities(String query, Integer limit) {
        return delegate.searchCities(query, limit);
    }

    /** Список ПВЗ/постаматов в городе по коду города из справочника СДЭК. */
    public List<CdekDeliveryPointDto> deliveryPoints(int cityCode) {
        return delegate.deliveryPoints(cityCode);
    }
}
