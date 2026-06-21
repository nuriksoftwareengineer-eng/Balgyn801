# DRY_RUN_FREEDOMPAY.md
> Сценарий: оплата через Freedom Pay (Kaspi / Halyk / Visa / Mastercard)  
> Дата: 2026-06-21

---

## УЧАСТНИКИ СИСТЕМЫ

| Компонент | Роль |
|-----------|------|
| CartPage | Инициирует оплату |
| PaymentController | `POST /payments/init` |
| FreedomPayServiceImpl | Строит URL, подписывает MD5 |
| FreedomPayCallbackController | `POST /callback/freedom-pay` (принимает результат) |
| PaymentService.handleFreedomPayCallback() | Обновляет статусы |
| PaymentReturnPage | Читает `?pg_result` и редиректит |

---

## ПОЛНЫЙ FLOW

### 1. Клиент выбирает Freedom Pay и нажимает "Оплатить"

**CartPage → `handleInitPayment()`:**
```
provider = "FREEDOM_PAY"
POST /api/v1/payments/init
Body: { orderId: 42, provider: "FREEDOM_PAY", returnUrl: "https://balgyn.kz/payment-return" }
```

**Сервер (PaymentController → FreedomPayServiceImpl):**
- Ищет Order по `orderId`, проверяет статус `PENDING_PAYMENT`
- Создаёт `Payment` запись: `status=PENDING`, `provider=FREEDOM_PAY`, `externalId=null`
- Строит URL: `https://pay.freedompay.money/payment.php?pg_merchant_id=...&pg_order_id=42&pg_amount=...&pg_result_url=.../callback/freedom-pay&pg_sig=<md5>`
- Возвращает `{ paymentUrl: "https://pay.freedompay.money/..." }`

**Frontend:**
- `saveLastPayment({ orderId: 42, totalPrice: 15000, provider: "FREEDOM_PAY" })`
- `window.location.href = paymentUrl` — уход на Freedom Pay

---

### 2. Клиент оплачивает на странице Freedom Pay

Клиент видит платёжную форму Freedom Pay. Вводит данные карты.

Freedom Pay внутренне авторизует карту → в случае успеха POSTит callback **до редиректа клиента**.

---

### 3. Freedom Pay → Callback (Server-to-server)

**Запрос от Freedom Pay:**
```
POST /api/v1/payments/callback/freedom-pay
Content-Type: application/x-www-form-urlencoded

pg_merchant_id=12345
pg_order_id=42
pg_payment_id=FP-789
pg_result=1
pg_amount=15000
pg_salt=random_salt
pg_sig=<md5_signature>
```

**FreedomPayCallbackController:**
1. Проверяет `pg_sig` через `FreedomPaySignature.verify()` (MD5):
   - Если `secretKey` пустой → логирует warn + возвращает `xmlRejected("Invalid signature")` (защита от blank-secret уже проверена при старте)
   - Если подпись неверна → `xmlRejected("Invalid signature")`
2. При успешной верификации → `paymentService.handleFreedomPayCallback(params)`
3. Возвращает `<?xml ...><pg_status>ok</pg_status></response>`

**paymentService.handleFreedomPayCallback(params):**
- Ищет Payment по `pg_order_id`
- Если `pg_result == "1"`: Payment → `SUCCEEDED`, Order → `NEW`; списывает inventory
- Если `pg_result == "0"`: Payment → `FAILED`; inventory освобождается (expiry job или явно)
- Если Order уже `CANCELLED`/`EXPIRED`: бросает `BusinessRuleException` → callback возвращает `xmlRejected()`

---

### 4. Freedom Pay редиректит клиента обратно

**URL возврата:**
```
https://balgyn.kz/payment-return?pg_result=1&pg_order_id=42&pg_payment_id=FP-789&pg_sig=...
```

---

### 5. PaymentReturnPage обрабатывает возврат

```typescript
const fpResult = params.get("pg_result");  // "1"

if (fpResult === "1") {
  navigate("/payment/success", { replace: true });
}
```

Redirect на `/payment/success`.

---

### 6. PaymentSuccessPage

- `loadLastPayment()` → `{ orderId: 42, totalPrice: 15000, provider: "FREEDOM_PAY" }`
- `clearLastPayment()` + `clearPendingPayment()`
- Показывает: "Заказ #42, 15 000 ₸, Freedom Pay"

---

## ПРОВЕРКА СТАТУСОВ

| Сущность | До оплаты | После успеха | После failure |
|----------|-----------|--------------|---------------|
| Order.status | PENDING_PAYMENT | NEW | PENDING_PAYMENT → expiry → EXPIRED |
| Payment.status | PENDING | SUCCEEDED | FAILED |
| Inventory.quantity | Зарезервирован | Списан (deducted) | Освобождён |

---

## EDGE CASES

### Callback пришёл дважды (retry от Freedom Pay)
- `paymentService.handleFreedomPayCallback` проверяет текущий статус Payment
- Если уже `SUCCEEDED` → идемпотентный: повторный вызов возвращает ok без изменений (или throws BusinessRuleException) → лог + xmlRejected; Freedom Pay прекращает ретраи

### Клиент закрыл браузер после оплаты (callback пришёл, клиент не вернулся)
- Callback обработан: Order=NEW, Payment=SUCCEEDED
- При следующем открытии `/cart` → `loadPendingPayment()` из localStorage → RecoveryBanner
- RecoveryBanner предлагает оплатить → клиент нажимает → `POST /payments/init` → если Order уже NEW и Payment SUCCEEDED → заказ не создаётся повторно
- ⚠️ **ПРЕДУПРЕЖДЕНИЕ**: Если Payment уже SUCCEEDED и клиент снова инициирует оплату → сервер должен отклонить (проверяется статус Order → не PENDING_PAYMENT → ошибка 400)

### Клиент платит в момент истечения резервации (race condition)
- OrderExpiry @Scheduled job переводит PENDING_PAYMENT → EXPIRED + освобождает inventory
- Freedom Pay callback приходит после истечения → `BusinessRuleException("Order is not in PENDING_PAYMENT state")` → `xmlRejected` → Freedom Pay должен сделать refund
- ⚠️ Автоматического refund нет — нужно ручное вмешательство admin

---

## FAIL SCENARIOS

| Сценарий | Что происходит |
|----------|---------------|
| `FREEDOMPAY_SECRET_KEY` пустой | Callback всегда отклоняется (`secretKey.isBlank()` → warn + reject) |
| `pg_sig` неверный | `xmlRejected("Invalid signature")` |
| Order EXPIRED до callback | `BusinessRuleException` → `xmlRejected` |
| `pg_result=0` (клиент отменил) | PaymentReturnPage → `/payment/failed?error=PAYMENT_FAILED&orderId=42` |
| Сеть разорвана до redirect | Клиент не вернулся; callback обработан; recovery banner на /cart |

---

## ВИЗУАЛЬНЫЙ ПУТЬ (summary)

```
CartPage [выбрать FP] → [Оплатить]
  → POST /payments/init
  → window.location = FP URL
  → [Freedom Pay страница]
  → [Ввод карты]
  → [FP→ POST /callback]   ← server-to-server
  → [FP redirect] → /payment-return?pg_result=1
  → navigate("/payment/success")
  → PaymentSuccessPage [✓ Заказ #42 | 15 000 ₸ | Freedom Pay]
  → /orders [NEW статус]
```

**СТАТУС: ПРОХОДИТ ✅**
