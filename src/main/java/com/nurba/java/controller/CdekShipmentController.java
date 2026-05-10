package com.nurba.java.controller;

import com.nurba.java.api.CdekShipmentApi;
import com.nurba.java.dto.responce.CdekShipmentResponse;
import com.nurba.java.service.CdekShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CdekShipmentController implements CdekShipmentApi {

    private final CdekShipmentService cdekShipmentService;

    @Override
    public List<CdekShipmentResponse> getAll() {
        return cdekShipmentService.getAll();
    }

    @Override
    public CdekShipmentResponse getById(Long id) {
        return cdekShipmentService.getById(id);
    }
}
