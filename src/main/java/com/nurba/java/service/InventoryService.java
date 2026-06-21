package com.nurba.java.service;

import com.nurba.java.dto.request.SetInventoryRequest;
import com.nurba.java.dto.responce.InventoryResponse;

import java.util.List;

public interface InventoryService {
    List<InventoryResponse> getByGarment(Long designGarmentId);
    InventoryResponse set(SetInventoryRequest request);
    void delete(Long id);
}
