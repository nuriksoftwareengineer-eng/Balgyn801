package com.nurba.java.service;

import com.nurba.java.dto.request.CreateCatalogGroupRequest;
import com.nurba.java.dto.responce.CatalogGroupResponse;

import java.util.List;

public interface CatalogGroupService {
    List<CatalogGroupResponse> getAll();
    CatalogGroupResponse getById(Long id);
    CatalogGroupResponse create(CreateCatalogGroupRequest request);
    CatalogGroupResponse update(Long id, CreateCatalogGroupRequest request);
    void delete(Long id);
}
