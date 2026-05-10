package com.nurba.java.api;

import com.nurba.java.dto.responce.DeliveryAddressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Delivery Address", description = "Адреса доставки")
@RequestMapping("/api/v1/delivery-address")
public interface DeliveryAddressApi {

    @Operation(summary = "Список адресов доставки")
    @GetMapping
    List<DeliveryAddressResponse> getAll();

    @Operation(summary = "Адрес доставки по ID")
    @GetMapping("/{id}")
    DeliveryAddressResponse getById(@PathVariable Long id);
}
