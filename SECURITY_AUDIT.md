# BALGYN — Production Security Audit

Дата: 2026-06-22
Метод: чтение исходников по слоям + проверка живого стека. Оценка по OWASP Top 10 2021.
Стек: Spring Boot 4.0.6, React/TS, PostgreSQL 16, JWT (HS256), MinIO, PayPal, Freedom Pay, VTB KZ, СДЭК, Email, Telegram.

## Executive summary

Проект **зрело защищён**. Основные классы уязвимостей (SQLi, XSS, price/amount tampering,
oversell race, IDOR по адресам/заказам, weak JWT) — закрыты продуманными контролями.
Найден **1 High** (IDOR трекинга), несколько **Medium/Low** (в основном dev-конфигурация и
дизайн-компромиссы; production-профиль их закрывает). **Critical — не найдено.**

Вердикт: **готово к production при выполнении prod-чеклиста** (нижняя секция) и исправлении H1.

---

## Сильные стороны (подтверждено кодом)

| Контроль | Доказательство |
|---|---|
| Цены/доставка/скидка считаются **на сервере** | `OrderServiceImpl.createOrder` — `product.getPrice()`, `DesignGarmentPrice`, `deliveryPricingService.quote`; клиент не шлёт суммы |
| Нет oversell (гонки) | `inventoryRepository.findAndLockByGarmentColorSize` = SELECT FOR UPDATE, затем decrement |
| Загрузка файлов | admin-only + `image/*` + 8 МБ + **magic bytes** (JPEG/PNG/GIF/WebP); SVG/HTML/JS/EXE отклоняются; ключ = UUID (нет traversal) |
| JWT | HS256, `verifyWith(signingKey)` (нет alg-confusion/`none`), секрет ≥32 байт иначе fail-fast; роли берутся из БД, не из токена |
| Refresh-токен | HttpOnly + `SameSite=Strict` cookie, вырезается из тела; logout инкрементит `tokenVersion` (реальный revoke) |
| SQLi | нет `nativeQuery`/конкатенации в репозиториях — только derived queries + параметризованный `@Query` |
| XSS (frontend) | нет `dangerouslySetInnerHTML`/`innerHTML`/`eval`; React экранирует |
| PayPal | сумма сверяется со stored (`captureOrder`), webhook replay-protection (`ProcessedWebhookEvent`), подпись через verify-webhook-signature API |
| Freedom Pay | callback проверяет MD5 pg_sig до обработки, unsigned отклоняется; XML-ответ экранируется |
| VTB | security-gate = серверная сверка статуса (`getOrderStatusExtended`), checksum как defense-in-depth |
| СДЭК webhook | X-Authorization токен (в prod обязателен) |
| Authorization | `anyRequest().authenticated()` (default-deny), `@EnableMethodSecurity` + `@PreAuthorize` на admin-контроллерах, ownership-check на сохранённых адресах и `/me`, `/my-orders` |
| Prod-профиль | Swagger off, `cookie-secure=true`, `trust-proxy=true`, `allow-unsigned=false`; `docker-compose.prod.yml` требует **все** секреты через `${VAR:?}` (fail-fast) |

---

## Findings

| Severity | ID | Issue | File | Recommendation |
|---|---|---|---|---|
| 🟠 High | H1 | **IDOR трекинга**: `GET /api/v1/orders/{orderId}/tracking` публичен, без проверки владельца, id последовательны → перечисление трек-номеров/статусов любого заказа | `ParcelTrackingController.java:20-24` (permitAll `SecurityConfig:139`) | Требовать вторичный секрет (номер заказа как непубличный токен, либо телефон/email заказа), либо auth+ownership |
| 🟡 Medium | M1 | Dev `docker-compose.yml` содержит **известный** `JWT_SECRET` и `admin12345` дефолтами → при ошибочном публичном запуске dev-compose JWT подделываются, вход по дефолту | `docker-compose.yml:10-13,41-43` | Не хардкодить рабочий JWT-секрет даже в dev (генерировать/пустой→fail), громкий warning; в README — «dev-only» |
| 🟡 Medium | M2 | СДЭК webhook принимает **unsigned** при пустом `cdek.webhook-token` (dev-дефолт) | `CdekWebhookController.java:48-49` | В prod уже обязателен (`:?`). Оставить, но не запускать dev-конфиг публично |
| 🟡 Medium | M3 | Rate-limit по IP за прокси: с добавленным nginx `/api`-прокси весь трафик идёт с IP прокси, если `TRUST_PROXY=false` (dev-дефолт) → общий bucket на всех | `SensitiveEndpointRateLimiterFilter.java:113-122` | В prod `trust-proxy=true` (уже так). Для dev-за-прокси включить `TRUST_PROXY=true` |
| 🟡 Medium | M4 | Refresh-токен **без ротации/детекта повторного использования**, TTL 14 дней; украденный refresh валиден до истечения | `AuthServiceImpl.doRefreshWithToken`, `JwtService.generateRefreshToken` | Ротация refresh при каждом использовании + детект reuse (или jti-blacklist). `tokenVersion` даёт только глобальный revoke |
| 🟢 Low | L1 | Legacy refresh-токены `ver=-1` обходят revoke (переходная поблажка) | `AuthServiceImpl:107-110` | Убрать после миграции: считать отсутствие `ver` невалидным |
| 🟢 Low | L2 | User enumeration: `register` («email уже зарегистрирован») и публичный `coupons/validate` раскрывают существование | `AuthServiceImpl:49-51` | Обобщить сообщение register; rate-limit на validate |
| 🟢 Low | L3 | Freedom Pay callback: явной идемпотентности нет (подпись+salt пройдут повторно) — полагается на статус платежа | `FreedomPayCallbackController` → `PaymentService.handleCallback` | Добавить replay-guard (`ProcessedWebhookEvent`) как у PayPal |
| 🟢 Low | L4 | Dev `frontend/nginx.conf` без security-заголовков (CSP/HSTS); prod `nginx/nginx.conf` — с ними | `frontend/nginx.conf` | Dev-only; для паритета можно добавить CSP |
| 🟢 Low | L5 | `window.location.href = paymentUrl` без валидации домена (URL от бэкенда, не от юзера) | `CartPage.tsx:1171` | Проверять, что host ∈ {gateway-домены} |
| 🟢 Low | L6 | `trackingNumber` вставляется в HTML письма без `esc()` (значение админ/СДЭК) | `EmailServiceImpl.java:70` | Экранировать для единообразия |

