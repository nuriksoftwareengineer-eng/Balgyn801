# Balgyn801

Монолитный backend на **Spring Boot** и SPA на **React + Vite + Tailwind**, общение по REST (`/api/v1`). База данных — **PostgreSQL**. Опционально всё поднимается через **Docker Compose**.

Используйте этот файл как **пошаговый ориентир**: что уже есть, как запустить, куда двигаться дальше.

---

## Структура репозитория

| Путь | Назначение |
|------|------------|
| `src/main/java/com/nurba/java/` | Backend: API (контракты), контроллеры, сервисы, JPA, DTO, мапперы |
| `frontend/` | Витрина: React Router, TanStack Query, виджеты, страницы |
| `docker-compose.yml` | Сервисы `db`, `app`, `frontend` |
| `Dockerfile` | Многоэтапная сборка: Gradle внутри образа, артефакт `app.jar` |

---

## Что уже реализовано (кратко)

- REST-слой в стиле `*Api` + реализация в контроллерах, OpenAPI (springdoc).
- Заказы, клиенты, товары, часть сущностей под доставку и СДЭК (без полной интеграции с API перевозчика).
- Единый обработчик ошибок (`ProblemDetail`).
- Frontend: роутинг, каталог с API, демо-корзина (счётчик), Docker-сервис на порту **5174** снаружи.

---

## Требования к окружению

- **Java 21** (Gradle toolchain).
- **Node.js 20+** (для локальной работы с `frontend/`).
- **Docker Desktop** (если запуск через Compose).

---

## Шаг 1. Backend локально (без Docker)

1. Поднимите PostgreSQL и создайте БД (или используйте свои параметры).

2. Скопируйте настройки подключения в  
   `src/main/resources/application.properties`  
   (URL, пользователь, пароль).

3. Сборка и тесты:

   ```bash
   ./gradlew test bootJar
   ```

4. Запуск приложения:

   ```bash
   ./gradlew bootRun
   ```

   API по умолчанию: `http://localhost:8080/api/v1`  
   Swagger UI (если подключён springdoc): обычно `http://localhost:8080/swagger-ui.html`.

---

## Шаг 2. Frontend локально

```bash
cd frontend
cp .env.example .env   # при необходимости поправьте VITE_API_BASE_URL
npm install
npm run dev
```

Dev-сервер Vite: `http://localhost:5173`  
Укажите в backend **CORS** нужный origin (сейчас учитываются типичные порты для dev и Docker).

---

## Шаг 3. Запуск через Docker Compose

Образ **app** собирается в Dockerfile через `./gradlew bootJar` (локальный `bootJar` перед этим не обязателен).

```bash
docker compose build app
docker compose up -d
```

При проблемах со слоями кеша Docker:

```bash
docker compose build --no-cache app && docker compose up -d
```

| Сервис | Назначение |
|--------|------------|
| `db` | PostgreSQL `5432` |
| `app` | Spring Boot `8080` |
| `frontend` | Vite dev `5174` → контейнерный порт `5173` |

Проверка API:

```bash
curl -s http://localhost:8080/api/v1/product
```

Проверка фронта: откройте `http://localhost:5174`.

Проверка lint/build фронта **внутри** контейнера Node:

```bash
docker compose run --rm --no-deps frontend sh -c "npm ci && npm run lint && npm run build"
```

Если порт **5174** занят — поменяйте маппинг в `docker-compose.yml`.

---

## Шаг 4. Пересборка backend после изменений кода

```bash
docker compose build --no-cache app
docker compose up -d app
```

При ошибках старта смотрите логи:

```bash
docker compose logs app --tail=100
```

---

## Архитектура backend (ориентир)

Сверху вниз:

1. **`api/`** — контракт REST + аннотации OpenAPI.
2. **`controller/`** — тонкая реализация интерфейсов API.
3. **`service/`** — бизнес-логика; **`service/Impl/`** — реализации.
4. **`repositories/`** — Spring Data JPA.
5. **`domain/`** — сущности JPA.
6. **`dto/`**, **`mapper/`** — запросы/ответы и MapStruct.
7. **`exception/`**, **`config/`** — ошибки, CORS и др.

Интеграции (**СДЭК**, **MinIO/S3**, оплата) логично выносить в отдельные пакеты уровня `integration/` или `client/` и не смешивать HTTP клиента с доменными сущностями наружу.

---

## Дорожная карта по шагам (что делать дальше)

Отмечайте галочками по мере выполнения.

### Этап A — Безопасность и пользователь

- [ ] Подключить **Spring Security** (JWT или cookie-сессия — одно решение на проект).
- [ ] Модель **User** (логин, хеш пароля, роли), связь с бизнес-профилем при необходимости.
- [ ] Эндпойнты: регистрация/вход/refresh (если JWT)/текущий пользователь.
- [ ] Разделить **публичные** маршруты (каталог, расчёт доставки) и **защищённые** (админ, загрузка файлов).

### Этап B — Хранение файлов (MinIO, S3-совместимо)

- [ ] Добавить сервис **MinIO** в `docker-compose` (volume для данных, переменные окружения).
- [ ] Зависимость **AWS SDK v2** в Gradle, конфиг endpoint для Docker (`http://minio:9000`) и локально.
- [ ] Сервис **MediaStorageService**: загрузка (multipart или presigned URL).
- [ ] В БД хранить **`objectKey`** или готовый **URL** для `Product` / дизайнов.

### Этап C — СДЭК (доставка)

- [ ] Клиент **CdekApiClient** + конфиг учётных данных (только из env).
- [ ] Публичное или полупубличное API: **расчёт тарифа**, при необходимости **список ПВЗ**.
- [ ] Создание отправления **после** условия «заказ можно отдавать в логистику» (оплата или правило магазина) — не блокировать длинной транзакцией БД.
- [ ] Синхронизация статусов / трекинг в сущность **`CdekShipment`**.

### Этап D — Оплата

- [ ] Выбор провайдера и сценарий: оплата до отправки или наложка.
- [ ] Таблица/модель **Payment**, webhook, смена статуса заказа → триггер для СДЭК при схеме «сначала оплата».

### Этап E — Frontend

- [ ] Хранение токена / cookie и заголовки для защищённых запросов.
- [ ] Экраны входа/регистрации (если нужны покупателю).
- [ ] Админ: загрузка изображений товара, при необходимости отдельный layout `/admin`.

---

## Полезные команды (шпаргалка)

| Действие | Команда |
|----------|---------|
| Тесты backend | `./gradlew test` |
| Сборка JAR | `./gradlew bootJar` |
| Lint фронта | `cd frontend && npm run lint` |
| Production-сборка фронта | `cd frontend && npm run build` |
| Compose: статус | `docker compose ps` |
| Логи приложения | `docker compose logs -f app` |

---

## Контакты и документы

- Детали шаблона Vite см. [frontend/README.md](frontend/README.md).
- Меняйте этот **README** по мере закрытия этапов дорожной карты — так команда всегда видит актуальный «следующий шаг».
