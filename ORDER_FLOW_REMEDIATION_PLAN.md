# Order Flow Remediation Plan — BALGYN

Version: 1.0  
Date: 2026-06-20  
Audit method: полное чтение исходного кода, трассировка вызовов, анализ DB-схемы.  
Status: PLAN ONLY — код не изменялся.

---

## 1. Findings

Обнаружены 4 баги и 3 дополнительных аудита (callback idempotency, state machine, inventory consistency).

| ID | Severity | Title | Files |
|----|----------|-------|-------|
| BUG-1 | P0 | Заказ теряется при закрытии вкладки | CartPage.tsx |
| BUG-2 | P0 | Двойное освобождение инвентаря | OrderExpiryService.java |
| BUG-3 | P1 | PayPal REFUNDED не обновляет order.status | PayPalServiceImpl.java, OrderStatus.java |
| BUG-4 | P1 | PayPal Cancel → retry невозможен | PaymentCancelledPage.tsx, PayPalServiceImpl.java |
| AUD-1 | INFO | Callback idempotency analysis | PaymentServiceImpl.java, PayPalServiceImpl.java |
| AUD-2 | P1 | State machine — переходы не строго упорядочены | OrderServiceImpl.java, OrderStatus.java |
| AUD-3 | P0 | Inventory consistency — рефанд не возвращает инвентарь | PayPalServiceImpl.java, OrderExpiryService.java |

---

## 2. Root Causes

---

### BUG-1: Потеря заказа после создания

#### Участвующие файлы
```
frontend/src/pages/CartPage.tsx         lines 861-883 (orderMutation.onSuccess)
frontend/src/pages/CartPage.tsx         lines 947-998 (handleInitPayment)
frontend/src/stores/cart.store.ts       addItem / clear
```

#### Участвующие методы
```
CartPage.tsx:orderMutation.onSuccess()  → clear() + setCompletedOrder()
CartPage.tsx:handleInitPayment()        → guard: if (!completedOrder) return
cart.store.ts:clear()                   → удаляет localStorage["balgyn-cart-v1"]
```

#### Участвующие таблицы
```
orders           INSERT status=PENDING_PAYMENT + UPDATE totalPrice после расчёта доставки
order_items      INSERT для каждой позиции
inventory        UPDATE quantity -= qty (SELECT FOR UPDATE)
delivery_addresses INSERT (если requiresAddress)
order_history    INSERT status=PENDING_PAYMENT
```

#### Последовательность событий при создании заказа
```
POST /api/v1/order
  → OrderServiceImpl.createOrder()
    → INSERT customers
    → INSERT orders (status=PENDING_PAYMENT)
    → для каждого item: SELECT FOR UPDATE inventory, UPDATE inventory.quantity-=qty, INSERT order_items
    → deliveryPricingService.quote() → SELECT delivery_settings, delivery_tariffs, exchange_rates
    → UPDATE orders (totalPrice, deliveryFee, shippingZone...)
    → INSERT delivery_addresses
    → INSERT order_history (PENDING_PAYMENT)
    → return OrderResponse { id: 42, totalPrice: 35000, ... }
  
  Frontend onSuccess:
    clear()            ← localStorage["balgyn-cart-v1"] = []  (НЕОБРАТИМО)
    setCompletedOrder({id:42,...})   ← только React state (память)
    setPhase("cart")
    setStep(1)
```

#### Почему заказ теряется

`completedOrder` — это локальная переменная React (`useState`). Её жизненный цикл привязан к монтированию компонента `CartPage`. Любое из следующих действий уничтожает её значение:

- Закрытие вкладки браузера
- Перезагрузка страницы (F5 / Ctrl+R)
- Переход на другой URL и возврат обратно через историю браузера (React Router пересоздаёт компонент)
- Потеря интернета + автоматическое обновление страницы

После потери `completedOrder` функция `handleInitPayment()` немедленно возвращает управление (`if (!completedOrder) return`) — пользователь больше не видит форму оплаты и не может её найти. Корзина к этому моменту уже пуста.

---

### BUG-2: Двойное освобождение инвентаря

#### Участвующие файлы
```
src/.../service/Impl/OrderExpiryService.java    lines 75-83 (expire)
src/.../service/Impl/OrderExpiryService.java    lines 89-109 (releaseInventory)
src/.../service/Impl/OrderExpiryService.java    lines 57-69 (expireStaleOrders — @Scheduled)
src/.../service/Impl/PaymentServiceImpl.java    lines 156-159 (handleFreedomPayCallback)
src/.../service/Impl/PayPalServiceImpl.java     lines 186-189 (captureOrder — FAILED branch)
src/.../service/Impl/PayPalServiceImpl.java     lines 126-127 (cancelOrder)
src/.../service/Impl/PayPalServiceImpl.java     lines 277-279 (handleCaptureDenied)
src/.../repositories/InventoryRepository.java   lines 27-33 (findAndLockByGarmentColorSize)
```

#### Участвующие методы
```
OrderExpiryService.expireStaleOrders()    @Scheduled(fixedDelay=300000)
OrderExpiryService.expire(Order)          @Transactional — вызывается из 5 мест
OrderExpiryService.releaseInventory(Order) — НЕТ проверки текущего статуса заказа
InventoryRepository.findAndLockByGarmentColorSize() @Lock(PESSIMISTIC_WRITE)
```

#### Участвующие таблицы
```
orders     — SELECT status, затем UPDATE status=EXPIRED
inventory  — SELECT FOR UPDATE qty, UPDATE qty += qty_released
payments   — UPDATE status=CANCELLED
order_history — INSERT status=EXPIRED
```

#### Механизм гонки

`expireStaleOrders()` — это `@Scheduled` метод с `@Transactional`. Он стартует ОДНУ транзакцию, внутри которой читает ВСЕ заказы в статусе `PENDING_PAYMENT` за один SELECT:

```java
List<Order> stale = orderRepository.findByStatusAndCreatedAtBefore(PENDING_PAYMENT, cutoff);
for (Order order : stale) {
    expire(order);   // self-invocation: @Transactional expire() участвует в той же транзакции
}
```

Одновременно, в отдельной транзакции, приходит Freedom Pay callback (или PayPal cancel):

```java
// handleFreedomPayCallback() — своя @Transactional транзакция
orderExpiryService.expire(saved.getOrder());   // external call через прокси → своя транзакция
```

Временная шкала:

