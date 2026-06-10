package com.nurba.java.controller;

import com.nurba.java.api.InventoryApi;
import com.nurba.java.dto.request.SetInventoryRequest;
import com.nurba.java.dto.responce.InventoryResponse;
import com.nurba.java.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InventoryController implements InventoryApi {

    private final InventoryService service;

    @Override
    public List<InventoryResponse> getByGarment(Long designGarmentId) {
        return service.getByGarment(designGarmentId);
    }

    @Override
    public InventoryResponse set(SetInventoryRequest request) {
        return service.set(request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
