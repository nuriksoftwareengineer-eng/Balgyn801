# DRY_RUN_NEW_CUSTOMER.md
> Сценарий: новый клиент — от главной до истории заказов  
> Дата: 2026-06-21 | Окружение: production docker-compose.prod.yml

---

## ШАГИ И АНАЛИЗ

---

### Шаг 1 — Главная страница (`/`)

**Что видит пользователь:**  
Лендинг магазина. Мегаменю в SiteNavbar с группами каталога. Hero-секция, коллекции, CTA.

**Данные:**
- SiteNavbar вызывает `GET /api/v1/catalog/groups` (публично, без JWT)
- Ответ: список `CatalogGroupSummary[]` с `name`, `slug`, вложенными коллекциями для мегаменю

**API:** `GET /api/v1/catalog/groups`

**Что может сломаться:**
- Backend не запущен → nginx вернёт 502 на `/api/**`; страница рендерится без меню (деградация graceful — меню пустое, не крэш)
- MinIO недоступен → главное изображение не загружается (img broken), текст и кнопки работают

**Визуально:** чёрно-белый стиль, минималистичный. Мегаменю открывается при hover/focus.

---

### Шаг 2 — Каталог (`/catalog`)

**Что видит пользователь:**  
Список групп каталога с коллекциями. Breadcrumb: Каталог. i18n-переключатель в SiteNavbar.

**Данные:**
- `GET /api/v1/catalog/groups` — уже в React Query cache (staleTime не задан — refetch на mount)
- Рендерит `CatalogGroupSummary[]` → карточки групп

**API:** `GET /api/v1/catalog/groups`

**Что может сломаться:**
- Нет данных → пустая страница с "нет коллекций" (empty state в компоненте)
- 0 опубликованных групп → admin должен создать хотя бы одну до запуска

---

### Шаг 3 — Страница коллекции (`/catalog/:groupSlug/:collectionSlug`)

**Что видит пользователь:**  
Сетка карточек дизайнов. Название коллекции, breadcrumb. Карточки с `mainImageUrl`, ценой, названием.

**Данные:**
- `GET /api/v1/catalog/collections/:slug` → `CollectionDetail` с `designs: DesignSummary[]`
- Каждый дизайн: `id`, `name`, `slug`, `mainImageUrl`, `active`
- Цена отображается через `priceRange(garments)` — нужен отдельный запрос дизайна

**API:** `GET /api/v1/catalog/collections/{slug}`

**Что может сломаться:**
- Несуществующий slug → 404 бэкенд; React Router покажет `NotFoundPage`
- `mainImageUrl` = null → карточка без фото (обработано в UI через placeholder)
- Нет PUBLISHED дизайнов в коллекции → пустая сетка

---

### Шаг 4 — Страница дизайна (`/catalog/:groupSlug/:collectionSlug/:designSlug`)

**Что видит пользователь:**  
Главное фото дизайна. Галерея. Выбор: тип одежды → цвет → размер. Цена. Кнопка "В корзину". Индикатор остатка.

**Данные:**
- `GET /api/v1/catalog/designs/{slug}` → `DesignDetail` с полями:
  - `gallery: string[]` — список URL изображений галереи
  - `garments: GarmentDetail[]` — каждый `GarmentDetail` имеет:
    - `prices: GarmentPrice[]` (KZT, USD, EUR, RUB)
    - `colors: ColorInfo[]`, `sizes: SizeInfo[]`
    - `stockMap: Record<colorId, Record<sizeId, quantity>>` — остатки
- Bulk fetch inventory (исправлен N+1): 1 запрос к БД для всех garment ID

**API:** `GET /api/v1/catalog/designs/{slug}`

**Что может сломаться:**
- `stockMap` пустой → все варианты OOS (out-of-stock); кнопка "В корзину" заблокирована
- Нет KZT цены → `kztPrice()` вернёт null; цена не отображается (блок скрыт)
- `gallery` = [] → галерея не рендерится; показывается только `mainImageUrl`

**Визуально:**  
Выбранный цвет подсвечивается swatchем. Размер — кнопка с OOS-пометкой при `qty=0`. Галерея — grid под главным фото.

---

### Шаг 5 — Добавить в корзину

