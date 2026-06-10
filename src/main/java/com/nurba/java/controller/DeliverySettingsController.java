package com.nurba.java.controller;

import com.nurba.java.api.DeliverySettingsApi;
import com.nurba.java.dto.request.SetKzDeliveryFlatRequest;
import com.nurba.java.dto.responce.DeliverySettingsResponse;
import com.nurba.java.service.DeliverySettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeliverySettingsController implements DeliverySettingsApi {

    private final DeliverySettingService service;

    @Override
    public DeliverySettingsResponse get() {
        return new DeliverySettingsResponse(service.kzDeliveryFlatKzt());
    }

    @Override
    public DeliverySettingsResponse setKzFlat(SetKzDeliveryFlatRequest request) {
        return new DeliverySettingsResponse(service.setKzDeliveryFlatKzt(request.flatKzt()));
    }
}
