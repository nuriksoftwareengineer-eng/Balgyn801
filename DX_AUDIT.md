# BALGYN — Developer Experience (DX) Audit

Дата: 2026-06-22
Вопрос: «Сможет ли новый разработчик после `git clone` на чистой машине запустить проект и
протестировать основной функционал без ручных правок?»
Метод: проверка по факту (живой стек, реальные HTTP-запросы, БД, содержимое jar), а не по докам.

> **Найден ровно 1 блокер — он ИСПРАВЛЕН** в этом аудите (`frontend/nginx.conf`). После коммита
> этого файла проект собирается и работает «из коробки».

---

## Ключевой факт: нужен ли `.env`?
`.env` и `frontend/.env` — в `.gitignore` (правило `.env` действует и на подпапки), поэтому
**после clone их НЕТ**. Это нормально: весь backend конфигурируется дефолтами в
`docker-compose.yml` через `${VAR:-default}`. **`.env` для запуска НЕ нужен.**

---

## Чек-лист (17 пунктов)

| # | Пункт | Статус | Деталь (проверено по факту) |
|---|-------|--------|------------------------------|
| 1 | git clone | ✅ | `package-lock.json` (3307 строк) и `gradle-wrapper.jar` не в .gitignore → шипятся. `.env` исключён, но не нужен. |
| 2 | docker compose up --build | ✅ | backend (multi-stage gradle), frontend (vite→nginx), db, minio — собираются. |
| 3 | создание БД | ✅ | сервис `db` (postgres:16), БД `balgynbol-spring`. |
| 4 | Flyway migrations | ✅ | V1–V30 применяются на чистой БД (схема доходит до 30). |
| 5 | seed данных | ✅ | `APP_SEED_CATALOG=true` (дефолт). Факт: 5 групп / 14 коллекций / 16 дизайнов / 45 вариантов. |
| 6 | admin пользователь | ✅ | `BOOTSTRAP_ADMIN_EMAIL/PASSWORD` дефолты. Факт: `admin@balgyn.local` создан. |
| 7 | frontend доступен | ✅ | `http://localhost:5174` → HTTP 200. |
| 8 | backend доступен | ✅ | `http://localhost:8080` → HTTP 200. |
| 9 | login | ✅ | `POST /api/v1/auth/login` (admin@balgyn.local / admin12345) → 200 + ADMIN-токен. |
| 10 | каталог | ✅ | `GET /api/v1/catalog/groups` → 200. |
| 11 | корзина | ✅ | клиентская (localStorage), бэкенд не нужен. |
| 12 | checkout | ✅ | `POST /api/v1/order` — permitAll (гость), доходит до контроллера. |
| 13 | PayPal sandbox | ✅* | реальные валидные sandbox-ключи вшиты в `docker-compose.yml`; create-order работает. *Завершить оплату — нужен sandbox-аккаунт ПОКУПАТЕЛЯ PayPal. |
| 14 | FreedomPay | ✅ stub | дефолт `FREEDOMPAY_MERCHANT_ID=` (пусто) → stub-режим, авто-подтверждение через `/payments/stub/freedom-pay/approve`. |
| 15 | CDEK | ✅ stub | дефолт `CDEK_CLIENT_ID/SECRET=` (пусто) → mock: тестовые города + stub-тариф. |
| 16 | обязательные ENV | ✅ | все имеют дефолты в compose (JWT_SECRET, admin, datasource, MinIO, провайдеры). Без `.env` стартует. |
| 17 | где упадёт после clone | ⚠️→✅ | **Был 1 блокер: frontend не достигал backend (см. ниже). ИСПРАВЛЕН.** |

\* PayPal: инициация платежа работает из коробки; полный happy-path требует тестового
покупателя PayPal Sandbox (стандартно для любой PayPal-песочницы). Для теста checkout без
PayPal — есть FreedomPay stub.

---

## Единственный блокер (НАЙДЕН и ИСПРАВЛЕН)

**❌→✅ Frontend не мог достучаться до backend после чистого клона.**

Причина (по факту):
- Без `.env` фронт собирается с `VITE_API_BASE_URL=/api/v1` (дефолт build-arg в compose) —
  это **относительный** путь.
- `frontend/nginx.conf` **не имел** проксирования `/api/` (хотя комментарий в `frontend/Dockerfile`
  обещает «nginx can proxy to the backend»).
