package com.nurba.java.api;

import com.nurba.java.dto.responce.CountryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Catalog / Countries", description = "Список стран для оформления заказа")
@RequestMapping("/api/v1/catalog/countries")
public interface CountryApi {

    @Operation(summary = "Активные страны доставки (для витрины)")
    @GetMapping
    List<CountryResponse> listActive();
}
