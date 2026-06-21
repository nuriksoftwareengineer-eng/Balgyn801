# Zero-Config Setup

После `git clone` + `docker compose up --build` BALGYN полностью работоспособен — без `.env` файла, без ключей, без ручной настройки.

## Что работает сразу

| Компонент | Режим | Поведение |
|-----------|-------|-----------|
| PostgreSQL | prod | Реальная БД, 29 миграций Flyway |
| MinIO | prod | Реальное хранилище медиафайлов |
| CDEK доставка | mock | Расчёт доставки по мок-данным (sandbox API) |
| FreedomPay | **stub** | Оплата подтверждается мгновенно локально |
| PayPal | **stub** | Оплата подтверждается мгновенно локально |
| Каталог | seeded | 5 групп, 14 коллекций, 16 дизайнов автоматически |
| Администратор | default | admin@balgyn.local / admin12345 |

## Stub-режим платежей

### Как работает FreedomPay stub

1. Покупатель нажимает "Оплатить через Freedom Pay"
2. Фронтенд вызывает `POST /api/v1/payments/init`
3. Бэкенд возвращает `paymentUrl = http://localhost:8080/api/v1/payments/stub/freedom-pay/approve?orderId={id}`
4. Браузер переходит по этому URL
5. Бэкенд мгновенно:
   - Переводит платёж в статус `SUCCEEDED`
   - Переводит заказ в статус `CONFIRMED`
   - Редиректит на `http://localhost:5174/payment/success?orderId={id}`
6. В логах: `[PAYMENT-STUB] Order #N confirmed via FreedomPay stub`

### Как работает PayPal stub

1. Покупатель нажимает "Оплатить через PayPal"
2. Фронтенд вызывает `POST /api/v1/payments/paypal/create-order`
3. Бэкенд возвращает `paymentUrl = http://localhost:8080/api/v1/payments/stub/paypal/approve?paypalOrderId={id}&returnUrl={url}`
4. Браузер переходит по этому URL
5. Бэкенд редиректит на `{returnUrl}?token={paypalOrderId}&PayerID=STUB_PAYER` (имитирует PayPal)
6. Фронтенд вызывает `POST /api/v1/payments/paypal/capture/{token}`
7. Бэкенд подтверждает: статус `SUCCEEDED`, заказ `CONFIRMED`
8. В логах: `[PAYMENT-STUB] PayPal captureOrder stub — returning COMPLETED`

### Как активируется stub-режим

Stub активен автоматически когда переменные окружения пусты или не заданы:
- FreedomPay: `FREEDOMPAY_MERCHANT_ID` пусто ИЛИ `FREEDOMPAY_SECRET_KEY` пусто
- PayPal: `PAYPAL_CLIENT_ID` пусто ИЛИ `PAYPAL_CLIENT_SECRET` пусто

В docker-compose все эти переменные имеют пустые дефолты (`:-`), поэтому без `.env` файла stub включается автоматически.

### Stub endpoints

```
GET /api/v1/payments/stub/paypal/approve?paypalOrderId={id}&returnUrl={url}
    → 302 {returnUrl}?token={id}&PayerID=STUB_PAYER

GET /api/v1/payments/stub/freedom-pay/approve?orderId={id}
    → подтверждает заказ → 302 {successUrl}?orderId={id}
```

Stub endpoints возвращают 404, если соответствующий провайдер НЕ в stub-режиме (реальные ключи заданы).

## Dev defaults (без .env)

| Переменная | Значение по умолчанию |
|-----------|----------------------|
| `JWT_SECRET` | `balgyn-dev-jwt-secret-for-local-development-only-change-in-prod` |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@balgyn.local` |
| `BOOTSTRAP_ADMIN_PASSWORD` | `admin12345` |
| `POSTGRES_PASSWORD` | `postgres` |
| `APP_SEED_CATALOG` | `true` |
| `FREEDOMPAY_MERCHANT_ID` | `` (stub mode) |
| `PAYPAL_CLIENT_ID` | `` (stub mode) |

> **Production**: все эти значения ДОЛЖНЫ быть заменены через `.env` или CI/CD secrets.

## Включение реальных платежей

### FreedomPay (sandbox)
```bash
FREEDOMPAY_MERCHANT_ID=ваш_id
FREEDOMPAY_SECRET_KEY=ваш_ключ
FREEDOMPAY_RESULT_URL=https://your-ngrok-url/api/v1/payments/callback/freedom-pay
FREEDOMPAY_TESTING_MODE=true
```

### PayPal (sandbox)
```bash
PAYPAL_CLIENT_ID=ваш_client_id
PAYPAL_CLIENT_SECRET=ваш_client_secret
PAYPAL_WEBHOOK_ID=ваш_webhook_id
PAYPAL_MODE=sandbox
```

## Проверка stub-режима при старте

В логах бэкенда при первом платеже вы увидите:
```
[PAYMENT-STUB] FreedomPay running in stub mode — no real API call. orderId=1
[PAYMENT-STUB] Order #1 confirmed via FreedomPay stub
```
или
```
[PAYMENT-STUB] PayPal running in stub mode — no real API call. stubOrderId=stub-pp-abc123
[PAYMENT-STUB] PayPal captureOrder stub — returning COMPLETED. paypalOrderId=stub-pp-abc123
```
