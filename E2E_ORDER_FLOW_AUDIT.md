# E2E Order Flow Audit — BALGYN

Checked: 2026-06-20  
Scope: full path выбор товара → корзина → заказ → платёж → webhook → статус → админка  
Method: чтение реальных файлов, трассировка вызовов методов и таблиц БД.

---

## Этап 1 — Выбор товара → Корзина

### Фронтенд
| Шаг | Код | Что происходит |
|-----|-----|----------------|
| Пользователь выбирает дизайн, тип одежды, цвет, размер | `DesignPage.tsx` | Читает `stockMap: colorId → sizeId → qty`, фильтрует OOS размеры |
| «В корзину» | `cart.store.ts:addItem()` | Записывает в `localStorage["balgyn-cart-v1"]` через Zustand `persist` |

### Структура элемента корзины
```ts
{ designGarmentId, colorId, sizeId, qty, unitPrice, designName, garmentName, colorHex }
```

**`unitPrice` — снапшот на момент добавления.** Если администратор изменит цену между добавлением и оформлением, отображаемая сумма будет неверной. Однако backend при создании заказа читает цену из `design_garment_prices` заново — финансового риска нет, только UX-расхождение.

### Таблицы БД на этом этапе
Нет — только `localStorage`.

---

## Этап 2 — Checkout wizard (CartPage.tsx)

5-шаговый wizard (React state, НЕ персистентен):

| Шаг | Поля | Валидация |
|-----|------|-----------|
| 1 Контакты | `customerName`, `customerPhone`, `telegramUsername` | name + phone обязательны |
| 2 Страна | `countryIso2` | — |
| 3 Тип доставки | `deliveryType` | из `getDeliveryMethods(countryIso2)` |
| 4 Адрес | address / CDEK-город + ПВЗ | `requiresAddress` определяется через `DeliveryMethodResponse.requiresAddress` |
| 5 Комментарий | `comment` | — → **кнопка «Разместить заказ»** |

---

## Этап 3 — Создание заказа

### Фронтенд
```
CartPage.tsx:861  useMutation({ mutationFn: (body) => createOrder(body, token) })
```

`POST /api/v1/order` с телом `CreateOrderRequest`:
```json
{
  "customerName": "...", "customerPhone": "...", "telegramUsername": "...",
  "deliveryType": "CDEK", "countryIso2": "KZ", "pvzCode": "...",
  "items": [{ "designGarmentId": 5, "colorId": 2, "sizeId": 3, "quantity": 1, "currency": "KZT" }],
  "address": { ... }
}
```
> Цена НЕ передаётся — backend вычисляет сам. ✅

### Backend (OrderServiceImpl.createOrder)
```
@Transactional
createOrder(CreateOrderRequest, AppUser)
  ├── customerRepository.save(customer)        → INSERT customers
  ├── orderRepository.save(order, status=PENDING_PAYMENT) → INSERT orders
  ├── для каждого item:
  │   ├── designGarmentRepository.findById()   → SELECT design_garments
  │   ├── проверка colorId принадлежит garment → SELECT design_garment_colors
  │   ├── проверка sizeId принадлежит garment  → SELECT design_garment_sizes
  │   ├── inventoryRepository.findAndLockByGarmentColorSize()  ← SELECT FOR UPDATE (3 s timeout)
  │   ├── if qty < requested → throw InsufficientInventoryException → rollback
  │   ├── inventory.quantity -= requested; inventoryRepository.save() → UPDATE inventory
  │   ├── designGarmentPriceRepository.findByDesignGarment_IdAndCurrency() → SELECT prices
  │   └── orderItemRepository.save(orderItem) → INSERT order_items
  ├── deliveryPricingService.quote()           → SELECT delivery_settings, delivery_tariffs, exchange_rates
  ├── orderRepository.save(totalPrice, deliveryFee, shippingZone, totalWeightKg) → UPDATE orders
  ├── deliveryAddressRepository.save()         → INSERT delivery_addresses
  └── orderHistoryRepository.save(PENDING_PAYMENT) → INSERT order_history
```

