package com.nurba.java.controller;

import com.nurba.java.api.DeliveryMethodsApi;
import com.nurba.java.dto.delivery.DeliveryMethodResponse;
import com.nurba.java.service.delivery.DeliveryPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DeliveryMethodsController implements DeliveryMethodsApi {

    private final DeliveryPricingService deliveryPricingService;

    @Override
    public List<DeliveryMethodResponse> availableMethods(String countryIso2) {
        return deliveryPricingService.availableMethods(countryIso2);
    }
}
