package com.nurba.java.controller;

import com.nurba.java.api.GarmentProfileApi;
import com.nurba.java.dto.request.CreateGarmentProfileRequest;
import com.nurba.java.dto.responce.GarmentProfileResponse;
import com.nurba.java.service.GarmentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GarmentProfileController implements GarmentProfileApi {

    private final GarmentProfileService service;

    @Override
    public List<GarmentProfileResponse> listAll() {
        return service.listAll();
    }

    @Override
    public GarmentProfileResponse getById(Long id) {
        return service.getById(id);
    }

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    public GarmentProfileResponse create(CreateGarmentProfileRequest request) {
        return service.create(request);
    }

    @Override
    public GarmentProfileResponse update(Long id, CreateGarmentProfileRequest request) {
        return service.update(id, request);
    }

    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Long id) {
        service.delete(id);
    }
}