### Фронтенд при успехе (CartPage.tsx:863-882)
```ts
onSuccess: (order) => {
  clear()              // ← localStorage["balgyn-cart-v1"] очищается СРАЗУ
  setCompletedOrder(order)  // ← хранится ТОЛЬКО в React state
  setPhase("cart")
  setStep(1)
}
```

### ⚠️ КРИТИЧНО — Заказ потеряется при закрытии вкладки
После успешного создания:
- `completedOrder` хранится только в памяти React
- `clear()` уже выполнен — корзина пуста
- Если пользователь закрывает вкладку **до нажатия кнопки «Оплатить»**:
  - Заказ существует в `orders` (status=`PENDING_PAYMENT`), инвентарь зарезервирован
  - Пользователь не знает ID заказа, не может вернуться к оплате
  - Через 60 минут `OrderExpiryService` освободит инвентарь и выставит `EXPIRED`
  - Залогиненный пользователь может найти заказ в `/orders` — но кнопки «Оплатить» там нет

### ⚠️ КРИТИЧНО — `completedOrder` исчезает при F5
Перезагрузка страницы после `onSuccess` → `completedOrder = null` → форма оплаты исчезает → пользователь теряет возможность оплатить текущий заказ.

---

## Этап 4 — Инициализация платежа

### Freedom Pay (CartPage.tsx:977-984)
```
initPayment({ orderId, provider: "FREEDOM_PAY", returnUrl }) → POST /api/v1/payments/init
```

**Backend (PaymentServiceImpl.initPayment)**
```
├── orderRepository.findById(orderId)
├── paymentRepository.findByOrderAndProviderAndStatus(order, FREEDOM_PAY, PENDING)
│   → если уже PENDING: возвращает существующий (идемпотентность) ✅
├── freedomPayClient.initPayment() → HTTP POST к api.freedompay.kz/init_payment.php
│   → парсинг XML, проверка MD5-подписи
├── paymentRepository.save(payment, status=PENDING) → INSERT payments
└── return PaymentResponse { paymentUrl }
```

### PayPal (CartPage.tsx:968-975)
```
createPayPalOrder({ orderId, returnUrl, cancelUrl }) → POST /api/v1/payments/paypal/create-order
```

**Backend (PayPalServiceImpl.createOrder)**
```
├── orderRepository.findById(orderId)
├── paymentRepository.findByOrderAndProviderAndStatus(order, PAYPAL, PENDING)  ← идемпотентность ✅
├── exchangeRateService.kztPerUsd() → SELECT exchange_rates
├── amountUsd = order.totalPrice / kztPerUsd  ← курс фиксируется здесь
├── payPalOrdersClient.createOrder(amountUsd, "USD", returnUrl, cancelUrl) → PayPal API /v2/checkout/orders
├── paymentRepository.save(payment, providerPaymentId=ppOrder.id) → INSERT payments
└── return PaymentResponse { paymentUrl }
```

### Редирект
```ts
CartPage.tsx:987  window.location.href = paymentUrl  // пользователь покидает сайт
```

Перед редиректом:
```ts
sessionStorage.setItem("balgyn_last_payment", JSON.stringify({ orderId, totalPrice, provider }))
```
Используется только для отображения деталей на страницах success/failed/cancelled. **Не позволяет возобновить оплату.**

---

## Этап 5a — Freedom Pay (возврат и webhook)

### Пользователь оплачивает → Freedom Pay вызывает callback
```
POST /api/v1/payments/callback/freedom-pay
```

**Backend (FreedomPayCallbackController → PaymentServiceImpl.handleFreedomPayCallback)**
```
├── verifySignature(params)    → MD5(scriptName;sorted_values;secretKey) constantTime ✅
├── if invalid sig → return XML "rejected"
├── processedEventRepository.existsByProviderAndEventId(FREEDOM_PAY, pg_payment_id)
│   → if exists → return XML "ok" (replay protection) ✅
├── paymentRepository.findByProviderPaymentId(pg_payment_id) → SELECT payments
├── amount validation
├── mapFreedomPayResult("1" → SUCCEEDED, "0" → FAILED, else → PENDING)
├── if SUCCEEDED:
│   ├── payment.status = SUCCEEDED → UPDATE payments
│   ├── order.status = CONFIRMED → UPDATE orders
│   └── processedEventRepository.save() → INSERT processed_webhook_events
├── if FAILED/CANCELLED:
│   ├── orderExpiryService.expire(order)  → EXPIRED + инвентарь возвращается
│   └── processedEventRepository.save()
└── return XML "ok"
```

