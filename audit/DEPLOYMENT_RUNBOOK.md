# DEPLOYMENT_RUNBOOK.md
> Balgyn — Пошаговый деплой на production + чек-лист первого платежа  
> Дата: 2026-06-21 | Java: 133/133 | Flyway: V1–V28

---

## ВЕРИФИКАЦИЯ ПРЕДУПРЕЖДЕНИЙ W1, W2, W3

### W1 — CDEK webhook-token (CONFIRMED RISK)

**Код:** `application.properties:69`
```properties
cdek.webhook-token=${CDEK_WEBHOOK_TOKEN:}
```
Пустой дефолт. `application-prod.properties` не добавляет `:?` принудительно.

**Что происходит без токена:** `CdekWebhookController` логирует warn и принимает любой POST на `/delivery/cdek/webhook`. Злоумышленник, зная формат запроса CDEK, может поставить произвольный `cdekOrderUuid` и пометить отправление как DELIVERED.

**Финансовый ущерб:** нет (webhook не инициирует платежи/refund).  
**Операционный ущерб:** статус заказа может быть неверно обновлён.

**Решение в runbook:** установить `CDEK_WEBHOOK_TOKEN` ДО регистрации вебхука в CDEK личном кабинете.

---

### W2 — CDEK double shipment create (CONFIRMED RISK)

**Код:** `CdekOrderServiceImpl.java:56-62`
```java
CdekShipment shipment = shipmentRepository.findByOrder_Id(orderId)
    .orElseGet(() -> {
        CdekShipment s = new CdekShipment();
        s.setOrder(order);
        s.setCreatedAt(LocalDateTime.now());
        return s;
    });
// ... затем:
ShipmentResult result = provider.createShipment(request);  // CDEK API call
shipment.setCdekOrderUuid(result.cdekOrderUuid());         // перезаписывает старый UUID
```

**Что происходит при повторном нажатии "Создать отправление":**
1. `findByOrder_Id()` находит существующий `CdekShipment` (upsert)
2. `provider.createShipment()` снова вызывает CDEK API → создаёт НОВЫЙ заказ в CDEK
3. Старый CDEK-UUID (`cdekOrderUuid`) перезаписывается в БД
4. Старый заказ в CDEK остаётся открытым → CDEK может списать за оба

`window.confirm("Повторить создание?")` защищает от случайного клика, но не от намеренного.

**Решение в runbook:** если нужно пересоздать отправление → сначала отменить (`cancelShipment`) → затем создать. Документировать в SOP для admin.

---

### W3 — PayPal double capture при F5 (SAFE ✅)

**Код:** `PayPalServiceImpl.java:167-176`
```java
Payment payment = paymentRepository.findByProviderPaymentIdForUpdate(paypalOrderId);
// pessimistic lock ↑

if (payment.getStatus() != PaymentStatus.PENDING) {
    log.info("[PayPal] captureOrder: already in status={}, skipping", payment.getStatus());
    return toResponse(payment);  // ← ранний return, PayPal API не вызывается
}
```

**При F5 на `/payment-return`:**
- Первый запрос: `status=PENDING` → capture → `status=SUCCEEDED`
- Второй запрос: `status=SUCCEEDED` → ранний return → `PaymentResponse{status=SUCCEEDED}`
- Frontend: `payment.status === "SUCCEEDED"` → `/payment/success`

**Дополнительная защита:** `findByProviderPaymentIdForUpdate` — SELECT FOR UPDATE, исключает гонку при параллельных запросах.

**Вердикт: W3 безопасен. Никаких изменений не требуется.**

---

## DEPLOYMENT RUNBOOK

### Предварительные требования

| Требование | Проверка |
|-----------|---------|
| VPS/сервер Ubuntu 22.04+ с Docker 24+ | `docker --version` |
| Docker Compose v2.20+ | `docker compose version` |
| Публичный IP и домен `balgyn.kz` | DNS A-запись указывает на сервер |
| Порты 80 и 443 открыты в firewall | `ufw allow 80; ufw allow 443` |
| Реквизиты Freedom Pay | Merchant ID + Secret Key + Result URL |
| Реквизиты PayPal | Client ID + Secret + Webhook ID |
| SSH-доступ к серверу | `ssh user@server` |

---

### ШАГ 1 — Клонирование и подготовка

```bash
# На сервере:
git clone <repo_url> /opt/balgyn
cd /opt/balgyn

# Проверить что файлы на месте:
ls docker-compose.prod.yml nginx/ scripts/
```

