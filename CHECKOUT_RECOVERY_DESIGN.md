# CHECKOUT_RECOVERY_DESIGN.md
_Дата: 2026-06-21 | Статус: Design / Pre-implementation_

---

## 1. Текущий flow (фактический)

### 1.1 Полная последовательность

```
[CartPage] phase="cart"
  └─ «Оформить заказ» → phase="checkout", step=1

[CartPage] phase="checkout", steps 1–5 (Contacts → Region → Delivery → Details → Summary)
  └─ «Создать заказ» → orderMutation.mutate(payload)

orderMutation.onSuccess(order):
  ├─ clear()              ← КОРЗИНА ОЧИЩАЕТСЯ ЗДЕСЬ
  ├─ setCompletedOrder(order)   ← СОХРАНЕНО ТОЛЬКО В REACT STATE
  ├─ setPhase("cart")
  └─ setStep(1)

[CartPage] рендерит <OrderSuccess> (если completedOrder != null)
  └─ пользователь выбирает провайдера и жмёт «Оплатить»

handleInitPayment():
  ├─ sessionStorage.setItem("balgyn_last_payment", {orderId, totalPrice, provider})
  ├─ initPayment({orderId, ...}) / createPayPalOrder({orderId, ...})
  └─ window.location.href = paymentUrl  ← УХОД СО СТРАНИЦЫ

[Платёжный шлюз — внешний домен]
  ├─ Успех → /payment-return?token=... (PayPal) / /payment/success?... (Freedom Pay)
  ├─ Отмена → /payment/cancelled?token=... (PayPal) / /payment/cancelled (Freedom Pay)
  └─ Ошибка → /payment/failed?error=...

[PaymentReturnPage] (только PayPal)
  └─ capturePayPalOrder(token) → /payment/success или /payment/failed

[PaymentSuccessPage]
  ├─ читает sessionStorage["balgyn_last_payment"]
  ├─ очищает sessionStorage после прочтения
  └─ показывает orderId / amount / provider

[PaymentCancelledPage]
  ├─ читает sessionStorage["balgyn_last_payment"]  ← не очищает
  ├─ POST /payments/paypal/cancel/{token} (только PayPal, best-effort)
  └─ кнопка «Retry» → Link to="/cart"

[PaymentFailedPage]
  ├─ читает sessionStorage["balgyn_last_payment"]  ← не очищает
  └─ кнопка «Retry» → Link to="/cart"
```

### 1.2 Что хранится где

| Данные | Storage | Персистентность |
|--------|---------|-----------------|
| `useCartStore` items | localStorage `balgyn-cart-v1` | ✅ переживает F5, закрытие вкладки |
| `completedOrder` | React useState | ❌ теряется при F5 |
| `balgyn_last_payment` | sessionStorage | ❌ теряется при закрытии вкладки |
| `useCheckoutStore` | Zustand in-memory (no persist) | ❌ теряется при F5 |

---

## 2. Точки потери заказа

### LOSS-1: F5 на экране «Заказ принят / Оплата»

**Триггер:** пользователь создал заказ, видит `<OrderSuccess>` с кнопкой «Оплатить», нажимает F5 или открывает другую вкладку и возвращается.

**Что происходит:**
- `completedOrder` — React state — уничтожается
- Корзина уже пуста (`clear()` был вызван в `onSuccess`)
- `/cart` рендерит «Корзина пуста»
- Нет никаких визуальных подсказок
- Заказ существует в БД в статусе PENDING_PAYMENT, инвентарь зарезервирован
- Через 60 минут планировщик экспирирует заказ

**Тяжесть:** HIGH — пользователь думает, что заказ потерян, создаёт новый

---

### LOSS-2: Закрытие вкладки до оплаты

**Триггер:** пользователь создал заказ, закрыл вкладку (нечаянно или осознанно) до нажатия «Оплатить».

**Что происходит:**
- То же, что LOSS-1
- sessionStorage тоже очищается при закрытии вкладки
- Нет возможности вернуться к оплате

**Тяжесть:** HIGH

---

### LOSS-3: Отмена платежа → пустая корзина

**Триггер:** пользователь нажал «Отмена» на странице Freedom Pay или PayPal.

**Что происходит:**
- `/payment/cancelled` показывает orderId (из sessionStorage)
- Кнопка «Повторить» ведёт на `/cart`
- `/cart` — пустой, потому что корзина была очищена при создании заказа
- Пользователь видит пустую корзину и не понимает, что делать
- Заказ ещё живой в PENDING_PAYMENT (если < 60 мин)

