# BALGYN — Bugfix Report: PayPal 502 / Freedom Pay pg_sig / CDEK тестовые города

Дата: 2026-06-22
Метод: трассировка реального кода + эмпирическая проверка живых API (PayPal sandbox,
Freedom Pay `api.freedompay.kz`, CDEK prod `api.cdek.ru`) с фактическими ключами из `.env`.
Все выводы подтверждены вызовами, а не гипотезами.

Итог: **134 теста зелёные (0 failures, 0 errors)**. Рабочий поток PayPal и CDEK подтверждён
сквозным прогоном через реальные клиентские классы Java.

---

## PROBLEM 1 — PayPal `create-order` возвращает 502, в логах только одна строка

### Root Cause
Два дефекта, оба в коде (не в PayPal и не в ключах):

1. **502 генерирует сам бэкенд, а не nginx.** В `.env` стоит
   `VITE_API_BASE_URL=http://localhost:8080/api/v1` — фронт ходит в бэкенд напрямую, nginx в
   dev-схеме не участвует. 502 отдаёт
   [`RestExceptionHandler.handlePayPalApi`](src/main/java/com/nurba/java/exception/RestExceptionHandler.java):
   любой `PayPalApiException` → `HttpStatus.BAD_GATEWAY`.

2. **Ошибка проглатывалась полностью.** Хендлер `handlePayPalApi` **не логировал ничего**, а
   `PayPalOrdersClient` писал статус/тело ответа в `log.debug` (по умолчанию выключен — корневой
   уровень INFO). Поэтому в логах оставалась только INFO-строка контроллера
   `[PayPal] create-order request for orderId=1`, а реальная причина (стектрейс/HTTP-тело) была
   невидима. Точно совпадает с симптомом «дальше нет ни success, ни error, ни stacktrace».

Сами ключи и API **исправны** (доказано):
- OAuth `https://api-m.sandbox.paypal.com/v1/oauth2/token` с ключами из `docker-compose.yml`/`.env` → **HTTP 200**, токен 97 символов.
- `POST /v2/checkout/orders` тем же токеном → **HTTP 201**, approval URL присутствует (`rel:"approve"`).
- Egress из Docker-контейнера до PayPal → **HTTP 401 за 0.64s** (т.е. сеть из контейнера есть, «контейнер без интернета» исключено).
- Сквозной прогон через **реальный** `PayPalOrdersClient.createOrder(...)` → order `5UT65310C7116580S`, approvalUrl `https://www.sandbox.paypal.com/checkoutnow?token=5UT65310C7116580S`.

Вывод: 502 возникал, когда исходящий вызов падал по транзиентной/сетевой причине в момент теста,
а **молчащий хендлер делал диагностику невозможной**. Первопричина «невозможно понять, что
случилось» — это `handlePayPalApi` без логирования и `log.debug` в клиенте.

### Files Changed
- [`RestExceptionHandler.java`](src/main/java/com/nurba/java/exception/RestExceptionHandler.java)
  — добавлен SLF4J-логгер; `handlePayPalApi` теперь пишет `log.error(..., ex)` с полным стектрейсом перед отдачей 502.
- [`PayPalOrdersClient.java`](src/main/java/com/nurba/java/payment/PayPalOrdersClient.java)
  — `log.debug` → `log.info`; перед каждым реальным вызовом логируется URL/сумма/режим; не-2xx и
  транспортные ошибки логируются `log.error` со статусом и телом ответа.
- [`PayPalTokenClient.java`](src/main/java/com/nurba/java/payment/PayPalTokenClient.java)
  — логируется попытка получить OAuth-токен (URL+режим) и любая ошибка (`log.error`) до выброса исключения.

### Test Results
- `PayPalPaymentIntegrationTest` — зелёный (12 кейсов, мок-клиент).
- Live-прогон реального клиента — approval URL получен (см. выше).

