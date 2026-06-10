package com.nurba.java.controller;

import com.nurba.java.api.SizeApi;
import com.nurba.java.dto.request.CreateSizeRequest;
import com.nurba.java.dto.responce.SizeResponse;
import com.nurba.java.service.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SizeController implements SizeApi {

    private final SizeService service;

    @Override
    public List<SizeResponse> getAll() {
        return service.getAll();
    }

    @Override
    public SizeResponse getById(Long id) {
        return service.getById(id);
    }

    @Override
    public SizeResponse create(CreateSizeRequest request) {
        return service.create(request);
    }

    @Override
    public SizeResponse update(Long id, CreateSizeRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