**Тяжесть:** HIGH — прямой UX провал

---

### LOSS-4: Ошибка платежа → пустая корзина

**Триггер:** Freedom Pay вернул ошибку (declined card, timeout и т.д.).

**Что происходит:**
- `/payment/failed` показывает ошибку и orderId
- Кнопка «Повторить» ведёт на `/cart`
- Та же проблема — корзина пуста, completedOrder потерян

**Тяжесть:** HIGH

---

### LOSS-5: balgyn_last_payment в sessionStorage не переживает закрытие вкладки

**Триггер:** пользователь завершает оплату в новой вкладке (некоторые мобильные браузеры открывают платёжный шлюз в новой вкладке).

**Что происходит:**
- sessionStorage не разделяется между вкладками
- `/payment/success` и `/payment/cancelled` читают sessionStorage → ничего нет
- Отображается страница без orderId и suммы

**Тяжесть:** MEDIUM — информационный пробел, но платёж технически прошёл

---

### LOSS-6: Retry payment для уже существующего заказа невозможен

**Текущее состояние:** нет ни страницы, ни кнопки, которая бы инициировала повторную оплату для существующего orderId.

- `/cart` пуст → нет кнопки «Оплатить»
- `OrderHistory` показывает статус PENDING_PAYMENT, но нет кнопки «Оплатить»
- Единственный путь — создать НОВЫЙ заказ (двойное резервирование инвентаря!)

**Тяжесть:** CRITICAL — основная задача этого документа

---

## 3. Варианты восстановления

### Вариант A: localStorage pending payment record (рекомендован)

**Механизм:**
- При `orderMutation.onSuccess`: сохранить в localStorage `balgyn_pending_payment_v1 = {orderId, amount, items, expiresAt}`
- `expiresAt = now + 58 minutes` (чуть меньше серверного окна в 60 мин)
- На `/cart` mount: проверить localStorage, запросить `GET /api/v1/order/{id}`, показать баннер recovery
- После успешной оплаты: удалить запись
- После expiresAt: считать запись протухшей

**Плюсы:**
- Работает после F5, закрытия вкладки, нескольких вкладок (localStorage шарится)
- Не требует авторизации для чтения данных
- Не нужен новый backend endpoint
- Минимальный diff

**Минусы:**
- Если пользователь зашёл с другого устройства — данных нет
- localStorage может быть очищен пользователем

---

### Вариант B: Backend endpoint + auth

**Механизм:**
- Новый endpoint: `GET /api/v1/order/my/pending` (только для авторизованных)
- Возвращает последний PENDING_PAYMENT заказ пользователя (если есть)
- На `/cart` mount: для авторизованного пользователя запрашивать этот endpoint
- Показать баннер «У вас незавершённый заказ #N. Оплатить?»

**Плюсы:**
- Работает на любом устройстве
- Данные всегда актуальны

**Минусы:**
- Добавляет API вызов на каждый mount /cart
- Требует новый backend endpoint + тесты
- Только для авторизованных (гости теряют заказ)
- Latency на загрузке корзины

---

### Вариант C: Выделенная страница retry /orders/{id}/pay

**Механизм:**
- После создания заказа: редирект на `/orders/{orderId}/pay`
- Эта страница: `GET /api/v1/order/{id}` → показывает детали + выбор провайдера
- Кнопки "Отмена" и "Ошибка" на платёжных страницах ведут на `/orders/{orderId}/pay`
- OrderHistory: добавить кнопку «Оплатить» для PENDING_PAYMENT заказов

**Плюсы:**
- Чистый URL, который можно закладками/копировать
- Работает в любом браузере, с любого устройства
- Кнопка «Оплатить» в OrderHistory — естественный recovery path

**Минусы:**
- Ломает текущий UX (пользователь уходит с /cart после создания заказа)
- Нужен новый роут, новая страница
- Для гостей — сложнее (заказ без auth, id нужно где-то хранить)

---

### Вариант D: Перенести balgyn_last_payment из sessionStorage в localStorage

**Механизм:**
- Только изменить `sessionStorage.setItem` на `localStorage.setItem`
- Убрать `sessionStorage.removeItem` из PaymentSuccessPage (или перенести на localStorage)
- PaymentCancelledPage получает данные после закрытия и переоткрытия вкладки

