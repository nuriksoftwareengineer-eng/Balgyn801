# DRY_RUN_CDEK.md
> Сценарий: CDEK — создание отправления → webhook → доставка  
> Дата: 2026-06-21

---

## УЧАСТНИКИ СИСТЕМЫ

| Компонент | Роль |
|-----------|------|
| AdminOrderDetailPage | UI создания отправления |
| CdekShipmentController | Admin API: create/sync/cancel |
| CdekShipmentServiceImpl | Бизнес-логика: запросы к CDEK API |
| CdekWebhookController | `POST /delivery/cdek/webhook` |
| CdekWebhookServiceImpl | Обрабатывает событие, обновляет CdekShipment |
| CdekShipmentRepository | `findByCdekOrderUuid()` |

---

## ПРЕДУСЛОВИЯ

- Заказ #42 оформлен с `deliveryType = CDEK`
- `pvzCode` = "MSK19" (код ПВЗ, выбранный при checkout)
- `address.city = "Москва, Московская область"`, `address.street = "СДЭК ПВЗ «Офис» [MSK19]: ул. Ленина 1"`
- Заказ оплачен: `status = NEW` → admin переводит в `CONFIRMED`
- CDEK OAuth токен получен (FreedomPayProperties → CdekProperties → cached 24h)

---

## ШАГ 1 — Admin создаёт CDEK-отправление

**URL:** `/admin/orders/42`

**Admin нажимает кнопку "Создать отправление CDEK":**

```
window.confirm("Создать отправление CDEK? Это выставит счёт через CDEK API.")
→ [OK]
```

**Запрос:**
```
POST /api/v1/cdek-shipment/by-order/42/create
Authorization: Bearer <admin_token>
```

**CdekShipmentServiceImpl.createShipment(orderId: 42):**
1. Загружает Order #42 (с deliveryAddress, orderItems)
2. Получает `pvzCode = "MSK19"` из Order
3. Строит CDEK request: `CdekOrderRequest` с:
   - `to_location.code` = CDEK city code (из pvzCode)
   - `delivery_point` = "MSK19"
   - `packages[]` = рассчитанные размеры/вес из `GarmentWeightService`
   - `recipient` = из `deliveryAddress.recipientName` + `recipientPhone`
   - `sender` = из настроек (CDEK sender settings)
4. Вызывает CDEK API v2: `POST https://api.cdek.ru/v2/orders`
5. Получает ответ: `{ entity: { uuid: "CDEK-UUID-123", number: "123456789" } }`
6. Сохраняет `CdekShipment { orderId: 42, cdekOrderUuid: "CDEK-UUID-123", trackingNumber: "123456789", status: CREATED }`
7. Обновляет `Order.status = SHIPPED`

**Ответ API:**
```json
{
  "id": 7,
  "orderId": 42,
  "cdekOrderUuid": "CDEK-UUID-123",
  "trackingNumber": "123456789",
  "status": "CREATED",
  "createdAt": "2026-06-21T10:30:00Z"
}
```

**Визуально:**
- Секция CDEK в AdminOrderDetailPage показывает: Статус "Создано", Номер "123456789"
- Кнопка меняется на "Повторить создание отправления CDEK?"
- Order статус обновляется до "SHIPPED"

---

## ШАГ 2 — CDEK обрабатывает заказ

CDEK принимает заказ, клиент сдаёт посылку в ПВЗ отправитель.

CDEK начинает присылать webhook-события:
1. `ORDER_STATUS` с `status.code = ACCEPTED` (принято к перевозке)
2. `ORDER_STATUS` с `status.code = IN_TRANSIT` (в пути)
3. `ORDER_STATUS` с `status.code = ARRIVED` (прибыло в ПВЗ назначения)
4. `ORDER_STATUS` с `status.code = DELIVERED` (получено клиентом)

---

## ШАГ 3 — CDEK отправляет Webhook

**Запрос от CDEK:**
```
POST /api/v1/delivery/cdek/webhook
X-Authorization: {cdek.webhook-token}
Content-Type: application/json

{
  "type": "ORDER_STATUS",
  "date_time": "2026-06-22T15:30:00+0300",
  "uuid": "CDEK-UUID-123",
  "attributes": {
    "id": "CDEK-UUID-123",
    "status": {
      "code": "IN_TRANSIT",
      "name": "В пути",
      "date_time": "2026-06-22T15:00:00+0300"
    }
  }
}
```

**CdekWebhookController:**

```java
// 1. Token verification
if (webhookToken == null || webhookToken.isBlank()) {
    log.warn("webhook-token not configured — accepting unsigned webhook");
} else {
    if (!webhookToken.equals(received)) {
        throw new ResponseStatusException(UNAUTHORIZED, "Invalid CDEK webhook token");
    }
}

// 2. Process
boolean updated = webhookService.handle(request);
return Map.of("ok", true, "updated", updated);
```

