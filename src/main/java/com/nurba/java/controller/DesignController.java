package com.nurba.java.controller;

import com.nurba.java.api.DesignApi;
import com.nurba.java.dto.request.CreateDesignRequest;
import com.nurba.java.dto.responce.DesignResponse;
import com.nurba.java.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DesignController implements DesignApi {

    private final DesignService service;

    @Override
    public List<DesignResponse> getAll(Long collectionId) {
        return service.getAll(collectionId);
    }

    @Override
    public DesignResponse getById(Long id) {
        return service.getById(id);
    }

    @Override
    public DesignResponse create(CreateDesignRequest request) {
        return service.create(request);
    }

    @Override
    public DesignResponse update(Long id, CreateDesignRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
