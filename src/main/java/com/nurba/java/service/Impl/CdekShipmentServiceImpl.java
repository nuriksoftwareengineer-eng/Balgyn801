package com.nurba.java.service.Impl;

import com.nurba.java.dto.responce.CdekShipmentResponse;
import com.nurba.java.exception.NotFoundException;
import com.nurba.java.mapper.CdekMapper;
import com.nurba.java.repositories.CdekShipmentRepository;
import com.nurba.java.service.CdekShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CdekShipmentServiceImpl implements CdekShipmentService {

    private final CdekShipmentRepository cdekShipmentRepository;
    private final CdekMapper cdekMapper;

    @Override
    public CdekShipmentResponse getById(Long id) {
        return cdekMapper.toResponse(cdekShipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Отправка СДЭК не найдена")));
    }

    @Override
    public List<CdekShipmentResponse> getAll() {
        return cdekShipmentRepository.findAll()
                .stream()
                .map(cdekMapper::toResponse)
                .toList();
    }
}