### Затем Freedom Pay редиректит браузер на success/failed URL

### ⚠️ Страница успеха показывается ДО подтверждения
Freedom Pay может перенаправить браузер на `success_url` **раньше** чем придёт callback.
`PaymentSuccessPage.tsx` читает `sessionStorage["balgyn_last_payment"]` и показывает "Оплачено" —
но `order.status` в этот момент может быть всё ещё `PENDING_PAYMENT`.

---

## Этап 5b — PayPal (возврат и webhook)

### Пользователь одобряет → PayPal редиректит на returnUrl
```
GET /payment-return?token=<paypalOrderId>
```

**PaymentReturnPage.tsx (загружается)**
```ts
useEffect(() => {
  const token = searchParams.get("token")
  capturePayPalOrder(token)  // → POST /api/v1/payments/paypal/capture/{token}
}, [])
```

### ⚠️ Capture вызывается при каждой загрузке страницы
Нажатие «назад» или F5 на `/payment-return` запускает повторный capture PayPal API.
Защита через `findByProviderPaymentIdForUpdate()` + проверка `status != PENDING` ✅
Но каждый раз делает HTTP-запрос к PayPal — лишняя нагрузка.

**Backend (PayPalServiceImpl.captureOrder)**
```
├── paymentRepository.findByProviderPaymentIdForUpdate(paypalOrderId)  ← SELECT FOR UPDATE ✅
├── if payment.status != PENDING → return current status (идемпотентность) ✅
├── payPalOrdersClient.captureOrder(paypalOrderId) → PayPal API POST /v2/checkout/orders/{id}/capture
├── if captureStatus == COMPLETED:
│   ├── amount validation (tolerance 0.02 USD) ✅
│   ├── payment.status = SUCCEEDED; order.status = CONFIRMED → UPDATE payments, orders
│   └── navigate to /payment/success
└── else:
    ├── payment.status = FAILED
    ├── orderExpiryService.expire(order) → EXPIRED + инвентарь возвращается
    └── navigate to /payment/failed
```

### PayPal также присылает webhook
```
POST /api/v1/payments/paypal/webhook
```
**Backend (PayPalServiceImpl.handleWebhook)**
```
├── PayPalWebhookVerifier.verify() → RSA-SHA256 через PayPal API /v1/notifications/verify-webhook-signature ✅
├── processedEventRepository replay protection ✅
├── PAYMENT.CAPTURE.COMPLETED → handleCaptureCompleted()
│   ├── paymentRepository.findByProviderPaymentId()  ← БЕЗ блокировки (в отличие от capture)
│   ├── if payment.status != PENDING → ничего не делает
│   ├── payment.status = SUCCEEDED; order.status = CONFIRMED → UPDATE payments, orders
│   └── processedEventRepository.save()
└── PAYMENT.CAPTURE.REFUNDED → handleCaptureRefunded()
    ├── payment.status = REFUNDED → UPDATE payments
    └── order.status НЕ меняется ← ⚠️
```

### ⚠️ REFUNDED не обновляет статус заказа
`handleCaptureRefunded()` (строка 284–297 PayPalServiceImpl.java): только `payment.status = REFUNDED`.
`order.status` остаётся `CONFIRMED`/`PROCESSING`/`SHIPPED`.
Администратор не получает никакого сигнала о возврате — должен вручную проверять платежи.

### ⚠️ Отмена PayPal — retry невозможен
```
/payment/cancelled?token=<paypalOrderId>
PaymentCancelledPage.tsx:44  POST /api/v1/payments/paypal/cancel/{token}
```
→ `PayPalServiceImpl.cancelOrder()` → `orderExpiryService.expire(order)`:
- `order.status = EXPIRED` → UPDATE orders
- инвентарь возвращается → UPDATE inventory

Кнопка «Повторить» ведёт на `/cart` — **корзина уже очищена на этапе 3**.
Пользователь видит пустую корзину и не может повторить оплату без ручного добавления товаров заново.

---

## Этап 6 — Изменение статуса (Admin)

