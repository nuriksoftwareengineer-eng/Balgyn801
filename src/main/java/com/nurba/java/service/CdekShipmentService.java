package com.nurba.java.service;

import com.nurba.java.dto.responce.CdekShipmentResponse;

import java.util.List;

public interface CdekShipmentService {

    CdekShipmentResponse getById(Long id);
    List<CdekShipmentResponse> getAll();
}
