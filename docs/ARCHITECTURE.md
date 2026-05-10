# Архитектура Balgyn801 (frontend + backend)

Ниже — схемы для быстрой навигации по системе. Рендер Mermaid поддерживают GitHub, многие IDE и плагины Markdown.

---

## 1. Контекст системы

Кто с чем говорит в типичном запуске (локально или Docker).

```mermaid
flowchart LR
  subgraph browser["Браузер"]
    SPA["React SPA\n(Vite)"]
  end

  subgraph backend["Backend"]
    SB["Spring Boot\nпорт 8080"]
    PG[(PostgreSQL)]
    SB --- PG
  end

  SPA -->|"REST JSON\n/api/v1"| SB
```

В продакшене перед приложением обычно стоят **reverse proxy** (HTTPS), возможно **CDN** для статики фронта.

---

## 2. Frontend — структура приложения

Один SPA, разные зоны по маршрутам.

```mermaid
flowchart TB
  subgraph entry["Точка входа"]
    MAIN["main.tsx"]
    ROUTER["AppRouter"]
    PROV["Providers:\nQueryClient\nAuthProvider\nCartProvider"]
  end

  subgraph public_zone["Витрина /"]
    ML["MainLayout"]
    PAGES["HomePage, CatalogPage,\nProductPage, CartPage…"]
    W["widgets/*"]
    ML --> PAGES
    PAGES --> W
  end

  subgraph auth_zone["/auth-*"]
    AUTHshell["AuthShellLayout"]
    LOGIN["LoginPage"]
    REG["RegisterPage"]
    AUTHshell --> LOGIN
    AUTHshell --> REG
  end

  subgraph admin_zone["/admin/*"]
    REQ["RequireAdmin"]
    AL["AdminLayout"]
    ADM["AdminDashboard,\nAdminProducts…"]
    REQ --> AL
    AL --> ADM
  end

  subgraph data_layer["Данные с сервера"]
    RQ["TanStack Query\n(queries / мутации)"]
    API["shared/api:\napiFetch, backend-api.ts,\ntypes"]
    RQ --> API
  end

  MAIN --> PROV
  PROV --> ROUTER
  ROUTER --> ML
  ROUTER --> AUTHshell
  ROUTER --> REQ
  public_zone --> RQ
  admin_zone --> RQ
  AUTH_ZONE_NOTE["AuthContext читает/пишет JWT,\nшапка витрины показывает роли"]
```

**Идея:** витрина и админка **делят один HTTP-клиент и один базовый URL** (`VITE_API_BASE_URL`), но админские мутации всегда идут с **`Authorization: Bearer`**.

---

## 3. Backend — слои и поток запроса

Классический слой для REST-сервиса.

```mermaid
flowchart TB
  subgraph inbound["Вход"]
    FILT["JwtAuthenticationFilter\n(+ цепочка Security)"]
    CTRL["Controller\nimplements *Api"]
  end

  subgraph app["Приложение"]
    SVC["Service / Impl"]
    MAP["MapStruct Mappers"]
    VAL["Validation,\nExceptions"]
  end

  subgraph persistence["Персистентность"]
    REP["Spring Data\nRepositories"]
    ENT["JPA Entities"]
    DB[(PostgreSQL)]
  end

  FILT --> CTRL
  CTRL --> SVC
  SVC --> MAP
  SVC --> REP
  REP --> ENT
  ENT --> DB
```

Контракт REST описан в интерфейсах **`api/*Api`** (и дублируется в OpenAPI для Swagger).

---

## 4. Безопасность: три типа вызовов

Упрощённая модель решений Security.

```mermaid
flowchart TD
  R["HTTP-запрос /api/v1/..."]
  R --> PUB{"Публичный путь?\n(auth/register, login,\nGET /product/**,\nPOST /order, …)"}
  PUB -->|да| OK1["Цепочка фильтров\n→ Controller"]
  PUB -->|нет| JWT{"Есть валидный JWT?"}
  JWT -->|нет| U401["401 Unauthorized"]
  JWT -->|да| ROLE{"Нужна только ADMIN?"}
  ROLE -->|да и не ADMIN| U403["403 Forbidden"]
  ROLE -->|нет или ADMIN ок| OK2["→ Controller"]
```

Точные правила задаются в **`SecurityConfig`**; список публичных/админских путей дублируется для разработчиков в **`frontend/…/backend-api.ts`** (`BACKEND_API`).

---

## 5. Связка «экран → эндпойнт» (примеры)

| Зона фронта | Типичные запросы |
|-------------|------------------|
| Каталог, карточка | `GET /product`, `GET /product/{id}` |
| Корзина → заказ (план) | `POST /order` |
| Вход / регистрация | `POST /auth/login`, `POST /auth/register` |
| После входа | `GET /auth/me` |
| Админ: товары | `POST /product`, `DELETE /product/{id}` (+ тот же `GET` для списка) |
| Админ: заказы (план) | `GET /order`, `GET /order/{id}` |

---

## Связанные документы

- [PLAN_AND_ARCHITECTURE.md](./PLAN_AND_ARCHITECTURE.md) — порядок задач и краткие схемы.
- Корневой [README.md](../README.md) — как запустить проект.