**Плюсы:**
- Минимальный diff — 5 строк кода
- Мгновенно решает LOSS-5

**Минусы:**
- Не решает LOSS-1, LOSS-2, LOSS-3, LOSS-4
- Нет механизма retry для существующего заказа

---

## 4. Подробный разбор: localStorage подход

### 4.1 Структура записи

```typescript
interface PendingPaymentRecord {
  orderId: number;
  amount: number;          // totalPrice snapshot
  items: PendingItem[];    // snapshot для отображения в баннере
  expiresAt: number;       // Date.now() + 58 * 60 * 1000
  provider: PaymentProvider; // последний выбранный провайдер
}

interface PendingItem {
  title: string;     // "Brand Of Sacrifice (Hoodie)"
  qty: number;
  price: number;
}

const PENDING_KEY = "balgyn_pending_payment_v1";
```

### 4.2 Жизненный цикл записи

```
orderMutation.onSuccess ──────────────── записать PendingPaymentRecord
        │
        ├── handleInitPayment() ─────── оставить запись (платёж начат)
        │         │
        │         └── PaymentSuccessPage ─ удалить запись
        │
        ├── пользователь нажал «Продолжить покупки» ─ удалить запись
        │   (order создан, но пользователь решил не платить сейчас)
        │
        ├── CartPage mount ─────────── прочитать запись, верифицировать через API
        │         ├── PENDING_PAYMENT → показать recovery banner
        │         └── EXPIRED/CANCELLED → удалить запись
        │
        └── expiresAt прошёл ─────── считать протухшей, удалить при следующем прочтении
```

### 4.3 Recovery banner на /cart

```
┌─────────────────────────────────────────────────────┐
│ ⚠  У вас незавершённый заказ №1234                 │
│    2 товара · 16 000 ₸ · ожидает оплаты            │
│                                                     │
│  [Оплатить заказ №1234]   [Отменить заказ]          │
└─────────────────────────────────────────────────────┘
```

- «Оплатить» → восстановить `completedOrder` из записи и показать OrderSuccess
- «Отменить» → `DELETE /api/v1/order/{id}` (или admin-cancel) + удалить запись из localStorage
  - Если cancel endpoint недоступен — просто удалить локальную запись, backend сам экспирирует

---

## 5. Backend recovery подход

Для полноценного cross-device recovery нужен:

```
GET /api/v1/order/my/pending
Authorization: Bearer {token}

Response 200:
{
  "id": 1234,
  "status": "PENDING_PAYMENT",
  "totalPrice": 16000,
  "items": [...],
  "createdAt": "2026-06-21T10:00:00",
  "expiresAt": "2026-06-21T11:00:00"
}

Response 204: No pending order
```

Требования:
- Репозиторий: `findFirstByCustomer_UserAndStatusOrderByCreatedAtDesc(user, PENDING_PAYMENT)`
- Только для авторизованных пользователей
- Возвращает только последний, не более одного
- Не показывает EXPIRED/CANCELLED

---

## 6. Retry payment flow

### Текущее состояние
Нет механизма повторной оплаты для существующего orderId.

### Требуемый flow

```
[Пользователь нажимает «Оплатить заказ №1234»]
        │
        ├── orderId есть в localStorage / URL params
        │
        ├── Выбор провайдера (FREEDOM_PAY | PAYPAL)
        │
        ├── POST /api/v1/payments/init { orderId: 1234 }
        │   — бэкенд принимает, если order.status == PENDING_PAYMENT
        │
        └── window.location.href = paymentUrl
```

Backend уже поддерживает `POST /api/v1/payments/init` и `POST /api/v1/payments/paypal/create-order` с `orderId`. Никаких backend изменений для retry не нужно.

### Путь через OrderHistory

```
/orders (OrderHistoryPage) → строка с PENDING_PAYMENT
        └── кнопка «Оплатить» → открывает модалку/страницу с выбором провайдера
```

---

## 7. UX сценарии

### Сценарий 1: F5 после создания заказа
**Было:** пустая корзина, пользователь в растерянности
**Станет:** recovery banner «Незавершённый заказ №1234 · [Оплатить]»

### Сценарий 2: Отмена платежа на Freedom Pay
**Было:** `/payment/cancelled` → «Повторить» → пустая корзина
**Станет:** `/payment/cancelled` → «Повторить оплату заказа №1234» → страница выбора провайдера с уже существующим orderId

