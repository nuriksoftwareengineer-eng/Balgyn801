package com.nurba.java.controller;

import com.nurba.java.api.GarmentWeightApi;
import com.nurba.java.dto.request.UpdateGarmentWeightRequest;
import com.nurba.java.dto.responce.GarmentWeightResponse;
import com.nurba.java.enums.GarmentType;
import com.nurba.java.service.GarmentWeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GarmentWeightController implements GarmentWeightApi {

    private final GarmentWeightService service;

    @Override
    public List<GarmentWeightResponse> listAll() {
        return service.listAll();
    }

    @Override
    public GarmentWeightResponse upsert(GarmentType garmentType, UpdateGarmentWeightRequest request) {
        return service.upsert(garmentType, request);
    }
}
