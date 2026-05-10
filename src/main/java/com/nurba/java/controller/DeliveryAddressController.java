package com.nurba.java.controller;

import com.nurba.java.api.DeliveryAddressApi;
import com.nurba.java.dto.responce.DeliveryAddressResponse;
import com.nurba.java.service.DeliveryAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DeliveryAddressController implements DeliveryAddressApi {

    private final DeliveryAddressService deliveryAddressService;

    @Override
    public List<DeliveryAddressResponse> getAll() {
        return deliveryAddressService.getAll();
    }

    @Override
    public DeliveryAddressResponse getById(Long id) {
        return deliveryAddressService.getById(id);
    }
}