```
T=0    Scheduler TX начинается
T=1    Scheduler: SELECT * FROM orders WHERE status='PENDING_PAYMENT' AND created_at < cutoff
       → order #42 попадает в список (status ещё PENDING_PAYMENT)
T=2    Freedom Pay callback TX начинается
T=3    FP callback: expire(order#42)
         → releaseInventory(): SELECT FOR UPDATE inventory row → qty=2, qty+3=5, UPDATE
         → UPDATE orders SET status='EXPIRED'
         → INSERT order_history
       FP callback TX коммитится
T=4    Scheduler: expire(order#42) (из заранее полученного списка)
         → releaseInventory(): SELECT FOR UPDATE inventory row
           (READ COMMITTED: видит свежее qty=5, т.к. FP callback уже закоммитил)
           qty=5, qty+3=8, UPDATE   ← ДВОЙНОЕ ОСВОБОЖДЕНИЕ
         → UPDATE orders SET status='EXPIRED' (уже EXPIRED, перезаписывается)
       Scheduler TX коммитится
```

Итог: `inventory.quantity = 8` вместо `5`. Товаров на складе становится больше, чем есть физически.

#### Почему SELECT FOR UPDATE не помогает

`SELECT FOR UPDATE` на inventory-строку защищает от ПАРАЛЛЕЛЬНОГО изменения одной строки. Но гонка здесь ПОСЛЕДОВАТЕЛЬНАЯ:
- FP callback получает блокировку → делает UPDATE → отпускает
- Scheduler получает блокировку (после FP) → делает ЕЩЁ ОДИН UPDATE → ✗

`SELECT FOR UPDATE` гарантирует только что два потока не пишут ОДНОВРЕМЕННО — не гарантирует что второй поток не должен вообще писать.

#### Все вызывающие expire()

| Вызывающий | Транзакция | Когда |
|------------|-----------|-------|
| `expireStaleOrders()` | scheduler's tx | каждые 5 мин |
| `handleFreedomPayCallback()` pg_result=0 или CANCELLED | callback tx | Freedom Pay webhook |
| `PayPalServiceImpl.captureOrder()` — failed branch | capture tx | PaymentReturnPage.tsx при F5 |
| `PayPalServiceImpl.cancelOrder()` | cancel tx | PaymentCancelledPage.tsx |
| `PayPalServiceImpl.handleCaptureDenied()` | webhook tx | PayPal CAPTURE.DENIED webhook |

---

### BUG-3: PayPal REFUNDED не обновляет order.status

#### Участвующие файлы
```
src/.../service/Impl/PayPalServiceImpl.java    lines 284-298 (handleCaptureRefunded)
src/.../enums/OrderStatus.java                 — нет REFUNDED статуса
src/.../repositories/OrderRepository.java      — нет findByStatus(REFUNDED)
```

#### Участвующие методы
```
PayPalServiceImpl.handleCaptureRefunded(event, eventId)
PaymentRepository.findByProviderPaymentId(orderId)
```

#### Участвующие таблицы
```
payments     UPDATE status=REFUNDED  ← происходит
orders       НЕТ UPDATE              ← НЕ происходит (баг)
```

#### Точная точка отказа

```java
// PayPalServiceImpl.java:284-298
private void handleCaptureRefunded(PayPalWebhookEvent event, String eventId) {
    String orderId = extractOrderId(event);
    if (orderId == null) return;

    paymentRepository.findByProviderPaymentId(orderId).ifPresentOrElse(payment -> {
        if (payment.getProvider() != PaymentProvider.PAYPAL) return;
        payment.setStatus(PaymentStatus.REFUNDED);       // ← только payment
        payment.setWebhookEventId(eventId);
        payment.setLastWebhookPayload(...);
        payment.setUpdatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment); // ← UPDATE payments ✓
        log.info("[PayPal] Payment for order #{} refunded", saved.getOrder().getId());
        recordEvent(eventId, saved);
        // ↑ order.setStatus(REFUNDED) отсутствует
        // ↑ orderRepository.save(order) отсутствует
    }, () -> log.warn(...));
}
```

#### Дополнительная проблема

`OrderStatus` enum (OrderStatus.java) не содержит значения `REFUNDED`:
```java
public enum OrderStatus {
    PENDING_PAYMENT, NEW, CONFIRMED, IN_PRODUCTION, READY, SHIPPED, DELIVERED, CANCELLED, EXPIRED
}
```

Значит, исправление потребует:
1. Добавить `REFUNDED` в `OrderStatus` enum
2. Добавить Flyway-миграцию, если в PostgreSQL есть CHECK-constraint или ENUM-тип на колонке `status`
3. Проверить, обрабатывает ли `getAll()` и `assertAllowedStatusTransition()` новый статус
4. Обновить `handleCaptureRefunded()` для вызова `orderRepository.save()`

#### Последствия в продакшне

Администратор видит заказ в статусе `CONFIRMED` (или дальше по flow). PayPal прислал деньги обратно покупателю. Заказ числится активным — возможна ситуация когда:
- Заказ уходит в `IN_PRODUCTION` (вышивают)
- Менеджер не знает что деньги возвращены
- Товар производится без оплаты

---

### BUG-4: PayPal Cancel → корзина пуста, retry невозможен

#### Участвующие файлы
```
frontend/src/pages/CartPage.tsx             lines 861-883 (orderMutation.onSuccess — clear())
frontend/src/pages/CartPage.tsx             lines 951-953 (cancelUrl)
frontend/src/pages/PaymentCancelledPage.tsx lines 43-48 (cancel POST)
src/.../service/Impl/PayPalServiceImpl.java lines 109-132 (cancelOrder)
src/.../service/Impl/OrderExpiryService.java lines 76-83 (expire)
```

#### Участвующие методы
```
CartPage.tsx:orderMutation.onSuccess()      → clear() (корзина очищается СРАЗУ при создании заказа)
PaymentCancelledPage.tsx:useEffect()        → POST /api/v1/payments/paypal/cancel/{token}
PayPalServiceImpl.cancelOrder()             → payment.status=CANCELLED + expire(order)
OrderExpiryService.expire()                 → order.status=EXPIRED
```

#### Участвующие таблицы
```
orders     UPDATE status=EXPIRED
payments   UPDATE status=CANCELLED
inventory  UPDATE quantity += qty (инвентарь возвращается)
order_history INSERT status=EXPIRED
```

