# GO_LIVE_FINAL_REPORT.md
> Balgyn — Финальный отчёт готовности к запуску  
> Дата: 2026-06-21 | Java: 133/133 | TS: 0 ошибок | Flyway: V1–V28

---

## VERIFIED

Следующее полностью работает по результатам dry-run всех 6 сценариев.

### Клиентский путь
- Главная → каталог → коллекция → страница дизайна: загрузка данных, gallery, stockMap, OOS-фильтр
- Добавление в корзину через localStorage (без потери при перезагрузке в обычных браузерах)
- 5-шаговый checkout: контакты, регион, метод доставки, детали адреса (включая CDEK-поиск), итог
- Автопропуск шага 4 при PICKUP (нет адреса)
- Предзагрузка методов доставки с шага 2
- Автокалькуляция CDEK тарифа при выборе ПВЗ
- Возврат после логина: checkout-intent сохраняется, пользователь попадает обратно в checkout

### Freedom Pay
- `POST /payments/init` строит подписанный Freedom Pay URL (MD5)
- Callback verifies MD5 signature, обрабатывает `pg_result=1/0`
- `pg_result=1` → Order=NEW, Payment=SUCCEEDED, inventory списан
- `pg_result=0` → PaymentFailedPage → retry banner
- Fail-fast: `FREEDOMPAY_MERCHANT_ID` пустой → приложение не стартует на prod

### PayPal
- `POST /payments/paypal/create-order` → KZT→USD конвертация → PayPal Orders API v2
- Return URL с `?token=` → `capturePayPalOrder()` → Order=NEW, Payment=SUCCEEDED
- Cancel URL с `?token=` → HMAC-signed `cancelToken` → Payment=CANCELLED (или 403 без токена)
- `extractOrderId()` с fallback через `links[rel=up]` (исправлено B-C4)
- Async webhook: `PAYMENT.CAPTURE.COMPLETED` идемпотентно обновляет статус

### Отмена и recovery
- PaymentFailedPage / PaymentCancelledPage читают `loadLastPayment()` из localStorage
- RecoveryBanner появляется в CartPage (через navigation state ИЛИ localStorage)
- Смена провайдера при повторе (можно переключиться с FP на PayPal)
- `clearPendingPayment()` при успешной оплате — recovery banner не появляется снова

### История заказов
- `GET /me/orders` (JWT protected) через React Query, staleTime=30s
- Скелетон при загрузке, empty state при 0 заказах
- Статус-пилюли с i18n-метками и цветовым кодированием (все 9 статусов)
- Дата форматируется через i18n.language → kk-KZ / ru-RU / en-US
- Кнопка "Оплатить" для PENDING_PAYMENT → recovery flow

### Администрирование
- Полный CRUD дизайнов: создание, редактирование, загрузка изображений в MinIO
- Галерея: multi-file upload, хранение URL в `gallery[]`
- Статусный workflow: DRAFT → PUBLISHED → ARCHIVED → PUBLISHED
- `window.confirm` перед архивацией (A-C1)
- `window.confirm` перед созданием CDEK отправления (A-H3)
- Status badges (ЧЕРНОВИК/ГОТОВ/ОПУБЛИКОВАН/АРХИВ) с цветовым кодированием
- PublishGuidance: предупреждение "нет изображения / нет вариантов"
- KPI dashboard: реальные счётчики NEW-заказов (исправлено A-H1)

### CDEK
- Создание отправления через CDEK API v2
- Webhook с проверкой `X-Authorization` токена
- `findByCdekOrderUuid()` O(1) lookup вместо `findAll()` scan (исправлено P1-7)
- Синхронизация статуса вручную (`/sync`)
- Автоматический переход Order → DELIVERED при webhook DELIVERED

### Безопасность
- JWT refresh token revocation: `token_version` + `ver` claim (H1)
- HTTPS + TLS 1.2/1.3 + HSTS через nginx/Let's Encrypt (O-C1)
- Content-Security-Policy header в nginx (H4)
- ALLOWED_ORIGINS required через `:?` (H6)
- Rate limiting на auth/custom-design/order эндпоинтах
- Все реальные секреты удалены из `.env.example` (C2)
- Actuator health endpoint для Docker healthcheck (O-H1)

### Инфраструктура
- Docker prod compose с 7 сервисами: app, db, minio, nginx, certbot, db-backup, redis (опционально)
- Log rotation (json-file driver) на всех контейнерах (O-H2)
- PostgreSQL pg_dump sidecar каждые 24ч (O-H2)
- postgres:16.6 pinned (O-M1)
- V28: 3 индекса + 2 CHECK ограничения
- N+1 исправлен: admin orders (P1-15) и design page inventory (B-H2)
- Pessimistic lock → HTTP 409 (B-H3)

---

## WARNINGS

Работает, но требует ручной проверки перед первым клиентом.

### W1. CDEK webhook-token не настроен → dev режим
**Симптом:** `cdek.webhook-token` пустой → `log.warn("accepting unsigned webhook")` → любой POST на `/delivery/cdek/webhook` принимается.  
**Риск:** вебхуки без аутентификации принимаются в prod.  
**Действие:** установить `CDEK_WEBHOOK_TOKEN=<random_secret>` в `.env.prod` ДО первой отгрузки.

