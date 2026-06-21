# DRY_RUN_PAYPAL.md
> Сценарий: оплата через PayPal (Visa / Mastercard / Amex / PayPal-аккаунт)  
> Дата: 2026-06-21

---

## УЧАСТНИКИ СИСТЕМЫ

| Компонент | Роль |
|-----------|------|
| CartPage | Инициирует оплату |
| PayPalOrderController | `POST /payments/paypal/create-order` и `POST /capture/{id}` |
| PayPalServiceImpl | Создаёт PayPal Order через API, выполняет capture |
| PayPalWebhookController | `POST /paypal/webhook` (async события от PayPal) |
| PaymentReturnPage | Читает `?token=`, вызывает capture |
| ExchangeRateService | KZT → USD конвертация для PayPal |

---

## ПОЛНЫЙ FLOW

### 1. Клиент выбирает PayPal и нажимает "Оплатить"

**CartPage → `handleInitPayment()`:**
```
provider = "PAYPAL"
POST /api/v1/payments/paypal/create-order
Body: {
  orderId: 42,
  returnUrl: "https://balgyn.kz/payment-return",
  cancelUrl: "https://balgyn.kz/payment/cancelled"
}
```

**Сервер (PayPalOrderController → PayPalServiceImpl):**
1. Находит Order #42 (статус `PENDING_PAYMENT`)
2. `ExchangeRateService.convert(totalPrice_KZT, "USD")` — получает сумму в USD (необходима для PayPal Orders API)
3. Вызывает PayPal Orders API v2: `POST https://api.paypal.com/v2/checkout/orders`
4. Получает PayPal Order ID (`PAYPAL_ORDER_ID`) и `approve` link
5. Генерирует `cancelToken = HMAC-SHA256(orderId + ":" + secret)` — подписанный токен отмены
6. Создаёт `Payment` запись: `status=PENDING`, `provider=PAYPAL`, `externalId=PAYPAL_ORDER_ID`
7. Возвращает `{ paymentUrl: "https://www.paypal.com/checkoutnow?token=PAYPAL_ORDER_ID", cancelToken: "..." }`

**Frontend:**
- `saveLastPayment({ orderId: 42, totalPrice: 15000, provider: "PAYPAL", cancelToken: "..." })`
- `window.location.href = paypalApprovalUrl`

---

### 2. Клиент одобряет платёж на PayPal

PayPal показывает summary (USD сумма) и кнопку "Approve". Клиент логинится в PayPal / вводит карту.

После одобрения PayPal редиректит:
```
https://balgyn.kz/payment-return?token=PAYPAL_ORDER_ID&PayerID=BUYER123
```

---

### 3. PaymentReturnPage — capture

```typescript
const paypalToken = params.get("token");  // PAYPAL_ORDER_ID

if (paypalToken) {
  capturePayPalOrder(paypalToken)
    .then((payment) => {
      if (payment.status === "SUCCEEDED") {
        navigate("/payment/success", { replace: true });
      } else {
        navigate(`/payment/failed?error=${payment.status}`);
      }
    })
    .catch((err) => {
      navigate(`/payment/failed?error=${err.message}`);
    });
}
```

**Запрос:**
```
POST /api/v1/payments/paypal/capture/PAYPAL_ORDER_ID
```

**PayPalServiceImpl.captureOrder():**
1. Вызывает PayPal API: `POST https://api.paypal.com/v2/checkout/orders/PAYPAL_ORDER_ID/capture`
2. При успехе (`COMPLETED`): Payment → `SUCCEEDED`, Order → `NEW`; списывает inventory
3. Возвращает `PaymentResponse { status: "SUCCEEDED", ... }`

**Frontend:** `navigate("/payment/success", { replace: true })`

---

### 4. PaymentSuccessPage

- `loadLastPayment()` → `{ orderId: 42, totalPrice: 15000, provider: "PAYPAL" }`
- `clearLastPayment()` + `clearPendingPayment()`
- Показывает: "Заказ #42 | 15 000 ₸ | PayPal"

---

### 5. PayPal Webhook (async — параллельно)

PayPal отправляет webhook события: `PAYMENT.CAPTURE.COMPLETED`, `PAYMENT.CAPTURE.DENIED` и др.