---

### ШАГ 2 — Создание .env.prod

Создать `/opt/balgyn/.env.prod` со следующими переменными (все обязательны):

```env
# ── Домен ──────────────────────────────────────────────────────
DOMAIN=balgyn.kz
CERTBOT_EMAIL=admin@balgyn.kz

# ── Database ───────────────────────────────────────────────────
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/balgyn
SPRING_DATASOURCE_USERNAME=balgyn
SPRING_DATASOURCE_PASSWORD=<STRONG_DB_PASSWORD>
POSTGRES_DB=balgyn
POSTGRES_USER=balgyn
POSTGRES_PASSWORD=<STRONG_DB_PASSWORD>

# ── JWT (минимум 32 символа) ───────────────────────────────────
JWT_SECRET=<RANDOM_64_CHAR_HEX>

# ── Spring profile ─────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=prod

# ── MinIO ──────────────────────────────────────────────────────
MINIO_ROOT_USER=balgyn_minio
MINIO_ROOT_PASSWORD=<STRONG_MINIO_PASSWORD>
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_URL=https://balgyn.kz/media
MINIO_BUCKET=balgyn-media
STORAGE_ENABLED=true

# ── CORS ───────────────────────────────────────────────────────
ALLOWED_ORIGINS=https://balgyn.kz

# ── Freedom Pay ────────────────────────────────────────────────
FREEDOMPAY_MERCHANT_ID=<YOUR_MERCHANT_ID>
FREEDOMPAY_SECRET_KEY=<YOUR_SECRET_KEY>
FREEDOMPAY_RESULT_URL=https://balgyn.kz/api/v1/payments/callback/freedom-pay

# ── PayPal ─────────────────────────────────────────────────────
PAYPAL_CLIENT_ID=<YOUR_CLIENT_ID>
PAYPAL_CLIENT_SECRET=<YOUR_CLIENT_SECRET>
PAYPAL_WEBHOOK_ID=<YOUR_WEBHOOK_ID>
PAYPAL_BASE_URL=https://api.paypal.com

# ── CDEK ───────────────────────────────────────────────────────
CDEK_CLIENT_ID=<YOUR_CDEK_CLIENT_ID>
CDEK_CLIENT_SECRET=<YOUR_CDEK_CLIENT_SECRET>
CDEK_BASE_URL=https://api.cdek.ru/v2
CDEK_SENDER_CITY=270
# ВНИМАНИЕ (W1): установить ОБЯЗАТЕЛЬНО перед регистрацией вебхука в CDEK ЛК
CDEK_WEBHOOK_TOKEN=<RANDOM_32_CHAR_HEX>

# ── Admin bootstrap (только при первом старте) ─────────────────
BOOTSTRAP_ADMIN_EMAIL=admin@balgyn.kz
BOOTSTRAP_ADMIN_PASSWORD=<STRONG_ADMIN_PASSWORD>

# ── App settings ───────────────────────────────────────────────
ORDER_PAYMENT_WINDOW_MINUTES=60
FRONTEND_BASE_URL=https://balgyn.kz
```

**Сгенерировать случайные секреты:**
```bash
openssl rand -hex 32   # для JWT_SECRET
openssl rand -hex 16   # для CDEK_WEBHOOK_TOKEN
openssl rand -base64 24  # для паролей
```

---

### ШАГ 3 — Первый запуск (получение TLS-сертификата)

```bash
cd /opt/balgyn

# 1. Запустить только nginx с HTTP (для certbot challenge):
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d nginx

# 2. Получить сертификат:
docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm certbot certonly \
  --webroot --webroot-path=/var/www/certbot \
  -d balgyn.kz -d www.balgyn.kz \
  --email admin@balgyn.kz --agree-tos --non-interactive

# 3. Проверить наличие сертификата:
ls /etc/letsencrypt/live/balgyn.kz/fullchain.pem
```

---

### ШАГ 4 — Запуск всех сервисов

```bash
cd /opt/balgyn

docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Проверить статус:
docker compose -f docker-compose.prod.yml ps
```

Ожидаемый результат — все сервисы `running (healthy)`:
```
NAME          STATUS          PORTS
balgyn-app    running(healthy)  :8080
balgyn-db     running(healthy)  :5432
balgyn-minio  running(healthy)  :9000
balgyn-nginx  running(healthy)  0.0.0.0:80->80, 0.0.0.0:443->443
db-backup     running
certbot       exited(0)
```