#### Точная последовательность событий

```
1.  User: завершает 5-шаговый wizard, нажимает "Разместить заказ"
2.  POST /api/v1/order → 200 OK, { id: 42 }
3.  Frontend onSuccess:
      clear()            → localStorage["balgyn-cart-v1"] = []  ← КОРЗИНА ПУСТА
      setCompletedOrder({id:42,...})
4.  User: выбирает PayPal, нажимает "Оплатить"
5.  POST /api/v1/payments/paypal/create-order → { paymentUrl: "https://paypal.com/..." }
6.  window.location.href = paymentUrl  → браузер уходит на PayPal
7.  User: нажимает "Cancel" на странице PayPal
8.  PayPal редиректит: GET /payment/cancelled?token=PP-ORDER-XYZ
9.  PaymentCancelledPage.tsx:useEffect():
      POST /api/v1/payments/paypal/cancel/PP-ORDER-XYZ
        → PayPalServiceImpl.cancelOrder()
          → payment.status = CANCELLED
          → orderExpiryService.expire(order)
            → releaseInventory()   → инвентарь возвращается
            → cancelPendingPayments()
            → order.status = EXPIRED  ← ТЕРМИНАЛЬНЫЙ статус
10. User: видит страницу "Оплата отменена", нажимает "Повторить"
11. Redirect: GET /cart
12. Корзина пустая. Заказ #42 в статусе EXPIRED.
    assertAllowedStatusTransition(EXPIRED, anything) → BusinessRuleException
    Нет кнопки "вернуть к оплате". Нет способа восстановить прерванный заказ.
```

#### Разница с Freedom Pay

Freedom Pay НЕ имеет эквивалентной проблемы:
- При неудаче Freedom Pay сам шлёт callback с `pg_result=0`
- Callback приходит на backend, который вызывает `expire()`
- Frontend при этом просто редиректит на `success_url` / `failure_url` без дополнительных action
- Пользователь попадает на `/payment/failed`, там тоже кнопка "Повторить" ведёт на пустую корзину

Проблема в обоих случаях одна: `clear()` вызывается сразу при создании заказа, а не после подтверждения оплаты.

---

## 3. Risk Assessment

| Баг | Вероятность возникновения | Финансовый ущерб | Репутационный ущерб |
|-----|--------------------------|------------------|---------------------|
| BUG-1 | ВЫСОКАЯ (любой случайный refresh) | СРЕДНИЙ (потерянный заказ, замороженный инвентарь на 60 мин) | ВЫСОКИЙ (пользователь не понимает что произошло) |
| BUG-2 | НИЗКАЯ (race window ~мин) | ВЫСОКИЙ (phantom stock → overselling) | ВЫСОКИЙ |
| BUG-3 | СРЕДНЯЯ (зависит от % возвратов) | ВЫСОКИЙ (производство товара без оплаты) | КРИТИЧЕСКИЙ |
| BUG-4 | ВЫСОКАЯ (отмена — обычное действие) | НИЗКИЙ (пользователь просто повторит) | СРЕДНИЙ |

### Сценарий критического ущерба от BUG-2

При 100 заказах в день, если 10% имеют проблему с оплатой (payment fails/cancelled), шанс гонки между scheduler и callback невелик, но ненулевой. После 2-3 таких гонок phantom stock позволяет принять заказы на товары, которых физически нет → невыполненные обязательства перед покупателями.

### Сценарий критического ущерба от BUG-3

Покупатель оформляет заказ на 50 000 ₸ через PayPal. Заказ подтверждается. Через 2 дня покупатель инициирует refund в PayPal (например, не получил трекинг-номер). PayPal присылает webhook `PAYMENT.CAPTURE.REFUNDED`. Деньги возвращены покупателю. Backend НЕ меняет статус заказа. Менеджер продолжает производство. Через неделю отправляет посылку — потеря ~50 000 ₸ + стоимость производства.

---

## 4. Alternative Solutions

---

### BUG-1: Варианты решения

#### Вариант A: sessionStorage для pending order

Сразу после `orderMutation.onSuccess`, перед `clear()`, сохранить `orderId` в `sessionStorage`. При монтировании `CartPage`, если `completedOrder === null`, попытаться восстановить из `sessionStorage`.

**Преимущества:**
- Минимальный scope изменений (только CartPage.tsx)
- Нет новых API-эндпоинтов
- Работает мгновенно (0 RTT на восстановление)
- sessionStorage автоматически очищается при закрытии вкладки → не накапливается мусор

**Недостатки:**
- Не работает при закрытии и повторном открытии вкладки (sessionStorage не персистентен между сессиями)
- Не помогает пользователям, переключившимся на другое устройство
- Залогиненный пользователь всё равно не может найти заказ на другом устройстве

**Сложность:** НИЗКАЯ (1 файл, ~15 строк)
**Риск миграции:** НУЛЕВОЙ (frontend-only)

---

#### Вариант B: localStorage + TTL для pending order

То же что Вариант A, но `localStorage` с принудительным TTL (60 минут = время жизни заказа).

**Преимущества:**
- Работает после закрытия и повторного открытия вкладки
- Работает если пользователь перешёл по другой вкладке того же браузера
- TTL обеспечивает автоматическую очистку

**Недостатки:**
- Нужен TTL-механизм (читать при монтировании, проверять `expiresAt < Date.now()`)
- Мусор остаётся в localStorage если TTL-проверка не удалась

**Сложность:** НИЗКАЯ (1 файл, ~20 строк)
**Риск миграции:** НУЛЕВОЙ

---

#### Вариант C: Backend endpoint "resume payment" + ссылка из Order History

Добавить `GET /api/v1/payments/pending/{orderId}` — возвращает `paymentUrl` для заказа в PENDING_PAYMENT, если он принадлежит текущему пользователю. В `/orders` (Order History) показывать для PENDING_PAYMENT заказов кнопку "Оплатить".

**Преимущества:**
- Решает проблему на любом устройстве (кроссдевайсное восстановление)
- Пользователь всегда может найти незавершённую оплату

**Недостатки:**
- Требует нового backend-эндпоинта
- Требует изменений в `OrderHistoryPage.tsx`
- Работает только для авторизованных пользователей (анонимные заказы — нет привязки к userEmail)
- Технически сложнее: нужно повторно вызвать `initPayment()` или `createPayPalOrder()` (они идемпотентны, это ок)

