package com.nurba.java.controller;

import com.nurba.java.api.CdekShipmentApi;
import com.nurba.java.dto.responce.CdekShipmentResponse;
import com.nurba.java.service.CdekOrderService;
import com.nurba.java.service.CdekShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CdekShipmentController implements CdekShipmentApi {

    private final CdekShipmentService cdekShipmentService;
    private final CdekOrderService cdekOrderService;

    @Override
    public List<CdekShipmentResponse> getAll() {
        return cdekShipmentService.getAll();
    }

    @Override
    public CdekShipmentResponse getById(Long id) {
        return cdekShipmentService.getById(id);
    }

    @Override
    public CdekShipmentResponse getByOrder(Long orderId) {
        return cdekShipmentService.getByOrder(orderId);
    }

    @Override
    public CdekShipmentResponse createShipment(Long orderId) {
        return cdekOrderService.createShipment(orderId);
    }

    @Override
    public CdekShipmentResponse syncShipment(Long orderId) {
        return cdekOrderService.syncStatus(orderId);
    }

    @Override
    public CdekShipmentResponse cancelShipment(Long orderId) {
        return cdekOrderService.cancelShipment(orderId);
    }

    @Override
    public CdekShipmentResponse fetchDocuments(Long orderId) {
        return cdekOrderService.fetchDocuments(orderId);
    }
}
