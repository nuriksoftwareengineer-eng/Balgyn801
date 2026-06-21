# DRY_RUN_ADMIN.md
> Сценарий: полный путь администратора — создание дизайна до архивации  
> Дата: 2026-06-21

---

## УЧАСТНИКИ СИСТЕМЫ

| Компонент | Роль |
|-----------|------|
| AdminDesignsPage | Управление дизайнами |
| AdminDesignVariantsPage | Управление вариантами (garments, цвета, размеры, цены, остатки) |
| AdminOrderDetailPage | Просмотр заказа, смена статуса, CDEK-отправление |
| MediaUploadController | `POST /media/upload` (MinIO) |
| DesignController | CRUD дизайнов |
| DesignGarmentController | CRUD вариантов |
| InventoryController | Управление остатками |

---

## ШАГ 1 — Вход администратора

**URL:** `/login` → `/admin`

**Что происходит:**
1. `POST /auth/login` → получает `accessToken` (15 мин), HttpOnly refresh cookie
2. `GET /auth/me` → `roles: ["USER", "ADMIN"]`
3. `RequireAdmin` компонент проверяет `isAdmin` → рендерит AdminLayout
4. AdminDashboardPage загружает: `GET /order`, `GET /customer`, `GET /admin/designs`
5. KPI: "Всего заказов", "Новые заказы" (status=NEW), "Дизайнов активно" (PUBLISHED), "Клиентов"

---

## ШАГ 2 — Создание дизайна

**URL:** `/admin/designs`

**Что видит admin:**
- Список всех дизайнов (все статусы) с фильтром по группе/коллекции
- Форма создания вверху (имя, коллекция, slug, описание)
- Кнопка "Добавить дизайн"

**Действие: создать новый дизайн**
```
Имя: "Brand of Sacrifice"
Группа: Графика → Коллекция: "Metal"
Slug: автогенерируется slugify("Brand of Sacrifice") = "brand-of-sacrifice"
```

**API:**
```
POST /api/v1/admin/designs
Body: { collectionId: 5, name: "Brand of Sacrifice", slug: "brand-of-sacrifice",
        description: null, mainImageUrl: null, gallery: [] }
```

**Ответ:** `AdminDesign { id: 15, status: "DRAFT", activeGarmentCount: 0, ... }`

**Визуально:**
- Список обновляется мгновенно (React Query invalidate)
- Статус-бейдж "ЧЕРНОВИК" (серый)
- PublishGuidance предупреждает: "Для публикации: нет главного изображения, нет активных вариантов"

---

## ШАГ 3 — Добавить изображения

**В форме редактирования:**

**Главное изображение:**
- `<input type="file" accept="image/*">`
- `handleMainUpload()` → `POST /media/upload` (multipart/form-data)
- MinIO сохраняет файл, возвращает `{ publicUrl: "https://cdn.balgyn.kz/..." }`
- `mainImageUrl` → сохраняется при следующем save дизайна

**Галерея (multiple files):**
- `handleGalleryUpload()` → последовательный upload каждого файла
- `gallery: string[]` — массив URL
- При save: `gallery: ["url1", "url2", ...]` в теле запроса

**API:**
```
POST /api/v1/media/upload (multipart, Authorization: Bearer token)
Response: { publicUrl: "https://cdn..." }
```

**Что может сломаться:**
- MinIO недоступен → 500 → `setFormError("Ошибка загрузки")` → показывается пользователю
- Файл > лимита (настройка MinIO/nginx) → 413 → ошибка
- ⚠️ Нет клиентской валидации размера файла (A-M8 deferred)

---

## ШАГ 4 — Добавить варианты одежды

**URL:** `/admin/designs/15/variants`

**Что видит admin:**
- Список garments (пусто для нового дизайна)
- Форма: тип одежды (HOODIE / T_SHIRT / etc.), добавить

**Действие: добавить HOODIE**
```
POST /api/v1/admin/design-garments
Body: { designId: 15, garmentType: "HOODIE" }
Response: DesignGarment { id: 101, active: true }
```

**Добавить цвет к garment:**
```
POST /api/v1/admin/design-garments/101/colors
Body: { colorId: 3 }  (Black)
```

**Добавить размер к garment:**
```
POST /api/v1/admin/design-garments/101/sizes
Body: { sizeId: 2 }  (M)
```

**Установить цену:**
```
POST /api/v1/admin/design-garment-prices
Body: { designGarmentId: 101, currency: "KZT", amount: 18000 }
```

**Установить остатки:**
```
POST /api/v1/admin/inventory
Body: { designGarmentId: 101, colorId: 3, sizeId: 2, quantity: 10 }
```