**Сложность:** СРЕДНЯЯ (backend + frontend)
**Риск миграции:** НИЗКИЙ

---

#### Рекомендованное решение BUG-1

**Комбинация Варианта B + Варианта C:**

1. localStorage + TTL как немедленная защита (frontend-only, 0 риска)
2. "Resume payment" кнопка в Order History для авторизованных пользователей

Порядок внедрения: сначала Вариант B (1 день), потом Вариант C (3-5 дней).

---

### BUG-2: Варианты решения

#### Вариант A: Status-guard в начале expire()

Перечитать заказ из БД в начале `expire()` и вернуться если статус уже не `PENDING_PAYMENT`.

```
expire(Order order):
  order_fresh = SELECT * FROM orders WHERE id = order.id  ← новый SELECT
  if (order_fresh.status != PENDING_PAYMENT) return       ← ранний выход
  releaseInventory(order_fresh)
  ...
```

С READ COMMITTED (дефолт PostgreSQL): новый SELECT внутри транзакции увидит закоммиченный результат другой транзакции → EXPIRED статус будет виден → ранний выход.

**Преимущества:**
- Минимальный scope: 1 файл, 5 строк
- Идемпотентность: безопасно вызывать expire() несколько раз для одного заказа
- Не меняет сигнатуру, не ломает тесты

**Недостатки:**
- Дополнительный SELECT на каждый вызов expire()
- Требует внимания к propagation: если expire() вызывается в той же транзакции (scheduler self-invocation), новый SELECT внутри той же транзакции всё равно увидит COMMITTED данные другой транзакции (READ COMMITTED) ✓

**Сложность:** МИНИМАЛЬНАЯ (5 строк)
**Риск миграции:** НУЛЕВОЙ

---

#### Вариант B: Пессимистическая блокировка на строке orders

Добавить `SELECT FOR UPDATE` на строку `orders` в `expire()`, чтобы только один поток мог одновременно изменять статус заказа.

```
expire(Order order):
  order_locked = SELECT * FROM orders WHERE id=? FOR UPDATE
  if (order_locked.status != PENDING_PAYMENT) return
  releaseInventory(order_locked)
  ...
```

**Преимущества:**
- Полностью исключает race condition (блокировка на уровне строки БД)
- Гарантированная консистентность даже при высокой нагрузке

**Недостатки:**
- Требует нового метода в `OrderRepository` с `@Lock(PESSIMISTIC_WRITE)`
- При высокой нагрузке: потенциальный deadlock если scheduler держит lock на inventory И orders одновременно
- Нужна Flyway-миграция? Нет, но нужен новый JPA query

**Сложность:** СРЕДНЯЯ
**Риск миграции:** НИЗКИЙ (новый query)

---

#### Рекомендованное решение BUG-2

**Вариант A (status-guard)**. Простая проверка свежего статуса устраняет race при READ COMMITTED. Вариант B избыточен: блокировка строки orders одновременно с блокировкой inventory увеличивает вероятность deadlock.

---

### BUG-3: Варианты решения

#### Вариант A: Добавить REFUNDED в OrderStatus + обновить handleCaptureRefunded()

Добавить `REFUNDED` в enum. В `handleCaptureRefunded()` добавить `order.setStatus(REFUNDED)`. Flyway-миграция если нужно.

**Преимущества:**
- Семантически правильно: статус заказа отражает факт возврата
- Admin видит заказ в корректном статусе
- Можно фильтровать refunded заказы в admin

**Недостатки:**
- Требует проверки всех мест где используется `OrderStatus` (getAll, assertAllowedStatusTransition, OrderHistory)
- Нужно решить: что происходит с инвентарём при REFUNDED? (см. AUD-3)
- Нужна Flyway-миграция для данных если есть CHECK constraint на orders.status

**Сложность:** СРЕДНЯЯ (enum + service + migration + проверка всех switch/if)
**Риск миграции:** СРЕДНИЙ

---

#### Вариант B: Не добавлять REFUNDED в OrderStatus, только записать в order_history

`order.status` остаётся прежним (CONFIRMED/PROCESSING...). `handleCaptureRefunded()` записывает специальную запись в `order_history` с пометкой "REFUND" и обновляет `payment.status = REFUNDED` (уже происходит).

**Преимущества:**
- Нет изменений в enum и DB schema
- Простая реализация
- Меньший риск broken migrations

**Недостатки:**
- Admin видит заказ как обычный (CONFIRMED) — не очевидно что нужно остановить производство
- Нет фильтрации "показать только refunded заказы"

**Сложность:** НИЗКАЯ
**Риск миграции:** НУЛЕВОЙ

---

#### Рекомендованное решение BUG-3

**Вариант A** — технически правильный. Отдельный статус `REFUNDED` в `OrderStatus` даёт admin-пользователю мгновенный сигнал что производство нужно остановить. При этом нужно:
1. Проверить DB-схему (`orders.status` — VARCHAR или ENUM). По Flyway-миграциям: это VARCHAR(30) — Flyway-миграция не нужна.
2. Обновить `getAll()` — включать REFUNDED в admin-список? Да.
3. Обновить `assertAllowedStatusTransition()` — REFUNDED терминальный? Да.
4. Обновить `handleCaptureRefunded()` — добавить order update.

---

### BUG-4: Варианты решения

#### Вариант A: Не вызывать expire() при PayPal cancel — держать заказ PENDING_PAYMENT

Вместо expire() при отмене покупателем — только пометить payment.status=CANCELLED и позволить `OrderExpiryService` обработать заказ по истечении 60 минут.

**Преимущества:**
- Пользователь может retry: заказ ещё PENDING_PAYMENT, completedOrder (если он ещё в памяти) позволяет вызвать payment снова
- Инвентарь остаётся зарезервированным — конкурент не займёт место в течение 60 мин

**Недостатки:**
- Инвентарь заморожен лишние N минут (N = время до expiry)
- Если пользователь закрыл вкладку после cancel — retry всё равно невозможен без решения BUG-1
- Отменённый PayPal платёж всё равно нельзя "реактивировать" — нужен НОВЫЙ PayPal order

**Сложность:** МИНИМАЛЬНАЯ (удалить вызов expire в cancelOrder)
**Риск миграции:** НУЛЕВОЙ

---

#### Вариант B: Восстанавливать корзину из order_items при cancel

