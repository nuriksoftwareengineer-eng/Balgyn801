package com.nurba.java.controller;

import com.nurba.java.api.ExchangeRateApi;
import com.nurba.java.dto.request.SetExchangeRateRequest;
import com.nurba.java.dto.responce.ExchangeRateResponse;
import com.nurba.java.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExchangeRateController implements ExchangeRateApi {

    private final ExchangeRateService service;

    @Override
    public ExchangeRateResponse get() {
        return service.current();
    }

    @Override
    public ExchangeRateResponse set(SetExchangeRateRequest request) {
        return service.setManualRate(request.kztPerUsd(), Boolean.TRUE.equals(request.frozen()));
    }

    @Override
    public ExchangeRateResponse refresh() {
        service.refreshFromProvider();
        return service.current();
    }
}
