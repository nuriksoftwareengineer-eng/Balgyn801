package com.nurba.java.controller;

import com.nurba.java.api.CatalogGroupApi;
import com.nurba.java.dto.request.CreateCatalogGroupRequest;
import com.nurba.java.dto.responce.CatalogGroupResponse;
import com.nurba.java.service.CatalogGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogGroupController implements CatalogGroupApi {

    private final CatalogGroupService service;

    @Override
    public List<CatalogGroupResponse> getAll() {
        return service.getAll();
    }

    @Override
    public CatalogGroupResponse getById(Long id) {
        return service.getById(id);
    }

    @Override
    public CatalogGroupResponse create(CreateCatalogGroupRequest request) {
        return service.create(request);
    }

    @Override
    public CatalogGroupResponse update(Long id, CreateCatalogGroupRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
