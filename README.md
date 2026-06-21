# BALGYN

Платформа интернет-магазина вышивки на одежде (Алматы, Казахстан).

Покупатель выбирает дизайн вышивки → тип одежды → цвет → размер → оформляет заказ. Оплата через Freedom Pay или PayPal. Доставка через СДЭК или Казпочту.

---

## Stack

| Слой | Технология | Версия |
|------|-----------|--------|
| Frontend | React + TypeScript + Vite | React 19 · TS 6 · Vite 8 |
| Backend | Spring Boot + Java | Spring Boot 4.0.6 · Java 21 |
| Database | PostgreSQL + Flyway | PostgreSQL 16 · 28 миграций |
| Storage | MinIO (S3-compatible) | — |
| Оплата | Freedom Pay · PayPal | — |
| Доставка | СДЭК API v2 | — |
| i18n | react-i18next | ru · kk · en |
| Инфраструктура | Docker Compose + nginx | — |

---

## Quick Start

### Требования

- [Docker Desktop](https://docs.docker.com/get-docker/) (включает Compose)
- Git

JDK и Node.js **не нужны** — всё собирается внутри Docker.

---

### 1. Clone

```bash
git clone <repo-url> balgyn
cd balgyn
```

---

### 2. Configure

**Linux / macOS:**
```bash
cp .env.example .env
```

**Windows (PowerShell):**
```powershell
copy .env.example .env
```

Файл `.env` работает без правок для локальной разработки.  
`JWT_SECRET` и пароль bootstrap-admin содержат безопасные дефолтные значения для dev-окружения.

> **Хотите случайный JWT_SECRET?** `openssl rand -hex 32`

---

### 3. Run

```bash
docker compose up --build
```

Первый запуск: ~3–5 минут (скачивание образов + сборка JAR и React).  
Последующие запуски: ~30 секунд (кеш Docker).

Порядок старта: **PostgreSQL** → **MinIO** → **Spring Boot** (Flyway + seed) → **Frontend (nginx)**.  
Все сервисы имеют `healthcheck` — Compose ждёт готовности каждого перед следующим.

---

### 4. Open

| Что | URL |
|-----|-----|
| Магазин (Frontend) | http://localhost:5174 |
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MinIO Console | http://localhost:9001 |
| Админ-панель | http://localhost:5174/admin |

---

### Default Admin

| | |
|-|-|
| **Email** | `admin@balgyn.local` |
| **Пароль** | `admin12345` |

Задаётся в `.env` через `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`.  
Создаётся только на пустой базе (при повторном старте — пропускается).

---

## Seed Catalog

При первом старте с `APP_SEED_CATALOG=true` (включено в `.env.example` по умолчанию) автоматически создаётся тестовый каталог:

| | Количество |
|-|-----------|
| Групп (категорий) | 5 |
| Коллекций | 14 |
| Дизайнов (статус PUBLISHED) | 16 |
| Цветов | 6 |
| Размеров | 5 |

**Группы:** Аниме · Игры · Музыка · Спорт · Кино  
**Пример дизайна:** Brand of Sacrifice (Берсерк) — Худи 12 000₸ / Футболка 8 500₸ / Свитшот 10 500₸

Seed безопасно пропускается если каталог уже наполнен.

---

## Docker Services

| Сервис | Порт(ы) | Образ | Назначение |
|--------|---------|-------|-----------|
| `app` | 8080 | `eclipse-temurin:21-jdk` → `amazoncorretto:21` | Spring Boot backend |
| `frontend` | 5174 | `node:22-alpine` → `nginx:1.27-alpine` | React SPA через nginx |
| `db` | 5432 | `postgres:16` | PostgreSQL база данных |
| `minio` | 9000, 9001 | `minio/minio` | Хранилище медиафайлов (S3-compatible) |

MinIO credentials по умолчанию: `minioadmin / minioadmin` (только для локальной разработки).

---

## Environment Variables

### Обязательные

| Переменная | Описание |
|-----------|---------|
| `JWT_SECRET` | Секрет подписи JWT (HS256, минимум 32 байта). Генерация: `openssl rand -hex 32` |

### Auth & Users

| Переменная | Дефолт | Описание |
|-----------|--------|---------|
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@balgyn.local` | Email первого администратора |
| `BOOTSTRAP_ADMIN_PASSWORD` | `admin12345` | Пароль первого администратора |

### Database

| Переменная | Дефолт | Описание |
|-----------|--------|---------|
| `POSTGRES_PASSWORD` | `postgres` | Пароль PostgreSQL |

### Application

| Переменная | Дефолт | Описание |
|-----------|--------|---------|
| `APP_SEED_CATALOG` | `true` | Заполнить каталог тестовыми данными при первом старте |
| `SWAGGER_ENABLED` | `true` | Включить Swagger UI |
| `FRONTEND_BASE_URL` | `http://localhost:5174` | URL фронтенда (для redirect'ов после оплаты) |

### MinIO / Storage

| Переменная | Дефолт | Описание |
|-----------|--------|---------|
| `STORAGE_ENABLED` | `true` (в Docker) / `false` (локально) | Включить MinIO |
| `MINIO_ROOT_USER` | `minioadmin` | Логин MinIO |
| `MINIO_ROOT_PASSWORD` | `minioadmin` | Пароль MinIO |
| `MINIO_ENDPOINT` | `http://minio:9000` | Внутренний адрес MinIO (для backend) |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | Публичный адрес MinIO (для URL в браузере) |
| `MINIO_BUCKET` | `balgyn-media` | Название бакета |

### Freedom Pay (оплата — Kazakhstan)

| Переменная | Дефолт | Описание |
|-----------|--------|---------|
| `FREEDOMPAY_MERCHANT_ID` | пусто (stub-режим) | ID мерчанта Freedom Pay |
| `FREEDOMPAY_SECRET_KEY` | пусто | Секретный ключ |
| `FREEDOMPAY_TESTING_MODE` | `true` | Sandbox-режим Freedom Pay |
| `FREEDOMPAY_RESULT_URL` | — | URL для callback от Freedom Pay |

### PayPal (оплата — international)

| Переменная | Дефолт | Описание |
|-----------|--------|---------|
| `PAYPAL_MODE` | `sandbox` | `sandbox` или `live` |
| `PAYPAL_CLIENT_ID` | пусто (stub-режим) | Client ID из PayPal Developer |
| `PAYPAL_CLIENT_SECRET` | пусто | Client Secret |
| `PAYPAL_WEBHOOK_ID` | пусто | Webhook ID для верификации |

### CDEK (доставка)

| Переменная | Дефолт | Описание |
|-----------|--------|---------|
| `CDEK_CLIENT_ID` | пусто (stub-режим) | Client ID из личного кабинета CDEK |
| `CDEK_CLIENT_SECRET` | пусто | Client Secret |
| `CDEK_WEBHOOK_TOKEN` | пусто | Токен верификации webhook'ов CDEK |
| `CDEK_BASE_URL` | `https://api.edu.cdek.ru/v2` | CDEK API URL (sandbox) |
| `CDEK_SENDER_CITY` | `270` | Код города отправки (270 = Алматы) |

---

## Development — Integrations

Все три интеграции работают в **stub-режиме** если ключи не заданы. Для локальной разработки ключи не нужны.

### Freedom Pay

`FREEDOMPAY_MERCHANT_ID` пусто → stub-режим:
- API Freedom Pay не вызывается
- `POST /api/v1/payments/initiate` возвращает фиктивный `paymentUrl`
- Проверить полный флоу в sandbox: заполните `FREEDOMPAY_MERCHANT_ID`, `FREEDOMPAY_SECRET_KEY`, `FREEDOMPAY_TESTING_MODE=true` и используйте ngrok для `FREEDOMPAY_RESULT_URL`

### PayPal

`PAYPAL_CLIENT_ID` пусто → запросы к PayPal API будут падать с ошибкой аутентификации:
- Для sandbox: зарегистрируйтесь на [developer.paypal.com](https://developer.paypal.com), создайте sandbox app, заполните `PAYPAL_CLIENT_ID` / `PAYPAL_CLIENT_SECRET` / `PAYPAL_WEBHOOK_ID`
- Режим по умолчанию: `PAYPAL_MODE=sandbox`

### CDEK

`CDEK_CLIENT_ID` пусто → stub-режим:
- Расчёт стоимости возвращает фиктивные данные с `sourcedFromStub: true`
- Поиск ПВЗ и городов работает через stub-данные
- Для реальных данных: заполните `CDEK_CLIENT_ID` + `CDEK_CLIENT_SECRET`, измените `CDEK_BASE_URL` на `https://api.cdek.ru/v2` (прод) или оставьте sandbox

---

## Common Commands

### Docker

```bash
# Поднять все сервисы (foreground — видны логи)
docker compose up --build

# Поднять в фоне
docker compose up -d --build

# Остановить
docker compose down

# Остановить и удалить данные (PostgreSQL + MinIO volumes)
docker compose down -v

# Логи конкретного сервиса
docker compose logs -f app
docker compose logs -f frontend
docker compose logs -f db
```

### Backend (Gradle)

```bash
# Запустить тесты
./gradlew test --no-daemon

# Запустить тесты с принудительным перепрогоном
./gradlew test --no-daemon --rerun

# Собрать JAR (без тестов)
./gradlew bootJar --no-daemon -x test

# Запустить локально без Docker (нужны JDK 21 + PostgreSQL на :5432)
./gradlew bootRun
```

Windows:
```powershell
.\gradlew.bat test --no-daemon
```

### Frontend

```bash
cd frontend

# Установить зависимости
npm install

# Dev-сервер (http://localhost:5173)
npm run dev

# Продакшн-сборка
npm run build

# TypeScript-проверка
npx tsc --noEmit
```

### База данных

```bash
# Подключиться к PostgreSQL
docker compose exec db psql -U postgres -d balgynbol-spring

# Экспорт дампа
scripts/export-db.ps1   # Windows
scripts/backup-postgres.sh  # Linux/macOS

# Импорт дампа
scripts/import-db.ps1   # Windows
```

---

## Project Structure

```
balgyn/
├── src/main/java/com/nurba/java/
│   ├── config/          # Spring конфиги, MinIO, seed, bootstrap-admin
│   ├── controller/      # REST контроллеры
│   ├── domain/          # JPA сущности (AppUser, Order, Design, Payment…)
│   ├── dto/             # Request/Response DTO
│   ├── enums/           # Role, OrderStatus, PaymentStatus…
│   ├── payment/         # Freedom Pay и PayPal клиенты
│   ├── repositories/    # Spring Data репозитории
│   ├── security/        # JWT, SecurityConfig, rate limiter
│   └── service/         # Бизнес-логика
│
├── src/main/resources/
│   ├── application.properties
│   ├── application-prod.properties
│   └── db/
│       ├── migration/   # Flyway V1–V28
│       └── seed/        # seed_catalog.sql (16 дизайнов)
│
├── frontend/
│   ├── src/
│   │   ├── admin/       # Админ-панель (AdminDesignsPage, AdminOrdersPage…)
│   │   ├── app/         # Провайдеры (auth, cart, currency)
│   │   ├── pages/       # Страницы магазина
│   │   ├── shared/      # UI-компоненты, API-клиент, утилиты
│   │   └── widgets/     # Шапка, подвал, каталог-виджеты
│   ├── public/locales/  # i18n: ru / kk / en
│   ├── Dockerfile       # node:22-alpine → nginx:1.27-alpine
│   └── nginx.conf       # SPA fallback + gzip + cache headers
│
├── docker-compose.yml          # Локальная разработка
├── docker-compose.prod.yml     # Продакшн (nginx + TLS + Certbot)
├── Dockerfile                  # Backend (JDK 21 builder → JRE 21)
├── .env.example                # Шаблон переменных окружения
└── seed_catalog.sql            # Reference-копия сида (не используется напрямую)
```

---

## Troubleshooting

### `JWT_SECRET` not set — приложение не стартует

```
Error: JWT_SECRET is not configured. Set the JWT_SECRET environment variable.
```

**Решение:** убедитесь, что `.env` существует и `JWT_SECRET` не пустой.

```bash
cp .env.example .env
```

---

### Port already in use

```
Error: bind: address already in use
```

**Решение:** остановите процесс, занимающий порт:

```bash
# Найти процесс на порту 5432 (PostgreSQL)
lsof -i :5432        # macOS/Linux
netstat -ano | findstr :5432   # Windows
```

Или смените порт в `docker-compose.yml` (например: `"5433:5432"`).

---

### MinIO images not working (STORAGE_ENABLED=false)

Если видите ошибки загрузки изображений — `STORAGE_ENABLED` выставлен в `false`.  
В Docker-режиме MinIO включён автоматически (`STORAGE_ENABLED=true` hardcoded в docker-compose.yml).  
При локальном запуске без Docker: поднимите MinIO отдельно и выставьте `STORAGE_ENABLED=true` в `.env`.

---

### Flyway migration failed

```
ERROR: Found non-empty schema(s) "public" without schema history table
```

**Решение:** сбросьте базу и начните заново:

```bash
docker compose down -v
docker compose up --build
```

---

### `docker compose up` зависает на health check

Backend не проходит health check — проверьте логи:

```bash
docker compose logs -f app
```

Частые причины:
- PostgreSQL не успел стартовать (обычно решается само через 30–60 секунд)
- Ошибка в Flyway-миграции (смотреть в логах app)

---

### Пустой каталог в магазине

`APP_SEED_CATALOG` выставлен в `false` или seed уже пропущен (каталог не пустой в БД).  
Для чистого сида: `docker compose down -v && docker compose up --build`.

---

## Production Notes

Обязательно перед деплоем в прод:

| Переменная | Что сделать |
|-----------|------------|
| `JWT_SECRET` | Сгенерировать: `openssl rand -hex 32` (минимум 32 байта) |
| `MINIO_ROOT_USER` | Изменить с `minioadmin` |
| `MINIO_ROOT_PASSWORD` | Изменить с `minioadmin`, закрыть порты 9000/9001 фаерволом |
| `POSTGRES_PASSWORD` | Изменить с `postgres` |
| `BOOTSTRAP_ADMIN_PASSWORD` | Изменить с `admin12345`; очистить переменную после первого входа |
| `FREEDOMPAY_MERCHANT_ID` | Заполнить боевыми данными |
| `FREEDOMPAY_RESULT_URL` | Указать публичный URL бэкенда |
| `PAYPAL_CLIENT_ID / SECRET` | Переключить `PAYPAL_MODE=live`, заполнить production credentials |
| `CDEK_CLIENT_ID / SECRET` | Заполнить боевыми ключами |
| `CDEK_WEBHOOK_TOKEN` | Задать токен и зарегистрировать webhook в ЛК СДЭК |
| `APP_SEED_CATALOG` | Выставить в `false` (не нужен в проде) |
| `SWAGGER_ENABLED` | Выставить в `false` (задаётся автоматически при `SPRING_PROFILES_ACTIVE=prod`) |
| `MINIO_PUBLIC_URL` | Указать публичный домен MinIO (например, `https://media.balgyn.kz`) |
| `FRONTEND_BASE_URL` | Указать публичный URL магазина (например, `https://balgyn.kz`) |

**Продакшн Compose:**
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Включает: nginx с TLS, Certbot, prod-профиль Spring (Swagger off, HTTPS-cookie).