**CdekWebhookServiceImpl.handle(request):**
```java
// NEW: O(1) lookup вместо findAll() (исправлено в P1-7)
Optional<CdekShipment> shipmentOpt = shipmentRepository.findByCdekOrderUuid(request.uuid());

if (shipmentOpt.isEmpty()) {
    log.warn("[CDEK] Shipment not found for uuid={}", request.uuid());
    return false;
}

CdekShipment shipment = shipmentOpt.get();
CdekShipmentStatus newStatus = mapStatus(request.attributes().status().code());
shipment.setStatus(newStatus);
// Optionally update Order.status if status = DELIVERED
if (newStatus == CdekShipmentStatus.DELIVERED) {
    orderRepository.findById(shipment.getOrderId())
        .ifPresent(o -> o.setStatus(OrderStatus.DELIVERED));
}
shipmentRepository.save(shipment);
return true;
```

**Ответ:** `{ "ok": true, "updated": true }`
CDEK получает 200 и прекращает ретраи для этого события.

---

## ШАГ 4 — Admin синхронизирует статус вручную

Если webhook не пришёл (сетевые проблемы), admin может нажать "Синхронизировать":

```
POST /api/v1/cdek-shipment/by-order/42/sync
→ GET https://api.cdek.ru/v2/orders/{cdekOrderUuid}
→ Обновляет CdekShipment.status из ответа CDEK API
```

---

## ШАГ 5 — Заказ доставлен

**CDEK присылает webhook с `status.code = DELIVERED`:**

- `CdekShipment.status = DELIVERED`
- `Order.status = DELIVERED`

**Admin видит в AdminOrderDetailPage:**
- Статус заказа: "Доставлено" (зелёный badge)
- CDEK статус: "Доставлено"

---

## EDGE CASES

### Webhook пришёл для неизвестного UUID

```java
shipmentRepository.findByCdekOrderUuid(request.uuid())  // → Optional.empty()
return false;  // ok=true, updated=false
```

CDEK видит 200 → не ретраит. Лог предупреждает.

### Webhook пришёл с неверным токеном

```java
throw new ResponseStatusException(UNAUTHORIZED, "Invalid CDEK webhook token");
// → HTTP 401
```

CDEK будет ретраить. Если токен не настроен (`cdek.webhook-token` пустой) → warn + accept (dev режим).

### CDEK API недоступен при создании отправления

```
POST /api/v1/cdek-shipment/by-order/42/create
→ HTTP 502 от CDEK
→ CdekShipmentServiceImpl throws exception
→ AdminOrderDetailPage: setShipmentError("Не удалось выполнить операцию")
→ Показывается в UI
```

Admin может попробовать снова (кнопка остаётся доступной).

### Повторное создание отправления (cdekOrderUuid уже есть)

```
window.confirm("Повторить создание отправления CDEK?")  // другой текст!
→ POST /cdek-shipment/by-order/42/create
→ CdekShipmentServiceImpl создаёт НОВЫЙ CdekShipment или обновляет существующий
```

⚠️ **ПРЕДУПРЕЖДЕНИЕ**: При повторном create нужно убедиться, что старое отправление в CDEK отменяется, иначе два заказа могут идти параллельно.

---

## ПРОВЕРКА СТАТУСОВ

| Событие | Order.status | CdekShipment.status |
|---------|-------------|---------------------|
| Создание отправления | SHIPPED | CREATED |
| Webhook ACCEPTED | SHIPPED | ACCEPTED |
| Webhook IN_TRANSIT | SHIPPED | IN_TRANSIT |
| Webhook ARRIVED | SHIPPED | ARRIVED |
| Webhook DELIVERED | DELIVERED | DELIVERED |
| Webhook RETURNED | SHIPPED | RETURNED |

---

## ВИЗУАЛЬНЫЙ ПУТЬ (summary)

```
Admin: /admin/orders/42
  → [Создать отправление] → window.confirm → OK
  → POST /cdek-shipment/by-order/42/create
  → CDEK API: POST /v2/orders
  ← cdekOrderUuid="UUID-123", trackingNumber="123"
  ← CdekShipment saved, Order → SHIPPED

CDEK Processing:
  → POST /delivery/cdek/webhook (X-Authorization: token)
  → findByCdekOrderUuid("UUID-123")  ← O(1) lookup (fixed!)
  → CdekShipment.status = IN_TRANSIT
  ← 200 { ok: true, updated: true }

  → POST /delivery/cdek/webhook
  → status = DELIVERED
  → Order.status = DELIVERED
  ← 200

Admin sees: заказ #42 [DELIVERED ✓]
```

**СТАТУС: ПРОХОДИТ ✅ (с предупреждением о повторном create выше)**