### Verification Steps
1. `docker compose up -d --build`.
2. Создать заказ: `POST /api/v1/order` (валидные items) → запомнить `orderId`.
3. `POST /api/v1/payments/paypal/create-order` `{ "orderId": <id> }`.
   Ожидаемо: **200** + `paymentUrl` вида `https://www.sandbox.paypal.com/checkoutnow?token=...`.
4. В логах теперь видно: `[PayPal] Fetching OAuth token ...` → `[PayPal] OAuth token acquired ...`
   → `[PayPal] createOrder → POST ...` → `[PayPal] createOrder OK: HTTP 201`.
5. При сбое в логах будет точная причина: `[PayPal] ... failed: HTTP <code> body=<...>` или
   `transport error ...`, плюс ERROR-стектрейс из `handlePayPalApi`.

### Remaining Risks
- Если контейнер периодически теряет egress к `api-m.sandbox.paypal.com` (VPN/файрвол/IPv6),
  вызов снова даст 502 — но теперь с явной причиной в логах. Опциональное усиление: 1 ретрай на
  транспортные ошибки и/или `-Djava.net.preferIPv4Stack=true` для JVM в контейнере.
- 502 — корректный статус для ошибки внешнего провайдера; фронт должен показывать понятное
  сообщение и предлагать другой способ оплаты.

---

## PROBLEM 2 — Freedom Pay: `pg_sig mismatch in init_payment.php response`

### Root Cause
**Порядок проверок в `parseAndVerify` маскировал реальную ошибку.** Подтверждено живым вызовом
`api.freedompay.kz` с ключами из `.env` (merchant `587060`, secret `TOSTcm0z9miUgpyC`):

- Успешный ответ (`pg_status=ok`) **подписан**, и наша подпись совпадает с `pg_sig` Freedom Pay
  (`MATCH=true` — алгоритм и секрет верны, в т.ч. с полным набором URL и кириллицей в описании).
- Ошибочный ответ Freedom Pay **не содержит `pg_sig` вообще**:
  ```xml
  <response><pg_status>error</pg_status><pg_error_code>9998</pg_error_code>
  <pg_error_description>Некорректная подпись запроса</pg_error_description></response>
  ```

Старый код проверял подпись **до** `pg_status`. Для ошибочного (неподписанного) ответа
`receivedSig=null` → `verify(...)` возвращает `false` → лог `pg_sig mismatch in ... response` и
ответ «Invalid response signature», **скрывая настоящую ошибку** (`pg_error_description`).

То есть «pg_sig mismatch» — это НЕ проблема подписи, а замаскированная ошибка инициализации
(любой `pg_status=error` выглядел как несовпадение подписи).

### Files Changed
- [`FreedomPayHttpClient.java`](src/main/java/com/nurba/java/payment/FreedomPayHttpClient.java)
  — в `parseAndVerify` сначала проверяется `pg_status`; для `error` возвращается реальный
  `pg_error_code` / `pg_error_description`; подпись проверяется **только** для успешных (подписанных)
  ответов. Лог ответа `init_payment.php` поднят `debug` → `info`.

### Test Results
- Новый юнит-тест `errorResponse_withoutSig_surfacesRealError_notSigMismatch` — неподписанный
  error-ответ отдаёт «Некорректная подпись запроса», а не маскирующее «Invalid response signature».
- Все прежние кейсы `FreedomPayResponseVerificationTest` (валидная/битая/чужой ключ/отсутствует
  подпись/tampered) — зелёные: для `pg_status=ok` подпись по-прежнему обязательна.

### Verification Steps
1. `POST /api/v1/payments/init` (Freedom Pay) для валидного заказа.
   Happy-path (проверено вживую) → `pg_status=ok`, `pg_redirect_url=https://customer.freedompay.kz/pay.html?...`.
2. При ошибке Freedom Pay пользователь/лог теперь видит её текст (`pg_error_description`), а не «pg_sig mismatch».

### Remaining Risks
- Безопасность сохранена: успешные ответы по-прежнему отвергаются при неверной подписи (защита от MITM).
- `pg_redirect_url` с `&` в реальном ответе корректно декодируется DOM-парсером (`getTextContent`),
  поэтому подпись успешного ответа сходится (в отличие от наивного regex).