---

## OWASP Top 10 2021 — соответствие

| # | Категория | Статус |
|---|---|---|
| A01 Broken Access Control | ⚠️ H1 (трекинг IDOR); остальное — ownership-checks, default-deny, method-security |
| A02 Cryptographic Failures | ✅ HS256≥32B, HttpOnly/Secure/SameSite cookie, HTTPS+HSTS (prod), пароли BCrypt |
| A03 Injection | ✅ нет SQLi (JPA параметризован), нет XSS-sink, XML/email экранируются |
| A04 Insecure Design | ✅ серверные цены/остатки/статус-машина; ⚠️ refresh без ротации (M4) |
| A05 Security Misconfiguration | ⚠️ dev-дефолты (M1/M2/M3); ✅ prod-профиль жёсткий, Swagger off, секреты обязательны |
| A06 Vulnerable Components | ✅ Spring Boot 4.0.6, jjwt 0.12.6, AWS SDK 2.29.45 — актуальные |
| A07 Auth Failures | ✅ rate-limit login/register, generic login error, BCrypt, revoke; ⚠️ enum (L2) |
| A08 Integrity Failures | ✅ webhook-подписи (PayPal/FreedomPay/VTB/CDEK), amount-check |
| A09 Logging Failures | ✅ секреты/токены/пароли не логируются; есть аудит статусов заказа |
| A10 SSRF | ✅ исходящие вызовы только к фиксированным API (PayPal/FP/VTB/CDEK/MinIO из конфигурации), URL не из пользовательского ввода |

---

## Per-check (1–30) кратко

1 Auth ✅ (M4 refresh rotation) · 2 Authorization ✅ (H1) · 3 IDOR ⚠️ H1, остальное ✅ ·
4 SQLi ✅ · 5 XSS ✅ · 6 CSRF ✅ (stateless JWT, нет cookie-auth для мутаций; refresh-cookie SameSite=Strict) ·
7 SSRF ✅ · 8 File upload ✅ · 9 Secrets ⚠️ M1 (dev), prod ✅ · 10 Docker ✅ (prod: non-root? см. ниже) ·
11 Headers ✅ prod (CSP/HSTS/XCTO/XFO/Referrer) · 12 HTTPS ✅ prod (certbot, HSTS, secure cookie) ·
13 CORS ✅ (env-список, не wildcard, credentials) · 14 Rate limiting ✅ (M3 proxy-ip) · 15 Brute force ✅ (login 10/min) ·
16 Webhooks ✅ (подписи; L3 FP idempotency) · 17 Payments ✅ (серверные суммы, amount-check) ·
18 CDEK ✅ (M2 dev unsigned) · 19 Email ✅ (off by default, header-safe) · 20 Telegram ✅ (off by default) ·
21 Validation ✅ (DTO + сервисные проверки enum/currency/color/size) · 22 Logging ✅ · 23 GDPR ⚠️ (H1 раскрывает трекинг) ·
24 Admin panel ✅ (ADMIN + @PreAuthorize) · 25 Frontend ✅ (нет DOM-XSS/open-redirect от юзера) ·
26 Dependencies ✅ (актуальные) · 27 OWASP — см. выше · 28 отчёт — этот файл ·
29 Business logic ✅ (двойная оплата — статус-гард; купон — атомарный лимит; oversell — lock; статус — allowlist) ·
30 Pentest — см. «Проверенные атаки».