---

### ШАГ 5 — Проверка Flyway миграций

```bash
docker compose -f docker-compose.prod.yml logs app | grep -E "Flyway|migration|error" | head -30
```

Ожидается:
```
Successfully applied N migrations to schema "public" (execution time ...ms)
```

При ошибке миграции — **не перезапускать автоматически**. Читать лог, откатить вручную.

---

### ШАГ 6 — Проверка health-endpoint

```bash
curl -f https://balgyn.kz/actuator/health
# Expected: {"status":"UP","components":{"db":{"status":"UP"},...}}
```

---

### ШАГ 7 — Создание MinIO bucket

```bash
# Зайти в MinIO console: https://balgyn.kz:9001 (или порт из compose)
# Login: MINIO_ROOT_USER / MINIO_ROOT_PASSWORD
# Создать bucket "balgyn-media" с публичным доступом на read

# Или через CLI:
docker exec balgyn-minio mc alias set local http://localhost:9000 $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD
docker exec balgyn-minio mc mb local/balgyn-media
docker exec balgyn-minio mc anonymous set download local/balgyn-media
```

---

### ШАГ 8 — Регистрация PayPal Webhook

1. Зайти в [PayPal Developer Dashboard](https://developer.paypal.com/dashboard/applications)
2. Выбрать приложение → Webhooks → Add Webhook
3. URL: `https://balgyn.kz/api/v1/paypal/webhook`
4. Events: `PAYMENT.CAPTURE.COMPLETED`, `PAYMENT.CAPTURE.DENIED`, `PAYMENT.CAPTURE.REFUNDED`
5. Скопировать Webhook ID → обновить `PAYPAL_WEBHOOK_ID` в `.env.prod`
6. Перезапустить app: `docker compose -f docker-compose.prod.yml --env-file .env.prod restart app`

---

### ШАГ 9 — Регистрация CDEK Webhook (W1-fix)

**Обязательно: сначала убедиться что `CDEK_WEBHOOK_TOKEN` задан в `.env.prod`.**

1. Войти в [CDEK личный кабинет](https://seller.cdek.ru/)
2. Интеграции → Вебхуки → Добавить
3. URL: `https://balgyn.kz/api/v1/delivery/cdek/webhook`
4. Токен: то же значение что в `CDEK_WEBHOOK_TOKEN`
5. События: ORDER_STATUS

---

### ШАГ 10 — Создание контента

1. Открыть `https://balgyn.kz/login`
2. Войти под `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`
3. Перейти в `/admin`
4. Создать: Группу → Коллекцию → Дизайн → загрузить фото → добавить варианты → установить цены → установить остатки → **Опубликовать**
5. Проверить: `/catalog` показывает дизайн, `/catalog/.../.../<slug>` открывается

---

## ЧЕК-ЛИСТ ПЕРВОГО РЕАЛЬНОГО ПЛАТЕЖА

Перед публичным запуском провести тест-оплату с реальной картой/аккаунтом.

### FREEDOM PAY — Тест-платёж

```
[ ] 1. Открыть /catalog → выбрать дизайн → добавить в корзину
[ ] 2. Перейти /cart → войти → оформить заказ (Pickup, KZ)
[ ] 3. На экране OrderSuccess выбрать "Банковская карта" → [Оплатить]
[ ] 4. Убедиться: редирект на pay.freedompay.money (HTTPS)
[ ] 5. Ввести данные тестовой карты (или реальной для полного теста)
[ ] 6. После оплаты: редирект на /payment-return → spinner → /payment/success
[ ] 7. Проверить /payment/success: показывает orderId, сумму, "Freedom Pay"
[ ] 8. Проверить /orders: заказ в статусе CONFIRMED (не PENDING_PAYMENT)
[ ] 9. В /admin/orders: заказ виден, статус CONFIRMED
[ ] 10. В /admin/payments: Payment.status = SUCCEEDED, Provider = FREEDOM_PAY
[ ] 11. Проверить логи: "Callback received: pg_result=1" + "Callback processed OK"
[ ] 12. Inventory: количество уменьшилось на 1 (проверить в /admin/designs/<id>/variants)
```

**Если pg_result=0 в логах при успешной оплате:** несоответствие URL callback — проверить `FREEDOMPAY_RESULT_URL`.

**Если signature invalid:** пересмотреть `FREEDOMPAY_SECRET_KEY`.

---

### PAYPAL — Тест-платёж

```
[ ] 1. Открыть /cart → оформить новый заказ (те же шаги)
[ ] 2. На OrderSuccess выбрать "PayPal" → [Оплатить]
[ ] 3. Убедиться: редирект на www.paypal.com (HTTPS)
[ ] 4. Войти в PayPal аккаунт / ввести карту
[ ] 5. Одобрить платёж
[ ] 6. Редирект на /payment-return?token=... → spinner (capture) → /payment/success
[ ] 7. Проверить /payment/success: orderId, сумма в ₸, "PayPal"
[ ] 8. Проверить /orders: статус CONFIRMED
[ ] 9. В /admin/payments: Payment.status = SUCCEEDED, Provider = PAYPAL
[ ] 10. Проверить логи: "[PayPal] Created order ... captureOrder ... confirmed"
[ ] 11. Webhook: проверить PayPal dashboard → Webhook logs → PAYMENT.CAPTURE.COMPLETED received
[ ] 12. Тест отмены: повторить flow, на PayPal нажать "Cancel"
[ ] 13. Редирект на /payment/cancelled: показывает orderId, кнопка "Попробовать снова"
[ ] 14. Нажать "Попробовать снова": CartPage показывает RecoveryBanner (янтарный)
[ ] 15. Повторить оплату через FP: успешно завершается
```

**Если capture возвращает INSTRUMENT_DECLINED:** покупатель ввёл недостаточно средств — нормальное поведение, тест failed → /payment/failed → retry.

**Если cancelToken rejected (403):** `JWT_SECRET` не совпадает между созданием order и отменой. При рестарте app между этими действиями — это нормально; backend expiry job зачистит PENDING payment через 60 мин.

---

## ОПЕРАЦИОННЫЕ ПРОЦЕДУРЫ (SOP)

### SOP-1: Пересоздание CDEK отправления (W2-процедура)

**Запрещено:** нажимать "Создать отправление" если отправление уже существует без предварительной отмены.

**Правильная последовательность:**
1. `/admin/orders/<id>` → убедиться что статус отправления не DELIVERED/IN_TRANSIT
2. Нажать "Отменить отправление" → `window.confirm` → подтвердить
3. Проверить в CDEK ЛК что старый заказ отменён
4. Нажать "Создать отправление" → `window.confirm` → подтвердить
5. Проверить новый `trackingNumber` в CDEK ЛК

### SOP-2: Rollback при ошибке Flyway миграции

```bash
# НЕ перезапускать app автоматически при ошибке миграции

# 1. Получить лог:
docker compose -f docker-compose.prod.yml logs app | grep -E "FlywayException|migration|ERROR"

# 2. Зайти в БД:
docker exec -it balgyn-db psql -U balgyn -d balgyn

# 3. Проверить состояние:
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

# 4. При partially applied migration — откат вручную, затем пометить как repaired:
-- DELETE FROM flyway_schema_history WHERE version='28' AND success=false;

# 5. После ручного отката — перезапустить:
docker compose -f docker-compose.prod.yml --env-file .env.prod restart app
```

### SOP-3: Обновление TLS-сертификата (каждые 90 дней)

```bash
# Автоматически через certbot (задан cron в docker-compose.prod.yml):
docker compose -f docker-compose.prod.yml run --rm certbot renew
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
```

### SOP-4: Ручной backup базы данных

```bash
# pg_dump sidecar делает это автоматически каждые 24ч.
# Ручной backup при необходимости:
docker exec balgyn-db pg_dump -U balgyn balgyn > /opt/backups/manual_$(date +%Y%m%d_%H%M%S).sql
```

---

## POST-LAUNCH CHECKLIST (первые 24 часа)

```
[ ] Actuator health: https://balgyn.kz/actuator/health → {"status":"UP"}
[ ] TLS grade A: https://www.ssllabs.com/ssltest/analyze.html?d=balgyn.kz
[ ] CSP header присутствует: curl -I https://balgyn.kz | grep content-security-policy
[ ] Логи app без ERROR: docker compose logs app | grep ERROR | tail -20
[ ] pg_dump работает: ls -la /opt/backups/ (файлы моложе 25ч)
[ ] Первая оплата FP прошла (чек-лист выше)
[ ] Первая оплата PayPal прошла (чек-лист выше)
[ ] CDEK webhook настроен с CDEK_WEBHOOK_TOKEN (W1 fix)
[ ] MinIO bucket balgyn-media создан и доступен
[ ] Admin может загрузить изображение дизайна (MinIO тест)
```
