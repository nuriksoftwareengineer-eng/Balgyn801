package com.nurba.java.controller;

import com.nurba.java.api.DeliveryApi;
import com.nurba.java.dto.delivery.CdekCityDto;
import com.nurba.java.dto.delivery.CdekDeliveryPointDto;
import com.nurba.java.dto.delivery.CdekOrderTariffRequest;
import com.nurba.java.dto.delivery.CdekOrderTariffResponse;
import com.nurba.java.dto.delivery.CdekTariffRequest;
import com.nurba.java.dto.delivery.CdekTariffResponse;
import com.nurba.java.service.delivery.CdekDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DeliveryController implements DeliveryApi {

    private final CdekDeliveryService deliveryService;

    @Override
    public List<CdekCityDto> searchCities(String query, Integer limit) {
        return deliveryService.searchCities(query, limit);
    }

    @Override
    public List<CdekDeliveryPointDto> deliveryPoints(int cityCode) {
        return deliveryService.deliveryPoints(cityCode);
    }

    @Override
    public CdekTariffResponse calculate(CdekTariffRequest request) {
        return deliveryService.calculate(request);
    }

    @Override
    public CdekOrderTariffResponse calculateOrder(CdekOrderTariffRequest request) {
        return deliveryService.calculateOrder(request);
    }
}
