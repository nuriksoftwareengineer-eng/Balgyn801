# Balgyn801 — Осталось сделать

Полный порядок и схемы: `docs/PLAN_AND_ARCHITECTURE.md`.

## Этап 2 — Медиа (хвост после того, что уже в коде)

В репозитории уже есть MinIO в `docker-compose.yml`, загрузка `POST /api/v1/media/upload` и кнопка загрузки в админке товаров. Дальше по смыслу плана:

- [ ] Прод: секреты и URL MinIO/S3 только через env, согласованный публичный доступ к файлам.
- [ ] При необходимости: presigned URL, учёт `objectKey` у товара, отказ от ручного URL в админке как основного сценария.

## Этап 4 — Учётные записи и прод-безопасность

- [ ] Безопасный сценарий выдачи роли ADMIN.
- [ ] Стратегия сессий: refresh-токены или короткий TTL access.
- [ ] Прод: сильный `JWT_SECRET`, ограничение Swagger, CORS только для боевых origin, HTTPS.

## Этап 5 — Доставка и оплата

Детальный чеклист (СДЭК v2, ЮKassa, PayPal, Kaspi, вебхуки, env, сквозной поток) — в **`docs/PLAN_AND_ARCHITECTURE.md`**, раздел **«Этап 5»**.

## Следующий шаг

Закрыть хвост этапа 2 (прод и политика медиа), затем этап 4 (безопасность и прод).

## CDEK runtime checklist (env + security)

Чтобы расчёт СДЭК работал стабильно и одинаково в checkout:

- Backend должен быть пересобран после изменений security/доставки (`docker compose up -d --build app`).
- Публичные маршруты доставки должны быть открыты в `SecurityConfig`:
  - `GET /api/v1/delivery/cdek/**`
  - `POST /api/v1/delivery/cdek/calculate`
  - `POST /api/v1/delivery/cdek/calculate-order`
- Для реального CDEK API задайте env:
  - `CDEK_BASE_URL` (`https://api.edu.cdek.ru/v2` sandbox или `https://api.cdek.ru/v2` prod)
  - `CDEK_CLIENT_ID`
  - `CDEK_CLIENT_SECRET`
  - `CDEK_SENDER_CITY`
  - `CDEK_DEFAULT_TARIFF`
- Если ключи не заданы, сервис работает в stub-режиме (`sourcedFromStub: true`), что подходит для локальной разработки.

## Как проверить СДЭК (smoke)

### 1) Поднять окружение

```bash
docker compose up -d --build app db minio
```

### 2) Проверить справочники СДЭК

```bash
curl --get "http://localhost:8080/api/v1/delivery/cdek/cities" \
  --data-urlencode "q=Алм" \
  --data "limit=5"
```

Ожидаемо: массив городов, например `Алматы`.

### 3) Проверить расчёт доставки по корзине

```bash
curl -X POST "http://localhost:8080/api/v1/delivery/cdek/calculate-order" \
  -H "Content-Type: application/json" \
  -d '{
    "toCityCode": 270,
    "items": [
      { "productId": 4, "quantity": 1 }
    ]
  }'
```

Ожидаемо в ответе:
- `deliveryPrice`
- `itemsTotal`
- `orderTotal`
- `estimatedWeightGrams`

И `orderTotal = itemsTotal + deliveryPrice`.

### 4) Проверить создание заказа CDEK

В `POST /api/v1/order` передаётся `deliveryFee` из расчёта (`deliveryPrice`).
Если отправить подменённую сумму — бэкенд должен вернуть `400`.

## Как проверить оплату (текущий stub-пайплайн)

Сейчас реализован каркас оплаты: `init` + `webhook`, без реального эквайринга.

## Оплата: классы и ответственность

Ниже текущая структура payment-модуля на бэкенде:

- `domain/Payment.java`
  - сущность платежа (`order`, `provider`, `status`, `amount`, `currency`, `providerPaymentId`, `paymentUrl`, webhook-поля, timestamps).
- `enums/PaymentProvider.java`
  - поддерживаемые провайдеры: `KASPI`, `YOOKASSA`, `PAYPAL`.
- `enums/PaymentStatus.java`
  - внутренние статусы: `PENDING`, `SUCCEEDED`, `CANCELLED`, `FAILED`, `REFUNDED`.
- `repositories/PaymentRepository.java`
  - доступ к платежам (`findById`, `findByProviderPaymentId` и т.д.).

API-контракт:

- `api/PaymentApi.java`
  - `POST /api/v1/payments/init`
  - `POST /api/v1/payments/webhook/{provider}`
- `controller/PaymentController.java`
  - тонкий слой: принимает HTTP и делегирует в сервис.

DTO:

- `dto/request/PaymentInitRequest.java`
  - вход для инициализации (`orderId`, `provider`, `returnUrl`).
- `dto/request/PaymentWebhookRequest.java`
  - вход для вебхука (`paymentId`/`providerPaymentId`, `status`, `eventId`, `payload`).
- `dto/responce/PaymentResponse.java`
  - унифицированный ответ в API.

Бизнес-логика:

- `service/PaymentService.java`
  - интерфейс use-case’ов оплаты (`initPayment`, `handleWebhook`).
- `service/Impl/PaymentServiceImpl.java`
  - основная логика:
    - проверка заказа перед init;
    - создание `Payment` со статусом `PENDING`;
    - генерация stub `providerPaymentId` и `paymentUrl`;
    - обработка webhook, маппинг внешнего статуса в `PaymentStatus`;
    - при `SUCCEEDED` перевод заказа `NEW -> CONFIRMED`.

Безопасность:

- `security/SecurityConfig.java`
  - `POST /api/v1/payments/init` открыт для checkout;
  - `POST /api/v1/payments/webhook/**` открыт для callback’ов провайдеров.

### 1) Инициализация платежа

```bash
curl -X POST "http://localhost:8080/api/v1/payments/init" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 7,
    "provider": "KASPI",
    "returnUrl": "http://localhost:5174/payment-return"
  }'
```

Ожидаемо:
- `status = "PENDING"`
- есть `providerPaymentId`
- есть `paymentUrl`

### 2) Вебхук оплаты (ручной smoke)

```bash
curl -X POST "http://localhost:8080/api/v1/payments/webhook/KASPI" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": 1,
    "eventId": "smoke-event-1",
    "status": "succeeded",
    "providerPaymentId": "stub-kaspi-...",
    "orderId": 7
  }'
```

Ожидаемо:
- платёж переходит в `SUCCEEDED`
- если заказ был `NEW`, он становится `CONFIRMED`

## Важно про оплату

- Это пока **stub-реализация** для отладки флоу.
- Реальный вызов Kaspi/YooKassa/PayPal API ещё не подключён.
- Следующий шаг: подключение провайдерных клиентов, подпись вебхуков, идемпотентность, реальный redirect URL.