### Список заказов
```
GET /api/v1/order  → OrderServiceImpl.getAll()
  → orderRepository.findByStatusNotInOrderByCreatedAtDesc([PENDING_PAYMENT, EXPIRED])
```
Новый заказ появляется в admin **мгновенно** после смены статуса на `CONFIRMED` (через webhook/capture). До этого — не виден.

### Смена статуса администратором
```
PATCH /api/v1/order/{id}  → OrderServiceImpl.updateOrderStatus()
  ├── assertAllowedStatusTransition(current, next)
  │   ← блокирует только terminal states: CANCELLED, DELIVERED, EXPIRED
  │   ← НЕ проверяет порядок переходов (SHIPPED → CONFIRMED разрешён)
  ├── if next == CANCELLED:
  │   ├── orderExpiryService.releaseInventory(order) → инвентарь возвращается
  │   └── orderExpiryService.cancelPendingPayments(order) → UPDATE payments
  ├── order.status = next → UPDATE orders
  └── orderHistoryRepository.save() → INSERT order_history
```

### ⚠️ Переходы не строго упорядочены
Допустимы переходы назад: `SHIPPED → PROCESSING`, `PROCESSING → CONFIRMED`.  
Допустим пропуск этапов: `CONFIRMED → DELIVERED`.  
Корозии инвентаря не возникает (инвентарь освобождается только при CANCELLED),
но история заказа будет неконсистентной.

---

## Этап 7 — OrderExpiryService (фоновая задача)

```java
@Scheduled(fixedDelay = 300_000, initialDelay = 60_000)  // каждые 5 мин
expireStaleOrders():
  cutoff = now() - paymentWindowMinutes  // default: 60 минут
  orders = orderRepository.findByStatusAndCreatedAtBefore(PENDING_PAYMENT, cutoff)
  for each order: expire(order)
    ├── releaseInventory(order)
    │   → для каждого OrderItem:
    │     inventoryRepository.findAndLockByGarmentColorSize()  ← SELECT FOR UPDATE
    │     inventory.quantity += item.quantity → UPDATE inventory
    ├── cancelPendingPayments(order) → UPDATE payments (PENDING → CANCELLED)
    ├── order.status = EXPIRED → UPDATE orders
    └── orderHistoryRepository.save() → INSERT order_history
```

### ⚠️ Двойное освобождение инвентаря (race condition)
`expire(order)` может быть вызван одновременно из двух потоков:
1. `expireStaleOrders()` (scheduler)
2. `handleFreedomPayCallback()` при FAILED результате

Сценарий:
1. Поток A (callback) читает `order.status = PENDING_PAYMENT` → вызывает `releaseInventory()`, получает SELECT FOR UPDATE на inventory, добавляет qty
2. Поток B (scheduler) ждёт блокировки → получает после Потока A → снова добавляет qty

Результат: инвентарь увеличивается **дважды** — фантомные товары в наличии.

**Защиты от этого нет**: `releaseInventory()` не проверяет `order.status` перед изменением инвентаря. SELECT FOR UPDATE на inventory-строке предотвращает параллельный доступ к одной строке, но не предотвращает **последовательные** вызовы.

---

## Этап 8 — Rate Limiter

```java
PaymentRateLimiterFilter.java
  Endpoints: /api/v1/payments/init, /callback/**, /paypal/create-order, /paypal/capture/**, /paypal/webhook
  Лимиты: init=10/min, webhook=100/min (per IP)
  Хранилище: ConcurrentHashMap<String, Deque<Instant>>  ← in-memory
```

### ⚠️ X-Forwarded-For читается с конца
```java
parts[parts.length - 1].trim()  // последний элемент
```
Стандарт: `X-Forwarded-For: client, proxy1, proxy2` — клиент **первый**.
В конфигурации с одним nginx всё работает корректно (nginx ставит `$remote_addr` без предыдущих proxy).
Но при наличии CDN или нескольких проксей rate-limit применяется к адресу промежуточного прокси, а не клиента.
Кроме того, заголовок можно подделать: `X-Forwarded-For: victim-ip, attacker-real-ip` → rate-limit применится к `attacker-real-ip` (последний), а квота `victim-ip` сгорит.