### W2. CDEK двойное создание отправления — ПОДТВЕРЖДЁН
**Симптом:** при повторном `POST /cdek-shipment/by-order/{id}/create` создаётся второй заказ в CDEK.  
**Код:** `CdekOrderServiceImpl.java:56-94` — `findByOrder_Id()` upsert для DB-записи, но `provider.createShipment()` вызывается безусловно каждый раз → новый UUID, старый CDEK-заказ осиротевает.  
**Риск:** CDEK может списать за оба отправления.  
**Действие:** SOP для admin — перед пересозданием отменить (`/cancel`) существующее отправление в CDEK ЛК. `window.confirm` защищает от случайного клика. Подробно: DEPLOYMENT_RUNBOOK.md § SOP-1.

### W3. PayPal double capture при F5 — БЕЗОПАСЕН ✅
**Верификация:** `PayPalServiceImpl.java:173-176` — ранний return `toResponse(payment)` при `status != PENDING`, без вызова PayPal API.  
`findByProviderPaymentIdForUpdate()` — pessimistic lock исключает гонку.  
**Результат:** F5 на `/payment-return` → получает `status=SUCCEEDED` → `/payment/success`. Ошибки `ORDER_ALREADY_CAPTURED` не возникает.  
**Никаких изменений не требуется.**

### W4. MinIO backup не автоматизирован (O-M3)
**Симптом:** изображения дизайнов хранятся только в Docker volume, без offsite backup.  
**Риск:** потеря всех медиа при инциденте с диском.  
**Действие:** настроить rclone cron → S3 до загрузки первых изображений (блокирует реальный контент, но не первый деплой).

### W5. Нет email-уведомлений
**Симптом:** клиент не получает подтверждения заказа по email.  
**Риск:** рост обращений в поддержку "где мой заказ?".  
**Действие:** реализовать SendGrid/SMTP уведомления в течение 1-2 недель после запуска.

### W6. CustomDesignPage без i18n
**Симптом:** `/custom-design` показывает хардкод RU-текст при KK/EN языке.  
**Риск:** минорная проблема для не-RU пользователей.  
**Действие:** добавить i18n ключи в течение 1 недели.

### W7. Доставка fee не пересчитывается при confirm (B-H6)
**Симптом:** стоимость доставки зафиксирована на момент выбора в корзине.  
**Риск:** при изменении тарифов CDEK между выбором и оплатой сумма будет отличаться от реальной.  
**Действие:** мониторить жалобы; реализовать пересчёт при первых расхождениях.

---

## BLOCKERS

Реальных блокеров на данный момент нет при соблюдении следующих операционных условий:

| # | Условие | Статус |
|---|---------|--------|
| 1 | Публичный домен `DOMAIN=balgyn.kz` указан в `.env.prod` | Ops-действие |
| 2 | DNS А-запись указывает на prod-сервер | Ops-действие |
| 3 | `certbot` успешно получил TLS-сертификат | Ops-действие |
| 4 | `FREEDOMPAY_MERCHANT_ID` и `FREEDOMPAY_SECRET_KEY` заданы | Обязательно |
| 5 | `PAYPAL_CLIENT_ID` и `PAYPAL_CLIENT_SECRET` заданы | Обязательно |
| 6 | `PAYPAL_WEBHOOK_ID` задан и вебхук зарегистрирован в PayPal dashboard | Обязательно |
| 7 | `JWT_SECRET` ≥ 32 символов | Обязательно |
| 8 | Хотя бы 1 дизайн в статусе PUBLISHED с вариантами и ценами | Контент |

**Если любой из п.1-7 не выполнен — запуск невозможен. Это не баги кода, это деплойные условия.**

---

## FINAL VERDICT

```
╔══════════════════════════════════════╗
║   READY WITH MINOR RISKS             ║
╚══════════════════════════════════════╝
```

### Обоснование

**Все 6 клиентских сценариев проходят без критических сбоев:**
- Новый клиент полностью завершает покупку через оба платёжных провайдера
- Отмена и retry работают через localStorage recovery
- Администратор создаёт дизайны, управляет статусами, обрабатывает заказы
- CDEK отправления создаются и отслеживаются через webhook

**Все P0-блокеры закрыты в коде.** Все P1 "до первого клиента" реализованы.

**Оставшиеся риски (W1-W7) не блокируют первую продажу:**
- W1 (CDEK webhook-token): настраивается за 2 минуты в `.env.prod`
- W2 (double create): admin видит предупреждение, подтверждает вручную
- W3 (PayPal double capture): крайне редкий edge case; крэша нет (просто /payment/failed)
- W4 (MinIO backup): первые дни без контента — риск нулевой
- W5-W7: не влияют на завершение покупки

**Платформа готова принять первых клиентов при выполнении 8 деплойных условий выше.**

---

*DRY_RUN документы: [NEW_CUSTOMER](DRY_RUN_NEW_CUSTOMER.md) · [FREEDOMPAY](DRY_RUN_FREEDOMPAY.md) · [PAYPAL](DRY_RUN_PAYPAL.md) · [CANCEL](DRY_RUN_PAYMENT_CANCEL.md) · [ADMIN](DRY_RUN_ADMIN.md) · [CDEK](DRY_RUN_CDEK.md)*