После cancel не вызывать expire(). Redirect на CartPage с query-параметром `?resumeOrderId=42`. CartPage при монтировании читает `GET /api/v1/order/42/items` и восстанавливает корзину.

**Преимущества:**
- Полноценный retry: пользователь видит корзину с теми же товарами
- Если он снова идёт в checkout — создаётся НОВЫЙ заказ (старый PENDING → expire через 60 мин)

**Недостатки:**
- Требует нового backend endpoint `GET /api/v1/order/{id}/items`
- При создании нового заказа — двойное резервирование инвентаря до expiry старого
- Нужна защита от бесконечного цикла (cancel → restore → cancel → restore)

**Сложность:** СРЕДНЯЯ
**Риск миграции:** НИЗКИЙ

---

#### Вариант C: Кнопка "Выбрать другой способ оплаты" на /payment/cancelled

На странице `/payment/cancelled` вместо "Повторить → /cart" показать кнопку "Выбрать другой способ оплаты". Эта кнопка читает orderId из sessionStorage и вызывает `initPayment()` с другим провайдером или тем же.

**Преимущества:**
- Не требует backend изменений
- Пользователь остаётся в контексте оплаты

**Недостатки:**
- orderId в sessionStorage пуст после F5 на /payment/cancelled
- Нужна связка с BUG-1 fix (sessionStorage) для работы

**Сложность:** НИЗКАЯ
**Риск миграции:** НУЛЕВОЙ

---

#### Рекомендованное решение BUG-4

Зависит от решения BUG-1. Если реализован Вариант B (localStorage + TTL):

1. Вариант A (не вызывать expire при cancel) — самостоятельного значения не имеет, т.к. корзина всё равно пуста
2. **Вариант C + BUG-1 fix** — пользователь попадает на /payment/cancelled, там кнопка "Попробовать другой способ оплаты" использует orderId из localStorage, вызывает `handleInitPayment()` с выбором провайдера

Таким образом, BUG-4 частично решается исправлением BUG-1.

---

## 5. Additional Audit Results

---

### AUD-1: Callback Idempotency

#### Freedom Pay — SUCCESS × N

```
Первый callback (pg_payment_id=FP-123, pg_result=1):
  existsByProviderAndEventId(FREEDOM_PAY, FP-123) → FALSE
  payment.status = SUCCEEDED
  order.status = CONFIRMED
  INSERT processed_webhook_events(FREEDOM_PAY, FP-123)   ← UNIQUE constraint
  COMMIT

Второй callback (pg_payment_id=FP-123, pg_result=1):
  existsByProviderAndEventId(FREEDOM_PAY, FP-123) → TRUE
  log("Callback replay ignored")
  return toResponse(payment)   ← SAFE ✓
```

**Защита от параллельных одинаковых callbacks:**
Если два одинаковых callback приходят ОДНОВРЕМЕННО — оба проходят `existsBy` (оба видят FALSE), оба пытаются INSERT в `processed_webhook_events`. Один INSERT успевает, второй получает UNIQUE constraint violation → exception → rollback второго.

При rollback второго: изменения `payment.status=SUCCEEDED` и `order.status=CONFIRMED` тоже откатываются — они были в той же транзакции. После первого коммита данные корректные. ✅ SAFE.

**Вывод: Freedom Pay идемпотентность SOLID.**

---

#### PayPal webhook CAPTURE.COMPLETED × N

```
Первый webhook (eventId=EVT-123):
  existsByProviderAndEventId(PAYPAL, EVT-123) → FALSE
  payment.status != SUCCEEDED → обрабатываем
  payment.status = SUCCEEDED, order.status = CONFIRMED
  INSERT processed_webhook_events(PAYPAL, EVT-123)
  COMMIT

Второй webhook (eventId=EVT-123):
  existsByProviderAndEventId(PAYPAL, EVT-123) → TRUE
  return   ← SAFE ✓
```

**Дополнительная защита (handleCaptureCompleted):**
```java
if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
    recordEvent(eventId, payment);  // просто записывает event, не меняет статусы
    return;
}
```

Двойная защита: сначала `existsBy`, потом `status check`. ✅ SAFE.

---

#### PayPal capture endpoint (F5 на /payment-return) × N

```
PaymentReturnPage.tsx: useEffect → capturePayPalOrder(token)

Первый capture:
  findByProviderPaymentIdForUpdate(ppId)  ← SELECT FOR UPDATE
  payment.status == PENDING → продолжаем
  payPalOrdersClient.captureOrder()  ← реальный вызов к PayPal API
  payment.status = SUCCEEDED, order.status = CONFIRMED
  COMMIT

Второй capture (F5 пока первый ещё не закончился):
  findByProviderPaymentIdForUpdate(ppId)  ← блокируется (первый держит lock)
  После разблокировки: payment.status == SUCCEEDED
  status != PENDING → return toResponse(payment)   ← SAFE ✓

Третий capture (после первого закоммитил):
  payment.status == SUCCEEDED
  status != PENDING → return toResponse(payment)   ← SAFE ✓
```

**Вывод: PayPal capture endpoint идемпотентен.** SELECT FOR UPDATE + status-check — двойная защита. ✅

---

#### ВАЖНОЕ ЗАМЕЧАНИЕ: race между capture и CAPTURE.COMPLETED webhook

```
T=0   User нажимает Back на PayPal → redirect на /payment-return?token=PP-XYZ
T=1   PaymentReturnPage: capturePayPalOrder(PP-XYZ) → findByProviderPaymentIdForUpdate → LOCKED
T=2   PayPal присылает CAPTURE.COMPLETED webhook (параллельно)
T=3   handleCaptureCompleted: findByProviderPaymentId(PP-XYZ) ← БЕЗ БЛОКИРОВКИ
      payment.status = PENDING → обрабатываем
      payment.status = SUCCEEDED, order.status = CONFIRMED
      COMMIT
T=4   captureOrder разблокируется: payment.status = SUCCEEDED
      status != PENDING → return ✓
```

Оба пути могут записать `CONFIRMED` в orders. Второй раз — идемпотентная перезапись. ✅ Технически безопасно.

НО: в `handleCaptureCompleted` используется `findByProviderPaymentId` (БЕЗ SELECT FOR UPDATE), а в `captureOrder` используется `findByProviderPaymentIdForUpdate` (WITH SELECT FOR UPDATE). Это означает webhook handler не ожидает завершения capture. Возможна ситуация когда webhook пишет SUCCEEDED раньше capture → capture видит SUCCEEDED → возвращает без второго вызова PayPal API. ✅ Правильное поведение.