### Сценарий 3: Отказ карты
**Было:** `/payment/failed` → «Повторить» → пустая корзина
**Станет:** `/payment/failed` → «Попробовать другой способ оплаты» → тот же orderId, можно выбрать PayPal

### Сценарий 4: Закрыл вкладку, открыл снова
**Было:** пустая корзина без объяснений
**Станет:** recovery banner (localStorage сохранён)

### Сценарий 5: Успешная оплата
**Было:** `/payment/success` — всё работает
**Станет:** то же (localStorage запись удаляется)

### Сценарий 6: Тихая отмена (пользователь решил не платить)
**Было:** заказ висит 60 мин и экспирирует сам
**Станет:** кнопка «Отменить заказ» в recovery banner → явное действие + немедленное освобождение инвентаря

---

## 8. Что НЕ надо делать

- **Не надо** пересоздавать заказ при retry — двойная резервация инвентаря
- **Не надо** восстанавливать корзину из orderId — заказ уже создан, корзина неактуальна
- **Не надо** хранить весь `OrderResponse` в localStorage — достаточно orderId + snapshot для UI
- **Не надо** поллинг статуса — однократная проверка при mount достаточна

---

## 9. Файлы, которые будут изменены

| Файл | Изменение |
|------|-----------|
| `frontend/src/stores/cart.store.ts` | без изменений |
| `frontend/src/stores/checkout.store.ts` | без изменений (store не нужен для recovery) |
| `frontend/src/pages/CartPage.tsx` | recovery banner + логика восстановления completedOrder |
| `frontend/src/pages/PaymentCancelledPage.tsx` | кнопка «Повторить оплату» с orderId → новый flow |
| `frontend/src/pages/PaymentFailedPage.tsx` | кнопка «Повторить» с orderId → новый flow |
| `frontend/src/pages/PaymentSuccessPage.tsx` | читать/удалять из localStorage вместо sessionStorage |
| `frontend/src/pages/OrderHistoryPage.tsx` | кнопка «Оплатить» для PENDING_PAYMENT строк |
| `frontend/src/shared/lib/pending-payment.ts` | **новый** — утилиты для localStorage record |

---

## 10. Рекомендуемая архитектура

### Приоритет 1 (обязательно — LOSS-1, 2, 3, 4)

**localStorage pending payment + recovery banner на /cart**

1. При `orderMutation.onSuccess`: `localStorage.setItem(PENDING_KEY, JSON.stringify(record))`
2. При `onContinue` (пользователь нажал «Продолжить без оплаты»): удалить запись
3. После `PaymentSuccessPage` mount: удалить запись
4. `/cart` mount effect: прочитать запись → если `expiresAt > now` → `GET /api/v1/order/{id}` → если PENDING_PAYMENT → показать banner → восстановить `completedOrder`

**balgyn_last_payment — перенести из sessionStorage в localStorage** (решает LOSS-5, 5 строк)

---

### Приоритет 2 (UX кнопки retry)

**Кнопка «Оплатить» в OrderHistory для PENDING_PAYMENT**

- `OrderHistoryPage.tsx`: для строк с `status == PENDING_PAYMENT` добавить кнопку
- Кнопка: `navigate("/cart", { state: { retryOrderId: order.id } })`
- CartPage читает `location.state.retryOrderId` и восстанавливает `completedOrder` через API

**PaymentCancelledPage и PaymentFailedPage**

- Вместо `Link to="/cart"` — ссылка с state: `{retryOrderId}`
- Или отдельная страница `/payment/retry?orderId=X`

---

### Приоритет 3 (опционально — cross-device)

**Backend endpoint `/api/v1/order/my/pending`**

- Только если нужна поддержка смены устройства
- Авторизованный пользователь входит с телефона — видит recovery banner
- Не в текущем спринте

---

### Граничные случаи

| Случай | Решение |
|--------|---------|
| Два PENDING_PAYMENT заказа (race condition) | localStorage хранит только последний; API `/my/pending` возвращает самый новый |
| Заказ экспирировал пока пользователь на /cart | `GET /api/v1/order/{id}` вернёт EXPIRED → удалить запись, не показывать banner |
| Пользователь очистил localStorage | Заказ экспирирует за 60 мин (backend fallback) |
| Гость (не авторизован) | localStorage работает; backend endpoint недоступен — не критично |
| Оплата в новой вкладке | localStorage шарится между вкладками → всё работает |