**Что видит пользователь:**  
Клик по "В корзину" → анимация, кнопка меняется на "Добавлено". Счётчик в хедере обновляется.

**Данные:**
- **Никаких API-вызовов.** Корзина хранится в localStorage через `useCart` / `cart-context`.
- `DesignLine` в корзине: `designGarmentId`, `colorId`, `sizeId`, `qty`, `price`, `title`, `garmentLabel`, `colorName`, `sizeLabel`

**API:** нет (только localStorage)

**Что может сломаться:**
- localStorage отключён (Safari приватный режим) → корзина не сохраняется между сессиями (не крэш, но данные теряются при перезагрузке)
- Пользователь добавляет > доступного qty → валидация на стороне клиента пустая; сервер проверит при создании заказа (pessimistic lock + CHECK)

---

### Шаг 6 — Корзина → Оформление заказа (`/cart`)

**Что видит пользователь:**  
Список товаров. Кнопка "Оформить заказ". Если не авторизован → редирект на `/login` с сохранением intent в sessionStorage. После логина → автоматический возврат и открытие checkout.

**5-шаговый checkout:**

| Шаг | Экран | API |
|-----|-------|-----|
| 1 | Контакты (имя, телефон, Telegram опционально) | нет |
| 2 | Регион (KZ/RU/US) | `GET /delivery/methods?countryIso2=KZ` (prefetch на шаге 2) |
| 3 | Метод доставки (из ответа delivery/methods) | уже в cache |
| 4 | Детали (для CDEK: город → ПВЗ → получатель; для TAXI/POSTAL: адрес) | `GET /delivery/cdek/cities?q=`, `GET /delivery/cdek/points?cityCode=`, `POST /delivery/cdek/calculate-order` |
| 5 | Итог (summary, кнопка "Подтвердить") | `POST /order` |

**API при подтверждении:** `POST /api/v1/order`  
Body: `CreateOrderRequest` → `customerName`, `customerPhone`, `deliveryType`, `countryIso2`, `pvzCode`, `items[]`, `address`  

**Что сервер делает:**
- Валидирует поля
- Создаёт `Order` со статусом `PENDING_PAYMENT`
- Резервирует inventory (pessimistic lock, 60 минут)
- Возвращает `OrderResponse` с `id`, `totalPrice`, `items[]`, `deliveryType`, `address`

**Что может сломаться:**
- `PICKUP` выбран → шаг 4 пропускается (код: `if (step === 3 && !requiresAddress) { setStep(5) }`)
- Нет доступных методов доставки для KZ → пустой список, amber предупреждение, пользователь не может перейти дальше
- CDEK API недоступен → `calculateCdekTariffByOrder` падает; показывается `cdekCalcError` (amber); пользователь всё равно может продолжить (тариф не обязателен для submit)
- `POST /order` → 409 (pessimistic lock) → `GlobalExceptionHandler` → HTTP 409 → `formError` отображается

**Визуально:**  
Step indicator с 5 шагами. Sidebar со списком товаров и итоговой суммой (sticky, desktop). Mobile — sidebar скрыт, только кнопка и total.

---

### Шаг 7 — Выбор оплаты

**Что видит пользователь:**  
После успешного `POST /order` → `OrderSuccess` компонент. Две карточки: "Банковская карта" (Freedom Pay) + PayPal. По умолчанию выбран Freedom Pay.

**При клике "Оплатить" (Freedom Pay):**
1. `POST /api/v1/payments/init` → `{ orderId, provider: "FREEDOM_PAY", returnUrl }`
2. Сервер создаёт Payment запись (PENDING), строит URL Freedom Pay с MD5 подписью
3. Ответ: `{ paymentUrl: "https://pay.freedompay.money/..." }`
4. `window.location.href = paymentUrl` — редирект на Freedom Pay
5. localStorage: `saveLastPayment({ orderId, totalPrice, provider: "FREEDOM_PAY" })`

**При клике "Оплатить" (PayPal):**
1. `POST /api/v1/payments/paypal/create-order` → `{ orderId, returnUrl, cancelUrl }`
2. Сервер создаёт PayPal Order через API, возвращает `{ paymentUrl, cancelToken }`
3. `saveLastPayment({ orderId, totalPrice, provider: "PAYPAL", cancelToken })`
4. `window.location.href = paymentUrl` — редирект на PayPal