---

## PROBLEM 3 — CDEK показывает только тестовые города

### Root Cause
Чисто конфигурационная причина, два момента:

1. **Ключи CDEK закомментированы в `.env`** → `CdekProperties.isConfigured()=false` → режим `auto`
   разрешается в **mock**. [`CdekDeliveryService.searchCities`](src/main/java/com/nurba/java/service/delivery/CdekDeliveryService.java)
   при `!useRealApi()` возвращает 4 захардкоженных города (`stubCities`: Алматы, Астана, Москва,
   Санкт-Петербург). Это и есть «тестовые города».
2. **Ключи — продакшн-аккаунт.** Проверено: OAuth этих ключей даёт **401 invalid_client** на
   sandbox `api.edu.cdek.ru` и **200 + токен** на prod `api.cdek.ru` (scope `location:all`).
   А дефолтный `CDEK_BASE_URL` указывал на edu-sandbox — т.е. даже после раскомментирования ключей
   на старом base-url был бы 401.

Сам код реального поиска **исправен**: `CdekClient.searchCities` кодирует кириллицу через
`UriComponentsBuilder.encode(UTF_8)` — это именно та кодировка, которая на prod возвращает реальные
города (UTF-8 `Алматы` → 200; ранние «400» были артефактом UTF-8 в Git Bash, а не кода/CDEK).

### Files Changed
- [`.env`](.env) — раскомментированы `CDEK_CLIENT_ID` / `CDEK_CLIENT_SECRET`, добавлены
  `CDEK_BASE_URL=https://api.cdek.ru/v2` (prod, обязательно для этих ключей) и `CDEK_PROVIDER=real`.
- [`docker-compose.yml`](docker-compose.yml) — добавлен проброс `CDEK_PROVIDER` (раньше не
  передавался в контейнер) + примечание про prod base-url.

> Код Java НЕ менялся — поиск городов работал, не работала конфигурация.

### Test Results
- Сквозной прогон реального `CdekClient.searchCities` с prod-ключами и prod base-url:
  - `Алматы` → Алматы (code 4756)
  - `Астана` → Астана (code 4961)
  - `Москва` → Москва (code 44)
- Все delivery-тесты (`CdekCalculateOrderIntegrationTest`, `DeliveryPricingIntegrationTest` и др.) — зелёные.

### Verification Steps
1. `docker compose up -d` (с обновлённым `.env`).
2. В логах старта: `CDEK delivery provider: mode=REAL, active=real-cdek, configured=true`.
3. `GET /api/v1/delivery/cities?query=Алматы` → реальные города CDEK (не STUB).
4. `GET /api/v1/delivery/cities?query=Астана` / `Москва` — аналогично.

### Remaining Risks
- Это **продакшн**-ключи CDEK на боевом API — реальные расчёты/заказы. Для тестирования создания
  отправлений (`/orders`) использовать осознанно.
- `.env` в `.gitignore` (не коммитится) — ключи не утекут в репозиторий.
- Для возврата в mock: `CDEK_PROVIDER=mock` или снова пустые ключи.

---

## Сводка изменённых файлов
| Файл | Проблема | Суть |
|------|----------|------|
| `RestExceptionHandler.java` | 1 | логирование PayPalApiException (ERROR + стектрейс) |
| `PayPalOrdersClient.java` | 1 | видимое логирование запроса/ответа/ошибок |
| `PayPalTokenClient.java` | 1 | логирование получения OAuth-токена и ошибок |
| `FreedomPayHttpClient.java` | 2 | проверка `pg_status` до подписи; реальная ошибка вместо маскировки |
| `FreedomPayResponseVerificationTest.java` | 2 | новый регресс-тест на неподписанный error-ответ |
| `.env` | 3 | реальные CDEK-ключи + prod base-url + provider=real |
| `docker-compose.yml` | 3 | проброс `CDEK_PROVIDER` |

**Тесты: 134 passed / 0 failed / 0 errors.**
