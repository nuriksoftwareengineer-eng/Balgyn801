package com.nurba.java.controller;

import com.nurba.java.api.CountryApi;
import com.nurba.java.dto.responce.CountryResponse;
import com.nurba.java.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CountryController implements CountryApi {

    private final CountryService service;

    @Override
    public List<CountryResponse> listActive() {
        return service.listActive();
    }
}