**Вывод: idempotency всех webhook handlers SOLID. Единственный незащищённый путь — двойное освобождение инвентаря (BUG-2).**

---

### AUD-2: Order Status State Machine

#### Зафиксированные статусы (`OrderStatus.java`)

```
PENDING_PAYMENT  — создан, ожидает оплаты (скрыт в admin)
NEW              — оплачен или создан вручную (виден в admin)
CONFIRMED        — подтверждён менеджером
IN_PRODUCTION    — в работе (шьют/вышивают)
READY            — готов к отправке
SHIPPED          — отправлен
DELIVERED        — доставлен клиенту (терминальный)
CANCELLED        — отменён (терминальный)
EXPIRED          — истёк срок оплаты (терминальный)
[ОТСУТСТВУЕТ: REFUNDED]
```

#### Автоматические переходы (система)

```
PENDING_PAYMENT → CONFIRMED    (Freedom Pay callback pg_result=1)
PENDING_PAYMENT → CONFIRMED    (PayPal captureOrder COMPLETED)
PENDING_PAYMENT → CONFIRMED    (PayPal CAPTURE.COMPLETED webhook)
PENDING_PAYMENT → EXPIRED      (OrderExpiryService.expire() — 60 мин)
PENDING_PAYMENT → EXPIRED      (Freedom Pay callback pg_result=0)
PENDING_PAYMENT → EXPIRED      (PayPal captureOrder non-COMPLETED)
PENDING_PAYMENT → EXPIRED      (PayPal cancelOrder — cancel URL)
PENDING_PAYMENT → EXPIRED      (PayPal CAPTURE.DENIED webhook)
```

#### Ручные переходы (admin) — что ПРОИСХОДИТ

`assertAllowedStatusTransition()` блокирует только выход из терминальных состояний:

```java
if (current == CANCELLED && next != CANCELLED) throw    // нельзя выйти из CANCELLED
if (current == DELIVERED && next != DELIVERED) throw    // нельзя выйти из DELIVERED
if (current == EXPIRED   && next != EXPIRED)   throw    // нельзя выйти из EXPIRED
```

Всё остальное РАЗРЕШЕНО, включая:

```
✓ Бизнес-flow (правильные переходы):
  CONFIRMED   → IN_PRODUCTION
  IN_PRODUCTION → READY
  READY       → SHIPPED
  SHIPPED     → DELIVERED
  ANY (кроме терминальных) → CANCELLED

✗ Неправильные переходы (не заблокированы):
  SHIPPED     → IN_PRODUCTION   ← назад
  SHIPPED     → CONFIRMED       ← назад
  READY       → CONFIRMED       ← назад
  IN_PRODUCTION → NEW           ← назад
  CONFIRMED   → DELIVERED       ← пропуск 3 этапов
  NEW         → DELIVERED       ← пропуск 4 этапов
  PENDING_PAYMENT → CONFIRMED   ← admin может подтвердить без оплаты (!)
```

#### Критическая уязвимость: admin может подтвердить заказ без оплаты

`PENDING_PAYMENT → CONFIRMED` через admin (PATCH /api/v1/order/{id}) не заблокирован. Admin-пользователь (или атакующий с JWT-токеном ADMIN) может подтвердить любой заказ без реальной оплаты.

Последствия: заказ появляется в admin-очереди, производится, отправляется без денег.

#### Рекомендации по state machine

| Переход | Что нужно |
|---------|-----------|
| ANY → CONFIRMED через admin | ЗАПРЕТИТЬ (CONFIRMED устанавливается только системой через payment) |
| Backwards transitions | ЗАПРЕТИТЬ путём явного списка допустимых переходов |
| REFUNDED | ДОБАВИТЬ как терминальный |

**Предлагаемая allowlist-логика:**

```
PENDING_PAYMENT → CONFIRMED    (только системный путь)
PENDING_PAYMENT → CANCELLED    (admin)
NEW             → CONFIRMED    (admin или система)
CONFIRMED       → IN_PRODUCTION (admin)
CONFIRMED       → CANCELLED    (admin)
IN_PRODUCTION   → READY        (admin)
IN_PRODUCTION   → CANCELLED    (admin)
READY           → SHIPPED      (admin)
READY           → CANCELLED    (admin)
SHIPPED         → DELIVERED    (admin)
SHIPPED         → CANCELLED    (admin)
```

Всё что не в этом списке — reject.

---

### AUD-3: Inventory Consistency

#### Все точки изменения inventory

| Место | Операция | Защита | Транзакционный контекст |
|-------|----------|--------|------------------------|
| `OrderServiceImpl.buildDesignOrderItem():274` | `qty -= requested` | SELECT FOR UPDATE (3s timeout) | @Transactional createOrder() |
| `OrderExpiryService.releaseInventory():104-107` | `qty += item.qty` | SELECT FOR UPDATE | expire() / releaseInventory() caller's tx |

#### Нет изменения inventory при:
- Payment SUCCEEDED → correct (товар забронирован)
- CONFIRMED → IN_PRODUCTION → READY → SHIPPED → DELIVERED → correct (товар в работе)
- REFUNDED → **НЕТ ОСВОБОЖДЕНИЯ** (спорно)

#### Может ли inventory уйти в минус?

Нет. При декрементировании проверка:
```java
if (inventory.getQuantity() < quantity) {
    throw new BusinessRuleException("Недостаточно товара...");
}
```
SELECT FOR UPDATE гарантирует что проверка видит актуальное значение. ✅

#### Double decrement (заказать дважды одно и то же)?

Нет. `createOrder()` — один атомарный вызов. Если пользователь дважды нажмёт кнопку, первый SUCCESS очистит корзину; второй вызов не может начаться (кнопка блокируется `orderMutation.isPending`). Даже если оба вызова параллельны — SELECT FOR UPDATE гарантирует что второй видит уже уменьшенное значение. ✅

#### Double increment (phantom stock) — BUG-2

Подробно описан выше. ✓ Требует исправления.

#### Инвентарь при отмене (admin CANCELLED)

```java
// OrderServiceImpl.updateOrderStatus:346-348
if (next == CANCELLED && current != CANCELLED) {
    orderExpiryService.releaseInventory(order);   // инвентарь возвращается ✓
    orderExpiryService.cancelPendingPayments(order);
}
```

