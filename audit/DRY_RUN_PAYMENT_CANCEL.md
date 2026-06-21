# DRY_RUN_PAYMENT_CANCEL.md
> Сценарий: отмена оплаты — Freedom Pay и PayPal  
> Дата: 2026-06-21

---

## СЦЕНАРИЙ A — Freedom Pay Cancel

### A1. Клиент нажимает "Отмена" на странице Freedom Pay

Freedom Pay редиректит клиента на `returnUrl` с параметром `pg_result=0`:

```
https://balgyn.kz/payment-return?pg_result=0&pg_order_id=42&pg_payment_id=FP-789
```

**PaymentReturnPage:**
```typescript
const fpResult = params.get("pg_result");  // "0"

if (fpResult !== null && fpResult !== "1") {
  const last = loadLastPayment();
  const orderId = last?.orderId ?? params.get("pg_order_id") ?? "";
  navigate(`/payment/failed?error=PAYMENT_FAILED&orderId=${orderId}`, { replace: true });
}
```

→ Редирект на `/payment/failed?error=PAYMENT_FAILED&orderId=42`

---

### A2. PaymentFailedPage

**Что видит пользователь:**  
Красный X circle. Заголовок "Оплата не прошла". Блок с номером заказа (#42). Коды ошибки (`PAYMENT_FAILED`). Кнопки: "Попробовать снова" и "В каталог".

**Данные:**
- `loadLastPayment()` → `{ orderId: 42, totalPrice: 15000, provider: "FREEDOM_PAY" }`
- Запись НЕ очищается (нужна для retry)

---

### A3. Freedom Pay Retry Flow

**Клиент нажимает "Попробовать снова":**
```typescript
navigate("/cart", {
  state: { retryOrderId: 42, retryAmount: 15000 }
});
```

**CartPage получает navigation state:**
```typescript
if (st?.retryOrderId && !completedOrder) {
  setPendingRecord({ orderId: 42, amount: 15000, provider: "FREEDOM_PAY", ... });
  setRecoveryProvider("FREEDOM_PAY");
}
// очищает state из URL
navigate(location.pathname, { replace: true, state: {} });
```

**RecoveryBanner:**
- Янтарный баннер: "Незавершённый платёж · Заказ #42 · 15 000 ₸"
- Выбор провайдера: Freedom Pay / PayPal (компактные кнопки)
- Кнопка "Оплатить" → `handleInitPayment({ orderId: 42, provider, amount: 15000 })`
- Кнопка "Отклонить" → `clearPendingPayment()` + `setPendingRecord(null)` → баннер исчезает

**Order #42:** остаётся `PENDING_PAYMENT` (резервация ещё активна, если <60 мин прошло).

---

### A4. Freedom Pay — localStorage Recovery (F5 / закрытие вкладки)

Если клиент закрыл вкладку ПОСЛЕ того как вернулся на /payment-return с pg_result=0, но ДО нажатия кнопки retry:

- `loadLastPayment()` при монтировании CartPage находит запись из localStorage
- `setPendingRecord((prev) => prev ?? record)` — показывает RecoveryBanner
- Клиент может оплатить снова без потери данных

---

## СЦЕНАРИЙ B — PayPal Cancel

### B1. Клиент нажимает "Отмена" на странице PayPal

PayPal редиректит на `cancelUrl`:
```
https://balgyn.kz/payment/cancelled?token=PAYPAL_ORDER_ID
```

---

### B2. PaymentCancelledPage

**Что видит пользователь:**  
Серый warning circle. Заголовок "Оплата отменена". Блок с номером заказа (если есть). Кнопки: "Попробовать снова" и "В каталог".

**При монтировании:**
```typescript
const info = loadLastPayment();  // { orderId: 42, cancelToken: "HMAC...", ... }
setPayment(info);

const token = params.get("token");  // PAYPAL_ORDER_ID
if (token && !cancelledRef.current) {
  cancelledRef.current = true;
  const cancelToken = info?.cancelToken ?? "";
  const qs = cancelToken ? `?cancelToken=${encodeURIComponent(cancelToken)}` : "";
  fetch(`${getApiBaseUrl()}/payments/paypal/cancel/${token}${qs}`, { method: "POST" })
    .catch(() => { /* best-effort */ });
}
```

**Backend (PayPalOrderController → PayPalServiceImpl.cancelOrder()):**
1. Верифицирует `cancelToken` через HMAC-SHA256
2. Если подпись верна → Payment → `CANCELLED`, Order остаётся `PENDING_PAYMENT`
3. Если пустой cancelToken (legacy) → `403`
4. PayPal Order сам по себе истекает (PayPal держит ордер ~3 часа)

**Защита:** без `cancelToken` бэкенд отклоняет запрос → злоумышленник не может отменить чужой заказ

---

### B3. PayPal Retry Flow

**Клиент нажимает "Попробовать снова":**
```typescript
navigate("/cart", {
  state: { retryOrderId: 42, retryAmount: 15000 }
});
```

Тот же RecoveryBanner, что и в сценарии A. Клиент может:
1. Снова нажать PayPal → `POST /create-order` → **новый** PayPal Order (с новым ID)
2. Переключиться на Freedom Pay → `POST /payments/init` → Freedom Pay URL

**Order #42:** по-прежнему `PENDING_PAYMENT`, резервация активна.

---

### B4. PayPal — провайдер отказал при approve (DECLINED)

**Редирект обратно:** PayPal может редиректить на `returnUrl` с ошибочным состоянием.

**PaymentReturnPage:**
- `capturePayPalOrder(token)` → PayPal API возвращает `DECLINED` или `INSTRUMENT_DECLINED`
- `PayPalServiceImpl` возвращает `PaymentResponse { status: "FAILED" }`
- Frontend: `navigate("/payment/failed?error=FAILED")`

→ PaymentFailedPage → retry flow как в A3

---

## EDGE CASE — Двойное нажатие Отмены

**cancelledRef.current = true** устанавливается при первом `POST /cancel/...`. Повторный mount (React Strict Mode, etc.) не вызовет второй запрос в production.

---

## RECOVERY FLOW — Полная схема

```
[Оплата не прошла] → /payment/failed
  → loadLastPayment() → orderId=42
  → НЕ очищает localStorage
  → handleRetry() → navigate("/cart", { state: { retryOrderId: 42 } })

[Отмена PayPal] → /payment/cancelled
  → POST /cancel/{token}?cancelToken=HMAC  (best-effort)
  → loadLastPayment() → orderId=42
  → handleRetry() → navigate("/cart", { state: { retryOrderId: 42 } })

CartPage (/cart):
  → priority 1: navigation.state.retryOrderId → setPendingRecord
  → priority 2: localStorage loadPendingPayment() → setPendingRecord
  
RecoveryBanner:
  → Amber banner "Незавершённый платёж · Заказ #42"
  → [Freedom Pay] [PayPal] buttons
  → [Оплатить] → handleInitPayment({ orderId: 42, provider, amount })
  → [Отклонить] → clearPendingPayment() + setPendingRecord(null)
```

---

## ПРОВЕРКА СТАТУСОВ

| Сценарий | Order.status | Payment.status | Recovery |
|----------|-------------|----------------|---------|
| FP отмена (pg_result=0) | PENDING_PAYMENT | FAILED | Да (navigator state + localStorage) |
| FP закрытие браузера | PENDING_PAYMENT | PENDING | Да (localStorage) |
| PP отмена кнопкой | PENDING_PAYMENT | CANCELLED | Да |
| PP истечение ~3h | PENDING_PAYMENT | PENDING | localStorage (если не expired) |
| Заказ истёк (>60 min) | EXPIRED | PENDING/CANCELLED | Нет (retry initPayment → 400) |

---

## ПРЕДУПРЕЖДЕНИЯ

1. **FP pg_result=0 ≠ callback**: callback с `pg_result=0` может прийти ОТ Freedom Pay с нулевым результатом. В текущей реализации `handleFreedomPayCallback` обрабатывает это как `FAILED`. Убедиться, что FP присылает `pg_result=0` в callback тоже (а не только в return URL).

2. **PayPal cancel без cancelToken**: если localStorage был очищен (другой браузер, incognito), `cancelToken = ""` → backend ответит `403` → `catch(() => {})` → платёж остаётся `PENDING`. Expiry job (@Scheduled) его зачистит через 60 минут.

3. **Retry после expiry**: если клиент пытается оплатить заказ, который уже EXPIRED, `POST /payments/init` вернёт ошибку `Order is not in PENDING_PAYMENT state` → `paymentError` на RecoveryBanner → пользователь должен создать новый заказ.

**СТАТУС: ПРОХОДИТ ✅ (с предупреждениями 1-3 выше)**
