package com.nurba.java.api;

import com.nurba.java.dto.responce.CdekShipmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "CDEK Shipment", description = "Отправки СДЭК (админ)")
@RequestMapping("/api/v1/cdek-shipment")
public interface CdekShipmentApi {

    @Operation(summary = "Список отправок СДЭК")
    @GetMapping
    List<CdekShipmentResponse> getAll();

    @Operation(summary = "Отправка СДЭК по ID")
    @GetMapping("/{id}")
    CdekShipmentResponse getById(@PathVariable Long id);

    @Operation(summary = "Отправление по заказу (null, если ещё не создано)")
    @GetMapping("/by-order/{orderId}")
    CdekShipmentResponse getByOrder(@PathVariable Long orderId);

    @Operation(summary = "Создать / повторить создание отправления для заказа")
    @PostMapping("/by-order/{orderId}/create")
    CdekShipmentResponse createShipment(@PathVariable Long orderId);

    @Operation(summary = "Синхронизировать статус отправления с СДЭК")
    @PostMapping("/by-order/{orderId}/sync")
    CdekShipmentResponse syncShipment(@PathVariable Long orderId);

    @Operation(summary = "Отменить отправление СДЭК")
    @PostMapping("/by-order/{orderId}/cancel")
    CdekShipmentResponse cancelShipment(@PathVariable Long orderId);

    @Operation(summary = "Получить / обновить URL документов СДЭК (штрихкод, квитанция)")
    @PostMapping("/by-order/{orderId}/fetch-docs")
    CdekShipmentResponse fetchDocuments(@PathVariable Long orderId);
}
