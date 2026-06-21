package com.nurba.java.controller;

import com.nurba.java.api.DesignGarmentApi;
import com.nurba.java.dto.request.CreateDesignGarmentRequest;
import com.nurba.java.dto.request.UpdateDesignGarmentRequest;
import com.nurba.java.dto.responce.DesignGarmentResponse;
import com.nurba.java.service.DesignGarmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DesignGarmentController implements DesignGarmentApi {

    private final DesignGarmentService service;

    @Override
    public List<DesignGarmentResponse> getByDesign(Long designId) {
        return service.getByDesign(designId);
    }

    @Override
    public DesignGarmentResponse getById(Long id) {
        return service.getById(id);
    }

    @Override
    public DesignGarmentResponse create(CreateDesignGarmentRequest request) {
        return service.create(request);
    }

    @Override
    public DesignGarmentResponse update(Long id, UpdateDesignGarmentRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