При отмене CONFIRMED/IN_PRODUCTION/READY/SHIPPED заказа — инвентарь возвращается. ✅ Корректно.
НО: если заказ был оплачен и отправлен (SHIPPED) и admin отменяет его — инвентарь физически уже у покупателя или в пути. Возврат инвентаря в этом статусе семантически некорректен. Рекомендуется: `releaseInventory()` только при отмене до SHIPPED статуса.

#### Инвентарь при REFUNDED

`handleCaptureRefunded()` НЕ вызывает `releaseInventory()`.

Два сценария:
1. **Возврат до производства**: товар ещё не сделан, резерв инвентаря не снят → инвентарь должен быть возвращён
2. **Возврат после отправки**: товар отправлен, покупатель получил деньги обратно → инвентарь НЕ возвращается (физически уже у клиента)

В текущей реализации оба сценария ведут к НЕ-возврату инвентаря. Это проблема для сценария 1.

Решение: при REFUNDED возвращать инвентарь если `order.status != SHIPPED && order.status != DELIVERED`.

---

## 6. Recommended Fixes

Приоритизированный список с минимальным scope изменений.

### Fix 1 — BUG-1: localStorage + TTL для pending order

**Файлы:** `CartPage.tsx`

**Логика:**
```
onSuccess:
  localStorage.setItem("balgyn_pending_order", {
    orderId: order.id,
    totalPrice: order.totalPrice,
    expiresAt: Date.now() + 55 * 60 * 1000  // 55 мин (меньше 60 чтобы не показывать уже expired)
  })
  
CartPage mount:
  if (completedOrder === null):
    raw = localStorage.getItem("balgyn_pending_order")
    if (raw && parsed.expiresAt > Date.now()):
      // восстановить completedOrder из {orderId, totalPrice}
      // или вызвать GET /api/v1/order/{orderId} для получения полного OrderResponse
    else:
      localStorage.removeItem("balgyn_pending_order")

После redirect на paymentUrl:
  // не очищать — очистится на /payment/success onload
  
/payment/success onload:
  localStorage.removeItem("balgyn_pending_order")
```

**Затронутые файлы:** `CartPage.tsx`, `PaymentSuccessPage.tsx`
**Миграции:** нет
**Тесты:** нужны unit-тесты для restore-логики

---

### Fix 2 — BUG-2: Status-guard в expire()

**Файл:** `OrderExpiryService.java`

**Логика:**
```
expire(Order order):
  Order fresh = orderRepository.findById(order.getId()).orElseThrow()
  if (fresh.getStatus() != OrderStatus.PENDING_PAYMENT):
    log.info("expire() skipped: order #{} status={}", fresh.getId(), fresh.getStatus())
    return
  // продолжить с fresh
  releaseInventory(fresh)
  cancelPendingPayments(fresh)
  fresh.setStatus(EXPIRED)
  ...
```

**Затронутые файлы:** `OrderExpiryService.java`
**Миграции:** нет
**Тесты:** нужен тест конкурентного вызова expire()

---

### Fix 3 — BUG-3: REFUNDED в OrderStatus + handleCaptureRefunded

**Файлы:** `OrderStatus.java`, `PayPalServiceImpl.java`, `OrderServiceImpl.java`

**Логика:**
```
1. Добавить REFUNDED в OrderStatus enum (после DELIVERED)
2. handleCaptureRefunded():
   Order order = payment.getOrder()
   order.setStatus(OrderStatus.REFUNDED)
   order.setUpdatedAt(LocalDateTime.now())
   orderRepository.save(order)
   + recordHistory(order, REFUNDED) если метод публичен
   
3. assertAllowedStatusTransition():
   добавить: if (current == REFUNDED && next != REFUNDED) throw
   
4. getAll():
   текущий фильтр [PENDING_PAYMENT, EXPIRED] — REFUNDED должен ВКЛЮЧАТЬСЯ в список admin
   
5. releaseInventory при REFUNDED:
   в handleCaptureRefunded добавить проверку:
   if (order.status не в {SHIPPED, DELIVERED}):
     orderExpiryService.releaseInventory(order)
```

**Затронутые файлы:** `OrderStatus.java`, `PayPalServiceImpl.java`, `OrderServiceImpl.java`
**Миграции:** НЕ нужны (orders.status — VARCHAR, не DB-enum)
**Тесты:** нужен тест handleCaptureRefunded() проверяющий order.status

---

### Fix 4 — BUG-4: Resume payment после PayPal cancel

**Файлы:** `PaymentCancelledPage.tsx`, `PayPalServiceImpl.java`

**Логика (depends on Fix 1 being deployed):**
```
PayPalServiceImpl.cancelOrder():
  НЕ вызывать expire(order) — только marking payment.CANCELLED
  Пусть OrderExpiryService сам обработает через 60 мин
  
PaymentCancelledPage.tsx:
  Кнопка "Попробовать снова" вместо /cart:
    читает orderId из localStorage["balgyn_pending_order"]
    если есть → navigate('/cart?resumePaymentOrderId=' + orderId)
    
CartPage.tsx при mount:
  читает ?resumePaymentOrderId → восстанавливает completedOrder
  (без перехода в checkout wizard — сразу показывает форму оплаты)
```

**Затронутые файлы:** `PaymentCancelledPage.tsx`, `PayPalServiceImpl.java`, `CartPage.tsx`
**Миграции:** нет
**Тесты:** нужен тест что cancelOrder не вызывает expire()

---

### Fix 5 — AUD-2: Ограничить state machine allowlist

**Файл:** `OrderServiceImpl.java`

**Логика:**
```
assertAllowedStatusTransition(current, next):
  // Заменить дырявую blocklist-защиту на строгую allowlist:
  
  Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
    PENDING_PAYMENT, Set.of(CANCELLED),                       // admin может отменить
    NEW,             Set.of(CONFIRMED, CANCELLED),
    CONFIRMED,       Set.of(IN_PRODUCTION, CANCELLED),
    IN_PRODUCTION,   Set.of(READY, CANCELLED),
    READY,           Set.of(SHIPPED, CANCELLED),
    SHIPPED,         Set.of(DELIVERED, CANCELLED),
    // Terminal:
    DELIVERED, Set.of(),
    CANCELLED, Set.of(),
    EXPIRED,   Set.of(),
    REFUNDED,  Set.of()
  )
  
  if (!ALLOWED.get(current).contains(next)):
    throw BusinessRuleException(...)
    
Примечание: переход PENDING_PAYMENT → CONFIRMED через admin НЕ в allowlist
           → автоматически заблокирован. ✓
```