---

## ШАГ 5 — Опубликовать дизайн

**Кнопка "Опубликовать" в AdminDesignsPage:**

```typescript
publishMut.mutate(d.id);
// → PATCH /api/v1/admin/designs/15/publish
// Response: AdminDesign { status: "PUBLISHED" }
```

**Проверки на сервере:**
- Дизайн должен иметь хотя бы 1 активный garment
- Если нет → 400 "Cannot publish design without active garments"

**Визуально:**
- Бейдж меняется на "ОПУБЛИКОВАН" (зелёный)
- PublishGuidance исчезает
- Дизайн появляется в публичном каталоге (`/catalog/...`)
- Сторфронт: `GET /catalog/designs/{slug}` возвращает этот дизайн

---

## ШАГ 6 — Архивировать дизайн

**Кнопка "Архивировать" в AdminDesignsPage:**

```typescript
onClick={() => {
  if (window.confirm(`Архивировать дизайн «${d.name}»? Он будет скрыт из каталога.`)) {
    archiveMut.mutate(d.id);
  }
}}
```

**window.confirm** — браузерный диалог. Пока клиент не нажал OK/Отмена, запрос не отправляется.

**При подтверждении:**
```
PATCH /api/v1/admin/designs/15/archive
Response: AdminDesign { status: "ARCHIVED" }
```

**Визуально:**
- Бейдж: "АРХИВ" (оранжевый)
- Дизайн пропадает из публичного каталога
- В admin списке остаётся видимым

---

## ШАГ 7 — Восстановить из архива

**Кнопка "Опубликовать" снова (или перевести в DRAFT):**

```
PATCH /api/v1/admin/designs/15/publish
Response: AdminDesign { status: "PUBLISHED" }
```

Дизайн снова доступен в каталоге.

---

## ШАГ 8 — Просмотр заказа и смена статуса

**URL:** `/admin/orders/42`

**Загрузка:**
```
GET /api/v1/order/42   (+ EntityGraph: customer, deliveryAddress, orderItems)
GET /api/v1/cdek-shipment/by-order/42  (отдельный запрос)
```

**Что видит admin:**
- Данные заказа: клиент, адрес, товары, сумма
- Блок "Статус": `<select>` с доступными статусами
- Блок "CDEK отправление"

**Смена статуса:**
- Admin выбирает "CONFIRMED" в select → `statusDirty = true` → кнопка "Сохранить" активна
- Нажимает "Сохранить":
```
PATCH /api/v1/order/42/status
Body: { status: "CONFIRMED" }
```
- `statusDraft` сбрасывается; query invalidates; страница перезагружает данные

---

## ШАГ 9 — Создать CDEK-отправление

**Кнопка в секции CDEK:**

```typescript
onClick={() => {
  const label = shipment
    ? "Повторить создание отправления CDEK?"
    : "Создать отправление CDEK? Это выставит счёт через CDEK API.";
  if (window.confirm(label)) shipmentMut.mutate("create");
}}
```

**window.confirm** — защита от случайного клика (выставление счёта через API).

**При подтверждении:**
```
POST /api/v1/cdek-shipment/by-order/42/create
Response: CdekShipmentResponse {
  id: 7,
  cdekOrderUuid: "UUID",
  status: "CREATED",
  trackingNumber: "1234567890"
}
```

**Что может сломаться:**
- CDEK API недоступен → 502/503 → `shipmentError` отображается
- Order не CONFIRMED/SHIPPED (неверный статус) → 400
- Нет pvzCode в заказе (PICKUP/TAXI) → CDEK API отклоняет (не CDEK delivery)

---

## ШАГ 10 — CDEK Sync статуса

**Кнопка "Синхронизировать":**
```
POST /api/v1/cdek-shipment/by-order/42/sync
→ Запрос к CDEK API: GET /v2/orders/{cdekOrderUuid}
→ Обновляет CdekShipment.status
```

Используется для ручного обновления статуса до поступления вебхука.

---

## ПРЕДУПРЕЖДЕНИЯ

1. **Файлы без валидации размера**: admin может загрузить 50MB видео → nginx/MinIO вернёт 413 → непонятная ошибка для admin (A-M8 deferred)

2. **Garment без цен**: если admin забыл установить цены — дизайн публикуется, но на странице дизайна цена не отображается. PublishGuidance предупреждает только об `activeGarmentCount=0`, но не о ценах.

3. **Нет bulk inventory**: при добавлении 10 цветов × 5 размеров = 50 записей inventory нужно создать по одной (A-H2 deferred)

**СТАТУС: ПРОХОДИТ ✅ (с предупреждениями выше)**
