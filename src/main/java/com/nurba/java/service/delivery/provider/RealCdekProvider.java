package com.nurba.java.service.delivery.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.config.CdekProperties;
import com.nurba.java.enums.CdekShipmentStatus;
import com.nurba.java.exception.BusinessRuleException;
import com.nurba.java.service.delivery.CdekAuthService;
import com.nurba.java.service.delivery.CdekClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реальный провайдер CDEK API v2 поверх {@link CdekClient}. Активируется через
 * {@code cdek.provider=real} (или auto при заданных ключах). Бизнес-логика магазина его
 * не вызывает напрямую — только через {@link DeliveryProvider}.
 *
 * <p>Тело запроса /orders собрано по документации CDEK v2. Корректность реальных вызовов
 * проверяется после получения ключей (Client ID / Client Secret) — код к этому готов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealCdekProvider implements DeliveryProvider {

    private final CdekClient client;
    private final CdekAuthService auth;
    private final CdekProperties props;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isMock() {
        return false;
    }

    @Override
    public String name() {
        return "real-cdek";
    }

    @Override
    public ShipmentResult createShipment(ShipmentRequest request) {
        ensureReady();
        Map<String, Object> body = buildOrderBody(request);
        CdekClient.CdekOrderRaw raw = client.createCdekOrder(body);
        log.info("CDEK real: создан заказ uuid={} order={}", raw.uuid(), request.orderId());
        return new ShipmentResult(
                raw.uuid(),
                null, // трек-номер появляется асинхронно — заберём в syncStatus/webhook
                CdekShipmentStatus.CREATED,
                request.pvzCode() != null ? "warehouse-pvz" : "warehouse-door",
                null,
                null,
                null,
                raw.rawJson()
        );
    }

    @Override
    public ShipmentResult syncStatus(String cdekOrderUuid) {
        ensureReady();
        CdekClient.CdekOrderRaw raw = client.getCdekOrder(cdekOrderUuid);
        return parseOrderInfo(cdekOrderUuid, raw.rawJson());
    }

    @Override
    public void cancelShipment(String cdekOrderUuid) {
        ensureReady();
        client.deleteCdekOrder(cdekOrderUuid);
        log.info("CDEK real: отменён заказ uuid={}", cdekOrderUuid);
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private void ensureReady() {
        if (!auth.isConfigured()) {
            throw new BusinessRuleException(
                    "CDEK real-провайдер выбран, но не заданы CDEK_CLIENT_ID / CDEK_CLIENT_SECRET");
        }
    }

    /** Сборка тела {@code POST /v2/orders} по документации CDEK v2. */
    private Map<String, Object> buildOrderBody(ShipmentRequest r) {
        Map<String, Object> body = new HashMap<>();
        if (r.tariffCode() != null) {
            body.put("tariff_code", r.tariffCode());
        }
        if (props.senderCity() != null) {
            body.put("from_location", Map.of("code", props.senderCity()));
        }
        // Доставка до ПВЗ (delivery_point) либо до города получателя (to_location).
        if (r.pvzCode() != null && !r.pvzCode().isBlank()) {
            body.put("delivery_point", r.pvzCode());
        } else if (r.toCityCode() != null) {
            body.put("to_location", Map.of("code", r.toCityCode()));
        }

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("name", r.recipientName());
        if (r.recipientPhone() != null) {
            recipient.put("phones", List.of(Map.of("number", r.recipientPhone())));
        }
        body.put("recipient", recipient);

        Map<String, Object> pkg = new HashMap<>();
        pkg.put("number", "1");
        pkg.put("weight", r.weightGrams());
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of(
                "name", "BALGYN заказ №" + r.orderId(),
                "ware_key", "ORDER-" + r.orderId(),
                "payment", Map.of("value", 0),
                "cost", r.deliveryPrice() == null ? 0 : r.deliveryPrice(),
                "weight", r.weightGrams(),
                "amount", 1
        ));
        pkg.put("items", items);
        body.put("packages", List.of(pkg));

        body.put("number", "BALGYN-" + r.orderId());
        return body;
    }

    /** Разбор {@code GET /v2/orders/{uuid}}: трек-номер + последний статус. */
    private ShipmentResult parseOrderInfo(String uuid, String rawJson) {
        String track = null;
        CdekShipmentStatus status = null;
        LocalDate eta = null;
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode entity = root.get("entity");
            if (entity != null) {
                if (entity.hasNonNull("cdek_number")) {
                    track = entity.get("cdek_number").asText();
                }
                JsonNode statuses = entity.get("statuses");
                if (statuses != null && statuses.isArray() && !statuses.isEmpty()) {
                    // последний по списку — самый свежий
                    JsonNode last = statuses.get(statuses.size() - 1);
                    if (last.hasNonNull("code")) {
                        status = CdekStatusMapper.map(last.get("code").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("CDEK real: не удалось разобрать ответ /orders/{}: {}", uuid, e.getMessage());
        }
        return new ShipmentResult(uuid, track, status, null, eta, null, null, rawJson);
    }
}