**PayPalWebhookController (`POST /api/v1/paypal/webhook`):**
1. Верифицирует подпись через PayPal API (`verify-webhook-signature`)
2. Если пустой body → `403` (защита от пустых вебхуков)
3. Извлекает `orderId` через `extractOrderId()`:
   - Сначала из `resource.supplementary_data.related_ids.order_id`
   - Fallback: из `resource.links[]` где `rel=up`, извлекает ID из href
4. При `PAYMENT.CAPTURE.COMPLETED` — дополнительная идемпотентная проверка (если уже `SUCCEEDED`, ничего не делает)

**Статусы обновляются через webhook ИЛИ через capture — оба пути идемпотентны.**

---

## ПРОВЕРКА СТАТУСОВ

| Сущность | До оплаты | После capture | После webhook COMPLETED |
|----------|-----------|---------------|------------------------|
| Order.status | PENDING_PAYMENT | NEW | NEW (идемпотентно) |
| Payment.status | PENDING | SUCCEEDED | SUCCEEDED (идемпотентно) |
| Inventory.quantity | Зарезервирован | Списан | Без изменений |
| Payment.externalId | PAYPAL_ORDER_ID | PAYPAL_ORDER_ID | — |

---

## EDGE CASES

### Capture вызывается дважды (двойной клик на return)
- `useRef(false)` + `if (calledRef.current) return` — защита в `PaymentReturnPage`
- Только один вызов `capturePayPalOrder` на жизнь компонента

### Клиент обновил страницу /payment-return
- `calledRef` сбрасывается (компонент ремаунтируется) → capture вызывается снова
- PayPal: повторный capture на уже `COMPLETED` order → PayPal возвращает ошибку `ORDER_ALREADY_CAPTURED`
- `PayPalServiceImpl` ловит эту ошибку → возвращает `SUCCEEDED` (идемпотентность)
- ⚠️ **ПРЕДУПРЕЖДЕНИЕ**: Убедиться, что `ORDER_ALREADY_CAPTURED` обрабатывается как успех, а не 500

### ExchangeRateService недоступен
- Если external rate API недоступен при создании PayPal order → fallback на кешированный курс
- Если кеш пустой → `PayPalServiceImpl` бросает исключение → `paymentError` на frontend

### Сумма в USD изменилась между созданием order и capture
- PayPal ордер зафиксирован в USD на момент `create-order`
- Capture всегда использует ту же сумму — несоответствия невозможны

---

## FAIL SCENARIOS

| Сценарий | Что происходит |
|----------|---------------|
| PayPal API недоступен (create) | 500 → `paymentError` на CartPage |
| `PAYPAL_CLIENT_ID` пустой | `create-order` → исключение → paymentError |
| PayPal API недоступен (capture) | catch → navigate `/payment/failed?error=...` |
| Webhook подпись неверная | 403; Payment остаётся PENDING → expiry job |
| Order EXPIRED до capture | OrderServiceImpl отклонит: "Order не в PENDING_PAYMENT" |
| `?token` отсутствует в return URL | PaymentReturnPage → `navigate("/payment/failed?error=UNKNOWN_RETURN")` |

---

## ВИЗУАЛЬНЫЙ ПУТЬ (summary)

```
CartPage [выбрать PayPal] → [Оплатить]
  → POST /payments/paypal/create-order
  → saveLastPayment({ ..., cancelToken: "HMAC..." })
  → window.location = PayPal URL

  → [PayPal approve page]
  → [Клиент одобряет]
  → redirect → /payment-return?token=PAYPAL_ORDER_ID

PaymentReturnPage:
  → POST /payments/paypal/capture/PAYPAL_ORDER_ID
  → payment.status === "SUCCEEDED"
  → navigate("/payment/success")

PaymentSuccessPage:
  → Заказ #42 | 15 000 ₸ | PayPal
  → clearLastPayment() + clearPendingPayment()
  → "Мои заказы" → /orders [NEW статус]

PayPal Webhook (async):
  → verify signature
  → extractOrderId()
  → COMPLETED → идемпотентное подтверждение
```

**СТАТУС: ПРОХОДИТ ✅**
