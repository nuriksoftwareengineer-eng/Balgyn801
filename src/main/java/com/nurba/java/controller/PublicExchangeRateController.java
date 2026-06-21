package com.nurba.java.controller;

import com.nurba.java.dto.responce.PublicExchangeRatesResponse;
import com.nurba.java.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoint — no auth required — returns all KZT exchange rates for frontend display. */
@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
public class PublicExchangeRateController {

    private final ExchangeRateService service;

    @GetMapping
    public PublicExchangeRatesResponse get() {
        return service.publicRates();
    }
}
