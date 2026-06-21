package com.nurba.java.api;

import com.nurba.java.dto.request.SetKzDeliveryFlatRequest;
import com.nurba.java.dto.responce.DeliverySettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin / Delivery settings")
@RequestMapping("/api/v1/admin/delivery/settings")
public interface DeliverySettingsApi {

    @Operation(summary = "Get editable delivery settings")
    @GetMapping
    DeliverySettingsResponse get();

    @Operation(summary = "Set the flat Kazakhstan delivery fee (KZT)")
    @PutMapping("/kz-flat")
    DeliverySettingsResponse setKzFlat(@Valid @RequestBody SetKzDeliveryFlatRequest request);
}
