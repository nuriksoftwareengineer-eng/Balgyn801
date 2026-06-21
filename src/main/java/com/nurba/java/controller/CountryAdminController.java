package com.nurba.java.controller;

import com.nurba.java.api.CountryAdminApi;
import com.nurba.java.dto.request.UpsertCountryRequest;
import com.nurba.java.dto.responce.AdminCountryResponse;
import com.nurba.java.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CountryAdminController implements CountryAdminApi {

    private final CountryService service;

    @Override
    public List<AdminCountryResponse> listAll() {
        return service.listAll();
    }

    @Override
    public AdminCountryResponse create(UpsertCountryRequest request) {
        return service.create(request);
    }

    @Override
    public AdminCountryResponse update(Long id, UpsertCountryRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
