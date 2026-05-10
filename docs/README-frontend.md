# Frontend (React + Vite) — структура проекта

Корень: `frontend/`. Сборка: `npm run dev` / `npm run build`.

## Точка входа

- `index.html` → `src/main.tsx` — монтирование React, провайдеры, роутер.
- `src/App.tsx` — оболочка приложения (если используется из `main`).

## Маршрутизация

`src/app/router.tsx` — `createBrowserRouter`:

- Публичная витрина под `MainLayout`: `/`, `/catalog`, `/catalog/:productId`, `/cart`, `/custom-design`, `/about`.
- Авторизация: `/login`, `/register` (`AuthShellLayout`).
- Админка: `/admin`, `/admin/orders`, `/admin/orders/:orderId`, `/admin/products` — обёртка `RequireAdmin` + `AdminLayout`.

## Слои (ориентир по папкам)

| Путь | Назначение |
|------|------------|
| `src/app/` | Провайдеры (`providers.tsx`), роутер, **контекст авторизации** (`auth-context.tsx`), корзина (`cart-provider.tsx`, `use-cart.ts`, `cart-context.ts`). |
| `src/pages/` | Страницы: главная, каталог, карточка товара, корзина, о проекте, вход/регистрация. |
| `src/admin/` | Админ-дэшборд, товары, `RequireAdmin`, общий `AdminLayout`. |
| `src/widgets/` | Крупные блоки UI: шапка, подвал, герой, CTA «свой дизайн», сетка каталога, карточка товара и т.д. |
| `src/shared/ui/` | Переиспользуемые примитивы: кнопка, контейнер, иконки-кнопки. |
| `src/shared/api/` | Контракт с бэкендом: `types.ts`, `http.ts` (`apiFetch`, `ApiError`), `backend-api.ts` (методы + комментарий `BACKEND_API`), при необходимости `queries.ts` (React Query). |
| `src/shared/lib/` | Утилиты: `cn`, форматирование цен, **`auth-storage.ts`** (JWT в `localStorage`, ключ `balgyn_access_token`). |
| `src/shared/constants/` | Тексты/константы контента. |
| `src/assets/` | Статика (картинки и т.п.). |

## API и переменные окружения

- Базовый URL API: `VITE_API_BASE_URL` (см. `frontend/.env.example`), иначе в коде дефолт `http://localhost:8080/api/v1`.
- В Docker Compose для сервиса `frontend` задаётся `VITE_API_BASE_URL=http://localhost:8080/api/v1` (браузер ходит на хост-машину).

## Авторизация

- После логина/регистрации токен пишется в **localStorage** (ключ `balgyn_access_token`, общий для вкладок с тем же origin). Раньше был `sessionStorage` — из‑за этого новая вкладка («Открыть товар») не видела JWT и выглядела как «автовыход».
- Событие **`storage`** в `AuthProvider`: вход или выход в **другой** вкладке синхронизирует сессию без перезагрузки.
- Сброс локальной сессии при ошибке **`/auth/me`** только при ответе **401** (сетевой сбой не выкидывает из аккаунта).
- Запросы с JWT: передача `token` в `apiFetch` / отдельные вызовы (например, multipart загрузка).
- Роль **ADMIN** определяется по `GET /auth/me` (`user.roles`).

## Стили

- Tailwind v4, глобальные стили в `src/index.css`, разметка страниц через utility-классы.

## Команды

```bash
cd frontend
npm ci
npm run dev
npm run lint
npm run build
```
