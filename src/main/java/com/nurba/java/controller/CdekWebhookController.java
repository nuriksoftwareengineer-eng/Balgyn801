package com.nurba.java.controller;

import com.nurba.java.dto.delivery.CdekWebhookRequest;
import com.nurba.java.service.CdekWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Публичный приёмник вебхуков CDEK (без JWT — вызывает внешний сервис СДЭК).
 * Работает уже сейчас на тестовых payload: меняет статус отправления в {@code cdek_shipments}.
 * Всегда отвечает 200, чтобы СДЭК не ретраил при бизнес-условиях (отправление не найдено и т.п.).
 */
@Tag(name = "Delivery", description = "Вебхуки СДЭК")
@RestController
@RequestMapping("/api/v1/delivery/cdek")
@RequiredArgsConstructor
public class CdekWebhookController {

    private final CdekWebhookService webhookService;

    @Operation(summary = "Вебхук статусов СДЭК")
    @PostMapping("/webhook")
    public Map<String, Object> webhook(@RequestBody CdekWebhookRequest request) {
        boolean updated = webhookService.handle(request);
        return Map.of("ok", true, "updated", updated);
    }
}
