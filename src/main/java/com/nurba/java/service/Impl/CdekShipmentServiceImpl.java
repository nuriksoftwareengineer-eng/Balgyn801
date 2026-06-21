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
        return toResponse(cdekShipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Отправка СДЭК не найдена")));
    }

    @Override
    public List<CdekShipmentResponse> getAll() {
        return cdekShipmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CdekShipmentResponse getByOrder(Long orderId) {
        return cdekShipmentRepository.findByOrder_Id(orderId)
                .map(this::toResponse)
                .orElse(null);
    }

    /** Маппинг + признак mock по префиксу UUID. */
    private CdekShipmentResponse toResponse(com.nurba.java.domain.CdekShipment shipment) {
        CdekShipmentResponse resp = cdekMapper.toResponse(shipment);
        resp.setMock(shipment.getCdekOrderUuid() != null
                && shipment.getCdekOrderUuid().startsWith("MOCK-"));
        return resp;
    }
}
