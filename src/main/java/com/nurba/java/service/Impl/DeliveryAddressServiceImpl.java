package com.nurba.java.service.Impl;

import com.nurba.java.dto.responce.DeliveryAddressResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.DeliveryMapper;
import com.nurba.java.repositories.DeliveryAddressRepository;
import com.nurba.java.service.DeliveryAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryAddressServiceImpl implements DeliveryAddressService {

    private final DeliveryAddressRepository deliveryAddressRepository;
    private final DeliveryMapper deliveryMapper;

    @Override
    public DeliveryAddressResponse getById(Long id) {
        return deliveryMapper.toResponse(deliveryAddressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Адрес доставки не найден")));
    }

    @Override
    public List<DeliveryAddressResponse> getAll() {
        return deliveryAddressRepository.findAll()
                .stream()
                .map(deliveryMapper::toResponse)
                .toList();
    }
}