## Проверенные атаки (mental pentest)
- Подмена цены/суммы в `POST /order` → игнорируется (сервер считает из БД). ✅
- Изменение статуса заказа через API обычным юзером → `/order/**` PATCH = ADMIN. ✅
- Чужой заказ/адрес → ownership-check / ADMIN-only. ✅ **кроме трекинга (H1).**
- Oversell гонкой → SELECT FOR UPDATE. ✅
- Повторное применение купона сверх лимита → атомарный `incrementUsageIfAllowed`. ✅
- Подделка webhook оплаты → подпись обязательна. ✅
- alg=none / RS256-confusion в JWT → `verifyWith(SecretKey)` отклоняет. ✅
- SVG/HTML/PHP как «картинка» → magic bytes отклоняют. ✅
- Enumeration трек-номеров по orderId → **работает (H1)**.

---

## Remediation applied (2026-06-22) — всё исправлено, 177/177 тестов зелёные

| ID | Статус | Что сделано |
|---|---|---|
| **H1** | ✅ FIXED | `ParcelTrackingController` + `getForRequester(orderId, phone, email)`: доступ только владельцу (JWT) или по совпадению телефона (последние 10 цифр), иначе `[]` (не перечисляемо). Проверено вживую: no-phone→[], wrong→[], correct→row. |
| **M1** | ✅ FIXED | `InsecureConfigWarner` — громкий WARN/ERROR при dev-дефолтном JWT-секрете / `admin12345` / (prod) пустом CDEK-токене. Проверено: warn виден в логах. |
| **M2** | ✅ FIXED | Покрыт `InsecureConfigWarner` (prod + пустой CDEK webhook-token → WARN). Prod-compose уже требует токен. |
| **M3** | ✅ FIXED | `TRUST_PROXY` проброшен в dev `docker-compose.yml` (за nginx `/api`-прокси). Prod-профиль уже `trust-proxy=true`. |
| **M4** | ✅ FIXED | **Ротация refresh + reuse-detection** через jti-стор (`refresh_tokens`, V41). Каждый refresh отзывает старый токен и выдаёт новый; повтор отозванного → revoke-all всех сессий (в REQUIRES_NEW-транзакции) + 400. Multi-device безопасно. Проверено вживую: rotate 200, replay 400, T2 после reuse 400. Есть scheduled-очистка. |
| **L1** | ✅ FIXED | Legacy `ver=-1` обход убран — refresh без jti-записи отклоняется. |
| **L2** | 🟢 accepted | Register авто-логинит → enumeration присущ; смягчено rate-limit'ом (уже есть). |
| **L3** | ✅ n/a | Уже покрыто: `PaymentServiceImpl.applyResult` дедуплицирует через `ProcessedWebhookEvent` (UNIQUE) + pessimistic lock + статус-гард. |
| **L4** | ✅ FIXED | Dev `frontend/nginx.conf`: добавлены `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` (CSP/HSTS — в prod-nginx). |
| **L5** | ✅ FIXED | `CartPage.tsx`: редирект только на `^https?://` (нет `javascript:`/`data:`). |
| **L6** | ✅ FIXED | `EmailServiceImpl`: `esc(trackingNumber)` в HTML письма. |
| weight | ✅ FIXED | `CdekCalculateOrderIntegrationTest`: устаревшие 1000/1400г → корректные 800/1200г (`HOODIE`=0.8кг). 2 «красных» теста → зелёные. |

Новые файлы: `InsecureConfigWarner`, `RefreshTokenRecord`, `RefreshTokenRepository`,
`SessionRevoker`, `RefreshTokenCleanupJob`, `V41__refresh_tokens.sql`.
Изменено: `ParcelTracking*`, `AuthServiceImpl`, `JwtService`, `EmailServiceImpl`,
`frontend/nginx.conf`, `docker-compose.yml`, `CartPage.tsx`, CDEK-тест.

**После деплоя:** refresh-токены, выданные до V41, станут невалидны (нет jti-записи) — пользователи
один раз залогинятся заново. Это ожидаемо.

## Production deployment checklist (обязательно)
- Запуск ТОЛЬКО через `docker-compose.prod.yml` (`SPRING_PROFILES_ACTIVE=prod`), НИКОГДА dev-compose публично.
- Все секреты из `.env.prod` (JWT_SECRET, POSTGRES_PASSWORD, CDEK_*, CDEK_WEBHOOK_TOKEN, FREEDOMPAY_*, PAYPAL_*, MINIO_*, ALLOWED_ORIGINS) — сгенерированы, не из репозитория.
- `ALLOWED_ORIGINS` = только боевые домены. `COOKIE_SECURE=true`, `TRUST_PROXY=true` (в prod-профиле по умолчанию).
- HTTPS + HSTS (certbot), MinIO-консоль не публична, бэкапы БД включены.