- Итог: браузер дёргает `http://localhost:5174/api/v1/...` → nginx отдаёт SPA-фоллбэк
  (`index.html`) вместо API → login/каталог/checkout не работают.
- (На машине автора это маскировалось личным `.env` с `VITE_API_BASE_URL=http://localhost:8080/api/v1`,
  который не шипится при clone.)

Исправление: добавлен прокси в [`frontend/nginx.conf`](frontend/nginx.conf):
```nginx
location /api/ {
    proxy_pass http://app:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_read_timeout 60s;
}
```
Проверено (фронт пересобран с `/api/v1`, как при чистом клоне):
`GET /api/v1/exchange-rates` → 200, `GET /api/v1/catalog/groups` → 200,
`POST /api/v1/auth/login` → 200, `GET /api/v1/delivery/methods?countryIso2=KZ` → 200, SPA `/` → 200.

---

## Ответы на 7 вопросов

1. **Нужен ли `.env` после clone?** — **НЕТ.** Все дефолты в `docker-compose.yml`. Backend
   стартует и core-flow работает без `.env`.
2. **Есть ли `YOUR_*` placeholders?** — **НЕТ** настоящих. Единственные вхождения `YOUR_` — это
   код-детектор `value.startsWith("YOUR_")` в `PayPalProperties`/`FreedomPayProperties`.
3. **Секреты, без которых не стартует?** — **НЕТ.** `JWT_SECRET`, admin, БД, MinIO — все с dev-дефолтами
   в compose. (Вне Docker, через `gradle bootRun` без `JWT_SECRET`, приложение не стартует — но
   вопрос про `docker compose`, где дефолт есть.)
4. **Checkout без аккаунта PayPal?** — **ДА.** FreedomPay stub (дефолт) авто-подтверждает заказ.
5. **Доставка без аккаунта CDEK?** — **ДА.** CDEK stub: тестовые города + расчёт тарифа-заглушки.
6. **Оплата без аккаунта FreedomPay?** — **ДА.** FreedomPay stub-режим (пустой merchant).
7. **Что НЕ заработает после чистого клона?**
   - **Реальные города/тарифы CDEK** — только stub (нужны ключи; их намеренно НЕ коммитят — это
     прод-ключи). Stub работает.
   - **Реальные платежи FreedomPay** — только stub (нужны ключи). Stub работает.
   - **Полное завершение оплаты PayPal** — нужен тестовый покупатель PayPal Sandbox (инициация работает).
   - Прочее (login/каталог/корзина/checkout/доставка-stub) — работает.

---

## Прочие наблюдения (не блокеры)

- **MinIO бакет** создаётся автоматически: `MinioBucketInitializer` (`@ConditionalOnProperty …
  matchIfMissing=true`) → загрузка картинок в админке работает из коробки.
- **`frontend/.env` (локально у автора)** указывает на порт **8081** (бэкенд — 8080). Файл
  gitignored → не шипится, но для локального `npm run dev` у автора путь неверный. Рекомендация:
  удалить файл или исправить на `8080` (или вообще убрать — относительный `/api/v1` + dev-proxy надёжнее).
- **Личный `.env` с `VITE_API_BASE_URL=http://localhost:8080/api/v1`** теперь не обязателен —
  с добавленным nginx-прокси относительный `/api/v1` работает сам.
- **Порты** 8080/5174/5432/9000/9001 должны быть свободны на хосте (иначе `compose up` упадёт) —
  средовое ограничение, не код.

---

## ВЕРДИКТ

# ✅ READY FOR NEW DEVELOPERS
*(при условии коммита исправленного `frontend/nginx.conf`)*

Один блокер найден и устранён. После него: `git clone` → `docker compose up --build` →
полностью рабочее приложение (БД, миграции, seed, admin, login, каталог, корзина, checkout,
доставка-stub, оплата FreedomPay-stub, PayPal sandbox) **без единой ручной правки и без `.env`**.

### Оставшиеся (НЕ блокирующие) задачи
1. Закоммитить `frontend/nginx.conf` (исправление).
2. (Опц.) Удалить/починить локальный `frontend/.env` (порт 8081).
3. (Опц.) Документировать, что реальные CDEK/FreedomPay требуют своих ключей в `.env` (по умолчанию — stub).
