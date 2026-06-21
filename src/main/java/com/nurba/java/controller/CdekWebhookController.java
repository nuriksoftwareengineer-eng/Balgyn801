package com.nurba.java.controller;

import com.nurba.java.dto.delivery.CdekWebhookRequest;
import com.nurba.java.service.CdekWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Публичный приёмник вебхуков CDEK (без JWT — вызывает внешний сервис СДЭК).
 * Всегда отвечает 200, чтобы СДЭК не ретраил при бизнес-условиях (отправление не найдено и т.п.).
 *
 * Аутентификация: CDEK присылает токен в заголовке X-Authorization.
 * При непустом cdek.webhook-token любой запрос без совпадающего токена отклоняется с 401.
 * При пустом webhook-token (dev/mock) проверка пропускается с предупреждением в лог.
 */
@Slf4j
@Tag(name = "Delivery", description = "Вебхуки СДЭК")
@RestController
@RequestMapping("/api/v1/delivery/cdek")
public class CdekWebhookController {

    private final CdekWebhookService webhookService;
    private final String webhookToken;

    public CdekWebhookController(CdekWebhookService webhookService,
                                  @Value("${cdek.webhook-token:}") String webhookToken) {
        this.webhookService = webhookService;
        this.webhookToken = webhookToken;
    }

    @Operation(summary = "Вебхук статусов СДЭК")
    @PostMapping("/webhook")
    public Map<String, Object> webhook(
            @RequestHeader(value = "X-Authorization", required = false) String authHeader,
            @RequestBody CdekWebhookRequest request) {

        if (webhookToken == null || webhookToken.isBlank()) {
            log.warn("[CDEK] webhook-token not configured — accepting unsigned webhook (dev/mock mode)");
        } else {
            String received = authHeader != null ? authHeader.trim() : "";
            if (!webhookToken.equals(received)) {
                log.warn("[CDEK] Webhook rejected: invalid X-Authorization token");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid CDEK webhook token");
            }
        }

        boolean updated = webhookService.handle(request);
        return Map.of("ok", true, "updated", updated);
    }
}
