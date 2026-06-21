package com.nurba.java.api;

import com.nurba.java.dto.request.SetExchangeRateRequest;
import com.nurba.java.dto.responce.ExchangeRateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin / Exchange rate")
@RequestMapping("/api/v1/admin/exchange-rate")
public interface ExchangeRateApi {

    @Operation(summary = "Get the cached KZT-per-USD rate")
    @GetMapping
    ExchangeRateResponse get();

    @Operation(summary = "Set the KZT-per-USD rate manually (optionally freeze it)")
    @PutMapping
    ExchangeRateResponse set(@Valid @RequestBody SetExchangeRateRequest request);

    @Operation(summary = "Trigger a best-effort refresh from the rate provider")
    @PostMapping("/refresh")
    ExchangeRateResponse refresh();
}