**Затронутые файлы:** `OrderServiceImpl.java`
**Миграции:** нет
**Тесты:** расширить существующие тесты OrderService

---

## 7. Migration Plan

### Sprint 1 (день 1-2): критические фиксы

```
Day 1:
  1. Implement Fix 2 (status-guard) — 30 минут
  2. Implement Fix 1 (localStorage pending order) — 2 часа
  3. Write tests for Fix 1 and Fix 2
  4. Code review

Day 2:
  5. Deploy to staging
  6. E2E test: create order → close tab → reopen → payment form visible
  7. E2E test: concurrent expire() calls → no double release
  8. Deploy to production
```

### Sprint 2 (день 3-5): P1 фиксы

```
Day 3:
  1. Implement Fix 3 (REFUNDED status) — 3 часа
  2. Tests: handleCaptureRefunded, OrderStatus enum coverage
  3. QA: PayPal sandbox refund flow

Day 4:
  4. Implement Fix 5 (state machine allowlist) — 2 часа
  5. Tests: all invalid transitions should throw
  6. QA: existing admin order flow not broken

Day 5:
  7. Implement Fix 4 (resume payment after cancel) — 2 часа
  8. Integration with Fix 1 (localStorage)
  9. Deploy Sprint 2 to production
```

---

## 8. Required Tests

### Для Fix 1 (pending order recovery)

```
□ CartPage: после onSuccess completedOrder восстанавливается из localStorage при mount
□ CartPage: не восстанавливает если expiresAt < Date.now()
□ CartPage: очищает localStorage после redirect на paymentUrl
□ PaymentSuccessPage: очищает localStorage["balgyn_pending_order"] при монтировании
```

### Для Fix 2 (double expire)

```
□ OrderExpiryService: expire() на уже EXPIRED заказе → no-op (нет изменений в inventory)
□ OrderExpiryService: expire() на CONFIRMED заказе → no-op
□ OrderExpiryService: expire() на PENDING_PAYMENT заказе → корректно выполняется
□ Integration: concurrent expire() calls на одном заказе → inventory изменён ровно один раз
```

### Для Fix 3 (REFUNDED status)

```
□ PayPalServiceImpl: handleCaptureRefunded() обновляет order.status = REFUNDED
□ PayPalServiceImpl: handleCaptureRefunded() вызывает releaseInventory() если order.status не SHIPPED/DELIVERED
□ PayPalServiceImpl: handleCaptureRefunded() НЕ вызывает releaseInventory() для SHIPPED/DELIVERED
□ OrderServiceImpl: assertAllowedStatusTransition(REFUNDED, CONFIRMED) → throws
□ OrderServiceImpl: getAll() включает REFUNDED заказы в admin-список
```

### Для Fix 4 (PayPal cancel retry)

```
□ PayPalServiceImpl: cancelOrder() НЕ вызывает expire() — только CANCELLED payment
□ PaymentCancelledPage: кнопка "Попробовать снова" видна если orderId в localStorage
□ CartPage: при resumePaymentOrderId показывает форму оплаты без checkout wizard
□ Integration: PayPal cancel → retry с другим провайдером → оплата проходит
```

### Для Fix 5 (state machine allowlist)

```
□ OrderServiceImpl: PENDING_PAYMENT → CONFIRMED через admin → throws
□ OrderServiceImpl: SHIPPED → IN_PRODUCTION → throws
□ OrderServiceImpl: CONFIRMED → DELIVERED → throws
□ OrderServiceImpl: CONFIRMED → IN_PRODUCTION → success
□ OrderServiceImpl: SHIPPED → DELIVERED → success
□ OrderServiceImpl: ANY → CANCELLED → success (кроме терминальных)
```

---

## 9. Rollout Strategy

### Порядок деплоя

```
1. Fix 2 (status-guard) — backend only, zero-downtime
   → Deploy: ./gradlew bootJar + docker-compose restart app
   → Verify: logs показывают "expire() skipped" для EXPIRED заказов

2. Fix 1 (localStorage) — frontend only
   → Build: npm run build в frontend/
   → Deploy: nginx перечитывает dist/
   → Verify: открыть /cart, создать тестовый заказ, F5 → форма оплаты видна

3. Fix 3 (REFUNDED) — backend + enum change
   → Важно: проверить нет ли hardcoded switch/case по OrderStatus во всём проекте
   → Deploy backend, verify REFUNDED не ломает admin panel

4. Fix 5 (state machine) — backend
   → Предупредить admin-пользователей о новых ограничениях переходов
   → Deploy, verify existing orders в корректных статусах переходят нормально

5. Fix 4 (PayPal cancel retry) — frontend + backend
   → Deploy вместе (atomically) т.к. frontend ожидает что backend НЕ вызывает expire
```

### Ручное тестирование перед релизом каждого Fix

| Fix | Тест | Ожидаемый результат |
|-----|------|---------------------|
| Fix 1 | Создать заказ → F5 → CartPage | Форма оплаты видна |
| Fix 1 | Создать заказ → закрыть вкладку → открыть /cart | Форма оплаты видна (localStorage) |
| Fix 2 | Включить симуляцию двойного expire() через логи | "expire() skipped" в логах для второго вызова |
| Fix 3 | Отправить CAPTURE.REFUNDED PayPal webhook (sandbox) | order.status = REFUNDED в БД |
| Fix 4 | Создать заказ → PayPal → Cancel → Попробовать снова | Форма оплаты с тем же orderId |
| Fix 5 | Admin: попытаться перевести SHIPPED → CONFIRMED | 422 / BusinessRuleException |

### Мониторинг после деплоя

```
□ Логи: "expire() skipped" — подтверждает Fix 2 работает
□ Логи: "[PayPal] Payment for order #N refunded" + "[PayPal] Order #N REFUNDED" — подтверждает Fix 3
□ DB: SELECT * FROM orders WHERE status='EXPIRED' — кол-во должно снизиться (Fix 4)
□ DB: SELECT inventory.quantity WHERE quantity < 0 — должно быть 0 строк всегда (инварьянт)
□ Alert: любой inventory.quantity < 0 → немедленное уведомление (критический баг)
```
