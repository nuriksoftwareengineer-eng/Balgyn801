package com.nurba.java.api;

import com.nurba.java.dto.responce.CdekShipmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "CDEK Shipment", description = "Отправки СДЭК")
@RequestMapping("/api/v1/cdek-shipment")
public interface CdekShipmentApi {

    @Operation(summary = "Список отправок СДЭК")
    @GetMapping
    List<CdekShipmentResponse> getAll();

    @Operation(summary = "Отправка СДЭК по ID")
    @GetMapping("/{id}")
    CdekShipmentResponse getById(@PathVariable Long id);
}