**Что может сломаться:**
- `FREEDOMPAY_MERCHANT_ID` пустой → `FreedomPayStartupValidator` падает при старте; не дойдёт до этого шага (fail-fast сработал)
- `PAYPAL_CLIENT_ID` пустой → createOrder бросит исключение → `paymentError` → inline ошибка под кнопкой
- Сеть разорвана до редиректа → `paymentBusy=true`, потом ошибка в catch → `paymentError`

---

### Шаг 8 — Возврат после оплаты (`/payment-return`)

**Что видит пользователь:**  
Spinner + "Подтверждаем платёж…" (1-2 секунды).

**Freedom Pay:**
- URL содержит `?pg_result=1&pg_order_id=...`
- `fpResult === "1"` → `navigate("/payment/success", { replace: true })`
- Сервер УЖЕ обработал callback до этого (FreedomPay POSTит на `pg_result_url`)

**PayPal:**
- URL содержит `?token=PAYPAL_ORDER_ID&PayerID=...`
- `capturePayPalOrder(paypalToken)` → `POST /payments/paypal/capture/{id}`
- Если `payment.status === "SUCCEEDED"` → `/payment/success`
- Если другой статус → `/payment/failed?error=...`

**Страница обёрнута в `<ErrorBoundary>`** — любой крэш рендерит fallback с кнопкой "Перезагрузить страницу".

**Что может сломаться:**
- Freedom Pay redirects с `pg_result=0` → `/payment/failed` (не крэш)
- PayPal capture сетевая ошибка → catch → `/payment/failed`
- `PaymentReturnPage` рендерится без параметров → `navigate("/payment/failed?error=UNKNOWN_RETURN")`

---

### Шаг 9 — Успешная оплата (`/payment/success`)

**Что видит пользователь:**  
Чёрный checkmark circle. Заголовок "Платёж успешен". Блок с номером заказа, суммой, методом оплаты (из localStorage). Кнопки: "Мои заказы" → `/orders`, "В каталог" → `/catalog`.

**Данные:**
- `loadLastPayment()` из localStorage → `{ orderId, totalPrice, provider }`
- После загрузки: `clearLastPayment()` + `clearPendingPayment()` (recovery banner не появится)

**API:** нет

---

### Шаг 10 — История заказов (`/orders`)

**Что видит пользователь:**  
Hero-баннер "Мои заказы". Список заказов с:
- Статус-пилюля (цветная, i18n label)
- Дата (toLocaleDateString с kk-KZ / ru-RU / en-US)
- Сумма (через `useCurrency().format`)
- Список товаров (productTitle, sizeLabel, colorName)
- Кнопка "Оплатить" для PENDING_PAYMENT заказов
- Скелетон при загрузке

**Данные:**
- `GET /api/v1/me/orders` (JWT required, `RequireAuth` guard)
- React Query `useQuery({ queryKey: ["my-orders", token], staleTime: 30_000 })`

**API:** `GET /api/v1/me/orders`

**Что может сломаться:**
- Токен истёк → 401 → `useQuery` error → отображается `error.message`
- Нет заказов → empty state (🧵 + текст + кнопка в каталог)
- Заказ в статусе PENDING_PAYMENT → кнопка "Оплатить" → `navigate("/cart", { state: { retryOrderId, retryAmount } })` → RecoveryBanner в CartPage

---

## ИТОГ СЦЕНАРИЯ

| Шаг | Статус | Риск |
|-----|--------|------|
| Главная | ✅ | MinIO image |
| Каталог | ✅ | Нет данных → empty |
| Коллекция | ✅ | Slug 404 → NotFoundPage |
| Дизайн | ✅ | stockMap пустой → OOS |
| Корзина | ✅ | localStorage Safari private |
| Checkout | ✅ | CDEK API флакки |
| Оплата (FP) | ✅ | MERCHANT_ID fail-fast |
| Оплата (PP) | ✅ | PayPal API timeout |
| Return page | ✅ | ErrorBoundary страхует |
| История | ✅ | JWT expiry |

**Путь проходим полностью. Нет крэшей без обработки.**