### ⚠️ In-memory buckets сбрасываются при перезапуске
Перезапуск контейнера → все счётчики rate-limit обнуляются → минутное окно для атаки.

---

## Сводка: где заказ может потеряться или зависнуть

| # | Сценарий | Последствие | Severity |
|---|----------|-------------|----------|
| 1 | Пользователь закрывает вкладку после создания заказа, до оплаты | `PENDING_PAYMENT` + зарезервированный инвентарь; пользователь не знает ID; через 60 мин → `EXPIRED` | CRITICAL |
| 2 | F5 на CartPage после `orderMutation.onSuccess` | `completedOrder = null`, форма оплаты исчезает | CRITICAL |
| 3 | PayPal cancel → пользователь жмёт «Повторить» | Корзина пуста, заказ уже `EXPIRED` | HIGH |
| 4 | Freedom Pay redirect success ДО callback | Пользователь видит «Оплачено», `order.status = PENDING_PAYMENT` | HIGH |
| 5 | PayPal REFUNDED webhook | `payment.status = REFUNDED`, `order.status` не меняется → admin не видит | HIGH |
| 6 | Двойной вызов `expire()` (callback + scheduler) | Инвентарь освобождается дважды → фантомный stock | HIGH |
| 7 | Статус отменяется назад (admin) | Некорректная history, хотя инвентарь не повреждён | MEDIUM |
| 8 | X-Forwarded-For spoofing | Rate-limit обходится при наличии CDN/multi-proxy | MEDIUM |
| 9 | Exchange rate меняется между заказом и оплатой PayPal | Сумма KZT в интерфейсе ≠ сумма USD в PayPal (tolerance 0.02 USD) | LOW |
| 10 | CDEK shipment не создаётся автоматически | Admin должен вручную создать отправление в CDEK | KNOWN |

---

## Рекомендованные фиксы по приоритету

### P0 — До запуска

**Fix #1 — Сохранить orderId в sessionStorage**
```ts
// CartPage.tsx:867, после clear()
sessionStorage.setItem("balgyn_pending_order", JSON.stringify({
  orderId: order.id,
  totalPrice: order.totalPrice,
  expiresAt: Date.now() + 60 * 60 * 1000
}))
```
Восстанавливать при монтировании CartPage если `completedOrder === null`.
Очищать после успешного redirect на paymentUrl или на `/payment/success`.

**Fix #2 — Защита от двойного `expire()`**
В `OrderExpiryService.expire()` добавить проверку перед изменением инвентаря:
```java
@Transactional
public void expire(Order order) {
    // Re-read under transaction to get current status
    Order fresh = orderRepository.findById(order.getId()).orElseThrow();
    if (fresh.getStatus() != OrderStatus.PENDING_PAYMENT) return;  // already expired/confirmed
    releaseInventory(fresh);
    ...
}
```

**Fix #3 — REFUNDED webhook должен обновлять статус заказа**
```java
private void handleCaptureRefunded(...) {
    ...
    payment.setStatus(PaymentStatus.REFUNDED);
    Order order = payment.getOrder();
    order.setStatus(OrderStatus.REFUNDED);  // ← добавить
    order.setUpdatedAt(LocalDateTime.now());
    orderRepository.save(order);
    ...
}
```
Требует добавить `REFUNDED` в `OrderStatus` enum если его там нет.

### P1 — Первая неделя после запуска

**Fix #4 — X-Forwarded-For: читать первый элемент, не последний**
```java
return parts[0].trim();  // не parts[parts.length - 1]
```
И убедиться, что nginx конфигурирован с `proxy_set_header X-Forwarded-For $remote_addr` (replace, не append).

**Fix #5 — Страница успеха Freedom Pay должна показывать pending state**
`PaymentSuccessPage` не делает запрос к backend — не знает реального статуса.
Добавить `GET /api/v1/order/{id}` polling (3–5 секунд) до подтверждения статуса.

**Fix #6 — Не чистить корзину до подтверждения платежа**
Текущий порядок: создать заказ → **clear()** → показать форму оплаты  
Правильный порядок: создать заказ → показать форму оплаты → redirect → **clear() только после capture/callback**  
Реализация: хранить pending lines в отдельном store или задержать `clear()` до `window.location.href`.
