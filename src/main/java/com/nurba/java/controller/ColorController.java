package com.nurba.java.controller;

import com.nurba.java.api.ColorApi;
import com.nurba.java.dto.request.CreateColorRequest;
import com.nurba.java.dto.responce.ColorResponse;
import com.nurba.java.service.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ColorController implements ColorApi {

    private final ColorService service;

    @Override
    public List<ColorResponse> getAll() {
        return service.getAll();
    }

    @Override
    public ColorResponse getById(Long id) {
        return service.getById(id);
    }

    @Override
    public ColorResponse create(CreateColorRequest request) {
        return service.create(request);
    }

    @Override
    public ColorResponse update(Long id, CreateColorRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
