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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реальный провайдер CDEK API v2 поверх {@link CdekClient}.
 *
 * <p>Тело запроса POST /v2/orders собрано по официальной документации:
 * <ul>
 *   <li>tariff_code    — обязателен; берётся из отправления или CDEK_DEFAULT_TARIFF (конфиг).</li>
 *   <li>from_location  — код города-отправителя (CDEK_SENDER_CITY).</li>
 *   <li>delivery_point — код ПВЗ при доставке до пункта выдачи.</li>
 *   <li>to_location    — код города-получателя при доставке до двери.</li>
 *   <li>recipient      — имя, телефон (нормализован в +7…), e-mail.</li>
 *   <li>packages[]     — один пакет; weight = сумма (единица × кол-во), + опциональные габариты.</li>
 *   <li>packages[].items[] — каждая позиция заказа: name, ware_key, cost (цена), weight, amount.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealCdekProvider implements DeliveryProvider {

    // Минимальные значения габаритов (CDEK требует > 0; защита от ошибок данных)
    private static final int PKG_MIN_DIM_CM = 1;

    private final CdekClient client;
    private final CdekAuthService auth;
    private final CdekProperties props;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isMock() { return false; }

    @Override
    public String name() { return "real-cdek"; }

    // ── createShipment ────────────────────────────────────────────────────────────

    @Override
    public ShipmentResult createShipment(ShipmentRequest request) {
        ensureReady();
        Map<String, Object> body = buildOrderBody(request);
        CdekClient.CdekOrderRaw raw = client.createCdekOrder(body);
        log.info("[CDEK] Заказ создан uuid={} order={}", raw.uuid(), request.orderId());
        return new ShipmentResult(
                raw.uuid(),
                null, // трек-номер появляется асинхронно — заберём через syncStatus/webhook
                CdekShipmentStatus.CREATED,
                request.pvzCode() != null ? "warehouse-pvz" : "warehouse-door",
                null,
                null,
                null,
                raw.rawJson()
        );
    }

    // ── syncStatus ────────────────────────────────────────────────────────────────

    @Override
    public ShipmentResult syncStatus(String cdekOrderUuid) {
        ensureReady();
        CdekClient.CdekOrderRaw raw = client.getCdekOrder(cdekOrderUuid);
        return parseOrderInfo(cdekOrderUuid, raw.rawJson());
    }

    // ── cancelShipment ────────────────────────────────────────────────────────────

    @Override
    public void cancelShipment(String cdekOrderUuid) {
        ensureReady();
        client.deleteCdekOrder(cdekOrderUuid);
        log.info("[CDEK] Заказ отменён uuid={}", cdekOrderUuid);
    }

    // ── request building ─────────────────────────────────────────────────────────

    /**
     * Сборка тела {@code POST /v2/orders} по документации CDEK API v2.
     *
     * <pre>
     * {
     *   "tariff_code": 136,
     *   "number": "BALGYN-42",
     *   "from_location": { "code": 270 },
     *   "delivery_point": "AAA"           // или to_location при доставке до двери
     *   "recipient": { "name": "…", "phones": [{"number":"+7…"}], "email": "…" },
     *   "packages": [{
     *     "number": "1",
     *     "weight": 1000,
     *     "length": {r.lengthCm}, "width": {r.widthCm}, "height": {r.heightCm},
     *     "items": [
     *       { "name":"…", "ware_key":"DG-1", "payment":{"value":0},
     *         "cost": 15000, "weight": 1000, "amount": 1 }
     *     ]
     *   }]
     * }
     * </pre>
     */
    private Map<String, Object> buildOrderBody(ShipmentRequest r) {
        Map<String, Object> body = new HashMap<>();

        // ── tariff_code (обязателен) ──────────────────────────────────────────
        Integer tariff = r.tariffCode() != null ? r.tariffCode() : props.defaultTariff();
        if (tariff != null) {
            body.put("tariff_code", tariff);
        } else {
            log.warn("[CDEK-BUILD] tariff_code не задан — запрос будет отклонён (order #{})", r.orderId());
        }

        // ── Номер заказа (наш) ────────────────────────────────────────────────
        body.put("number", "BALGYN-" + r.orderId());

        // ── Город отправителя ─────────────────────────────────────────────────
        if (props.senderCity() != null) {
            body.put("from_location", Map.of("code", props.senderCity()));
        } else {
            log.warn("[CDEK-BUILD] CDEK_SENDER_CITY не задан (order #{})", r.orderId());
        }

        // ── Пункт назначения (ПВЗ или город-получатель) ───────────────────────
        if (r.pvzCode() != null && !r.pvzCode().isBlank()) {
            body.put("delivery_point", r.pvzCode());
        } else if (r.toCityCode() != null) {
            body.put("to_location", Map.of("code", r.toCityCode()));
        } else {
            log.warn("[CDEK-BUILD] Нет pvzCode и toCityCode для заказа #{} — запрос будет отклонён", r.orderId());
        }

        // ── Получатель ────────────────────────────────────────────────────────
        Map<String, Object> recipient = new HashMap<>();
        recipient.put("name", r.recipientName() != null ? r.recipientName() : "");
        String phone = normalizePhone(r.recipientPhone());
        if (phone != null) {
            recipient.put("phones", List.of(Map.of("number", phone)));
        } else {
            log.warn("[CDEK-BUILD] Телефон получателя не задан или не нормализован для заказа #{}", r.orderId());
        }
        if (r.recipientEmail() != null && !r.recipientEmail().isBlank()) {
            recipient.put("email", r.recipientEmail());
        }
        body.put("recipient", recipient);

        // ── Пакеты ────────────────────────────────────────────────────────────
        body.put("packages", List.of(buildPackage(r)));

        return body;
    }

    private Map<String, Object> buildPackage(ShipmentRequest r) {
        Map<String, Object> pkg = new HashMap<>();
        pkg.put("number", "1");
        pkg.put("weight", Math.max(r.weightGrams(), 1));
        pkg.put("length", Math.max(r.lengthCm(), PKG_MIN_DIM_CM));
        pkg.put("width",  Math.max(r.widthCm(),  PKG_MIN_DIM_CM));
        pkg.put("height", Math.max(r.heightCm(), PKG_MIN_DIM_CM));
        pkg.put("items",  buildCdekItems(r));
        return pkg;
    }

    /**
     * Строит массив items[] для пакета.
     * Каждая позиция заказа → отдельная строка с реальными name/ware_key/cost/weight/amount.
     * {@code payment.value = 0} — наложенный платёж отсутствует.
     */
    private List<Map<String, Object>> buildCdekItems(ShipmentRequest r) {
        List<Map<String, Object>> items = new ArrayList<>();
        List<OrderItemSnapshot> snapshots = r.items();
        if (snapshots == null || snapshots.isEmpty()) {
            // Fallback — одна синтетическая строка, чтобы CDEK не отклонил пустой массив
            items.add(Map.of(
                    "name",     "BALGYN заказ №" + r.orderId(),
                    "ware_key", "ORDER-" + r.orderId(),
                    "payment",  Map.of("value", 0),
                    "cost",     0,
                    "weight",   Math.max(r.weightGrams(), 1),
                    "amount",   1
            ));
            return items;
        }
        for (OrderItemSnapshot snap : snapshots) {
            Map<String, Object> item = new HashMap<>();
            item.put("name",    snap.name());
            item.put("ware_key", snap.wareKey());
            item.put("payment", Map.of("value", 0)); // без наложенного платежа
            // cost = объявленная стоимость единицы; BigDecimal → int (CDEK принимает только целое)
            int costKzt = snap.price() != null
                    ? snap.price().setScale(0, java.math.RoundingMode.HALF_UP).intValue()
                    : 0;
            item.put("cost",   costKzt);
            item.put("weight", Math.max(snap.weightGrams(), 1));
            item.put("amount", snap.quantity());
            items.add(item);
        }
        return items;
    }

    // ── phone normalization ───────────────────────────────────────────────────────

    /**
     * Приводит телефон к формату, который принимает СДЭК: {@code +7XXXXXXXXXX}.
     * <ul>
     *   <li>11 цифр, начинается с 7 → {@code +7…}</li>
     *   <li>11 цифр, начинается с 8 → заменяем 8 на {@code +7}</li>
     *   <li>10 цифр → добавляем {@code +7}</li>
     *   <li>уже начинается с {@code +} после чистки → возвращаем {@code +digits}</li>
     * </ul>
     */
    static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        boolean hadPlus = raw.trim().startsWith("+");
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        if (digits.length() == 11) {
            if (digits.startsWith("7")) return "+" + digits;
            if (digits.startsWith("8")) return "+7" + digits.substring(1);
        }
        if (digits.length() == 10) return "+7" + digits;
        if (hadPlus) return "+" + digits;
        return raw; // неизвестный формат — отправляем как есть, CDEK сообщит об ошибке
    }

    // ── response parsing ─────────────────────────────────────────────────────────

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
                    JsonNode last = statuses.get(statuses.size() - 1);
                    if (last.hasNonNull("code")) {
                        status = CdekStatusMapper.map(last.get("code").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[CDEK] Не удалось разобрать ответ /orders/{}: {}", uuid, e.getMessage());
        }
        return new ShipmentResult(uuid, track, status, null, eta, null, null, rawJson);
    }

    // ── guards ────────────────────────────────────────────────────────────────────

    private void ensureReady() {
        if (!auth.isConfigured()) {
            throw new BusinessRuleException(
                    "CDEK real-провайдер выбран, но не заданы CDEK_CLIENT_ID / CDEK_CLIENT_SECRET");
        }
    }
}
