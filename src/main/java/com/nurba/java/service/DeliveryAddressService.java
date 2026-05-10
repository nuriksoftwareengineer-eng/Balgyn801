package com.nurba.java.service;

import com.nurba.java.dto.responce.DeliveryAddressResponse;

import java.util.List;

public interface DeliveryAddressService {

    DeliveryAddressResponse getById(Long id);
    List<DeliveryAddressResponse> getAll();
}
