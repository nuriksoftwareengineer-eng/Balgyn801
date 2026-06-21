# Catalog Refactor — Implementation Plan
**Дата:** 2026-06-21  
**Статус:** PLAN — ожидает утверждения перед реализацией

---

## 1. Текущая структура сущностей

### 1.1 Entity Map (как есть в коде)

| Сущность | Таблица | Назначение |
|----------|---------|-----------|
| `CatalogGroup` | `catalog_groups` | Категория верхнего уровня (напр. «Одежда») |
| `Collection` | `collections` | Коллекция внутри категории (напр. «Осень 2026») |
| `Design` | `designs` | Дизайн — главный продукт. Содержит изображения, slug, статус |
| `DesignGarment` | `design_garments` | Вариант дизайна по типу изделия (HOODIE, T_SHIRT, …) |
| `DesignGarmentPrice` | `design_garment_prices` | Цена варианта в конкретной валюте (KZT/USD/EUR/RUB) |
| `Color` | `colors` | Справочник цветов с hex-кодом |
| `Size` | `sizes` | Справочник размеров (S, M, L, …) |
| `Inventory` | `inventory` | Остатки по ячейке (DesignGarment × Color × Size) |
| `Product` | `products` | **Legacy** — монолитный продукт без реальных вариантов |
| `OrderItem` | `order_items` | Строка заказа. Поддерживает ОБА пути: legacy (product) и новый (designGarment) |

### 1.2 Поля Design (текущее состояние после V25)

```
designs
├── id              BIGSERIAL PK
├── collection_id   BIGINT FK → collections.id NOT NULL
├── name            VARCHAR(255) NOT NULL
├── slug            VARCHAR(255) UNIQUE NOT NULL
├── description     TEXT
├── main_image_url  VARCHAR(512)
├── gallery         JSONB (List<String>)
├── status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'   ← добавлен V25
└── created_at      TIMESTAMP
```

### 1.3 Поля DesignGarment

```
design_garments
├── id           BIGSERIAL PK
├── design_id    BIGINT FK → designs.id NOT NULL
├── garment_type VARCHAR(30) NOT NULL   (enum: T_SHIRT, HOODIE, SWEATSHIRT, LONGSLEEVE, OVERSIZE_TSHIRT, ZIP_HOODIE)
└── active       BOOLEAN NOT NULL DEFAULT TRUE
```

### 1.4 Поля Product (legacy)

```
products
├── id           BIGSERIAL PK
├── title        VARCHAR
├── description  TEXT
├── price        NUMERIC    ← единственная цена, одна валюта, KZT
├── image_url    VARCHAR
├── in_stock     BOOLEAN
├── category     VARCHAR    ← строка, не FK
├── sizes        JSONB      ← просто массив строк ["S","M","L"]
├── colors       JSONB      ← [{name,hexCode}]   нет ID, нет складского учёта
├── weight_grams INTEGER
└── created_at   TIMESTAMP
```

---

## 2. Полная ER-диаграмма текущей модели

```
catalog_groups
│  id, name, slug, sort_order, active, created_at
│
└─< collections
      │  id, group_id, name, slug, description,
      │  cover_image_url, banner_image_url, sort_order, active, created_at
      │
      └─< designs
            │  id, collection_id, name, slug, description,
            │  main_image_url, gallery (jsonb), status, created_at
            │
            └─< design_garments
                  │  id, design_id, garment_type, active
                  │
                  ├─< design_garment_prices
                  │     id, design_garment_id, currency, amount
                  │     UNIQUE (design_garment_id, currency)
                  │
                  ├─>< design_garment_colors    (M:N join)
                  │     design_garment_id, color_id
                  │
                  ├─>< design_garment_sizes     (M:N join)
                  │     design_garment_id, size_id
                  │
                  └─< inventory
                        id, design_garment_id, color_id, size_id, quantity
                        UNIQUE (design_garment_id, color_id, size_id)

colors:  id, name, hex_code, sort_order
sizes:   id, label, sort_order

──── LEGACY PATH ─────────────────────────────────────────────────

products:  id, title, price, image_url, in_stock, category,
           sizes (jsonb), colors (jsonb), weight_grams, ...

order_items
│  id, order_id, quantity, unit_price, currency
│
├── product_id        FK → products (legacy path)
├── custom_design_id  FK → custom_designs
├── size_label        VARCHAR (captured for legacy orders)
├── color_name        VARCHAR (captured for legacy orders)
│
├── design_garment_id FK → design_garments (new path)
├── color_id          FK → colors
└── size_id           FK → sizes
```

---

## 3. Проблемы текущей модели

### P-1 [CRITICAL] — Статус дизайна не проверяется на backend при публикации

**Проблема:** Endpoint `PATCH /admin/catalog/designs/{id}/publish` существует в admin-catalog API, однако серверная валидация публикации (наличие изображения + хотя бы 1 активного варианта) реализована **только на стороне фронтенда**. Прямой HTTP-запрос к endpoint обойдёт все проверки.

**Последствие:** Можно опубликовать дизайн без изображений и вариантов — покупатель увидит пустую карточку.

### P-2 [HIGH] — Нет поля `publishedAt` / `archivedAt`

**Проблема:** Невозможно показать дату публикации в admin-панели, отсортировать «недавно опубликованные», реализовать отложенную публикацию.

### P-3 [HIGH] — Нет поля `sortOrder` у Design

**Проблема:** Порядок дизайнов внутри коллекции определяется датой создания. Нельзя вручную выставить «Рекомендуемые» в топ.

### P-4 [HIGH] — Нет валидации уникальности (design, garmentType)

**Проблема:** Можно создать два варианта с одинаковым `garmentType` для одного дизайна (два HOODIE). В UI это выглядит как дубли.

### P-5 [MEDIUM] — `GarmentType` — закрытый enum в Java, нельзя расширить без деплоя

**Проблема:** Добавить новый тип изделия (VEST, SHORTS) требует изменения Java enum + деплоя. Словарь в БД был бы гибче.

### P-6 [MEDIUM] — `DesignGarmentPrice` не имеет модели скидки

**Проблема:** Нет поля `discountAmount` / `salePrice`. Акционные цены нельзя настроить без хаков.

### P-7 [MEDIUM] — `Color.hexCode` без валидации

**Проблема:** Нет проверки формата `#RRGGBB`. Невалидный hex не отобразится на витрине (color swatch останется пустым).

### P-8 [MEDIUM] — `Collection.active` и `CatalogGroup.active` не каскадируются на Design

**Проблема:** Если отключить группу или коллекцию, дизайны внутри всё равно остаются PUBLISHED и видны по прямому URL `/catalog/{slug}`.

### P-9 [LOW] — `Product` (legacy) и `Design` существуют параллельно

**Проблема:** Два параллельных пути создания заказа (`product_id` и `design_garment_id`). Admin видит товары в двух разных разделах. Код Order маппинга усложнён двойной логикой.

### P-10 [LOW] — Нет индекса по `designs.slug`

**Проблема:** Запрос по slug (основной для витрины) выполняется без индекса. При росте каталога >1000 дизайнов — full scan.

### P-11 [LOW] — `gallery` хранится как `JSONB` в `designs`

**Проблема:** Нельзя переупорядочить изображения в галерее атомарно, нет метаданных (alt-текст, тип). Подойдёт отдельная таблица `design_images`.

---

## 4. Почему покупатель видит дизайн без вариантов

**Цепочка событий:**

1. Администратор создаёт дизайн → статус `DRAFT` (после V25 корректно)
2. **Но:** если admin через API (`PATCH /publish`) или прямым SQL установит `status = PUBLISHED` до добавления вариантов — дизайн появится в каталоге
3. Витрина запрашивает `GET /api/v1/catalog/designs?status=PUBLISHED` (или slug)
4. `DesignPage.tsx` вычисляет `activeGarments = design.garments.filter(g => g.active)` → пустой список
5. Рендерится строка «Нет доступных вариантов» вместо селекторов

**Корень проблемы:** backend `publish` endpoint не проверяет наличие активных вариантов.

**Дополнительный сценарий:** Все варианты деактивированы после публикации (admin выставил `active=false` у всех `DesignGarment`). Дизайн остаётся `PUBLISHED`, но вариантов нет.

---

## 5. Почему Design и Product вызывают путаницу

| Аспект | Product (legacy) | Design (новый) |
|--------|-----------------|---------------|
| Назначение | Монолитный товар | Дизайн — «что вышивается» |
| Варианты | Нет (JSONB массивы) | DesignGarment (реальные строки в БД) |
| Цены | Одна цена, KZT | Multi-currency (KZT/USD/EUR/RUB) |
| Складской учёт | `in_stock: Boolean` | Inventory по ячейке (color × size) |
| Путь в заказе | `order_items.product_id` | `order_items.design_garment_id` |
| Admin UI | Отдельный раздел | `/admin/designs` |
| Витрина | Устарела, не используется | `/catalog/**` |
| Статус | Нет | DRAFT/READY/PUBLISHED/ARCHIVED |

**Визуальная путаница для разработчика:**
- `OrderItem` имеет оба FK одновременно → не очевидно, какой путь «правильный»
- Две разные таблицы описывают «продукт» → изменения цен надо делать в двух местах
- Admin-панель имеет два раздела («Товары» legacy + «Дизайны» новые)

---

## 6. Нужно ли объединять Design и Product

**Вердикт: НЕТ. Сохранить разделение, пометить Product как deprecated.**

**Обоснование:**

1. `Product` используется в уже созданных заказах — внешние ключи из `order_items.product_id` существуют и нельзя удалить без потери истории
2. Стоимость миграции высокая (перенос истории заказов, изменение всех отчётов)
3. `Design` уже является production-ready моделью — все новые заказы идут через неё
4. Достаточно скрыть legacy `Product` из admin UI и запретить создание новых

**Что делать с Product:**
- Оставить таблицу `products` и её FK без изменений
- Убрать раздел «Товары» из admin UI или пометить «только просмотр»
- Запретить `POST /products` (или ограничить ролью `SUPER_ADMIN`)
- В OrderHistory и отчётах: если `product_id != null` → legacy-строка, особый рендер

---

## 7. Production-ready модель для магазина вышивки

### 7.1 Целевая архитектура

```
CatalogGroup  (категория)
  └── Collection  (коллекция)
        └── Design  (дизайн — центральная сущность)
              └── DesignGarment  (вариант: HOODIE / T_SHIRT / …)
                    ├── DesignGarmentPrice[]  (цена × валюта)
                    ├── Color[]               (M:N — доступные цвета)
                    ├── Size[]                (M:N — доступные размеры)
                    └── Inventory[color × size]  (остатки по ячейке)
```

Эта структура **уже корректна**. Она полностью отражает бизнес-домен:
- «Дизайн» — это то, что вышивается (Brand Of Sacrifice, Abstract №3)
- «Вариант» — на каком изделии (худи, футболка)
- Цвет/размер выбирается внутри варианта
- Цена зависит от варианта, а не от цвета/размера (стандартная практика)

### 7.2 Предлагаемая новая модель (минимальные изменения)

Вместо полного переименования сущностей — **добавить недостающие поля и правила**:

#### Design (новые поля)

| Поле | Тип | Назначение |
|------|-----|-----------|
| `sort_order` | `INTEGER` | Ручная сортировка внутри коллекции |
| `published_at` | `TIMESTAMP` | Когда дизайн был опубликован (для «Новинок») |
| `archived_at` | `TIMESTAMP` | Когда архивирован |
| `tags` | `VARCHAR[]` | Теги для фильтрации (необязательно для MVP) |

#### DesignGarment (новые поля)

| Поле | Тип | Назначение |
|------|-----|-----------|
| `sort_order` | `INTEGER` | Порядок вариантов в UI (HOODIE первым) |
| Unique constraint | `(design_id, garment_type)` | Запрет дублей |

#### DesignGarmentPrice (будущее расширение)

| Поле | Тип | Назначение |
|------|-----|-----------|
| `compare_at_amount` | `NUMERIC(12,2)` | «Перечёркнутая» цена для акций |

#### Design (новые индексы)

```sql
CREATE UNIQUE INDEX idx_designs_slug ON designs(slug);          -- уже UNIQUE, но явный индекс
CREATE INDEX idx_designs_sort_order ON designs(collection_id, sort_order);
CREATE INDEX idx_designs_published_at ON designs(published_at DESC) WHERE status = 'PUBLISHED';
```

### 7.3 Итоговая целевая модель (полная схема новых полей)

```
designs (добавить)
├── sort_order   INTEGER
├── published_at TIMESTAMP
└── archived_at  TIMESTAMP

design_garments (добавить)
└── sort_order   INTEGER
    + UNIQUE (design_id, garment_type)

design_garment_prices (будущее, не в MVP)
└── compare_at_amount NUMERIC(12,2)

colors (добавить)
└── CHECK (hex_code ~ '^#[0-9A-Fa-f]{6}$')   -- валидация формата

sizes (без изменений)
```

---

## 8. Жизненный цикл дизайна: DRAFT → READY → PUBLISHED → ARCHIVED

### 8.1 Диаграмма переходов

```
┌─────────────┐
│    DRAFT    │  ← создан, требования публикации не выполнены
└──────┬──────┘
       │  [Требования выполнены: image + ≥1 active garment]
       │  (автоматический переход, вычисляется сервером)
       ▼
┌─────────────┐
│    READY    │  ← готов к публикации, ещё не виден на витрине
└──────┬──────┘
       │  [Admin нажимает «Опубликовать»]
       ▼
┌─────────────┐      [Admin нажимает «Снять»]     ┌────────────┐
│  PUBLISHED  │ ────────────────────────────────>  │    READY   │
└──────┬──────┘                                    └────────────┘
       │  [Admin нажимает «Архивировать»]
       ▼
┌─────────────┐
│  ARCHIVED   │  ← терминальное состояние (только «Восстановить» → DRAFT)
└─────────────┘
```

### 8.2 Правила каждого статуса

#### DRAFT

| Аспект | Правило |
|--------|---------|
| **Кто видит** | Только администратор в admin-панели |
| **Витрина** | Не отображается. API `/catalog/designs` возвращает только `status = PUBLISHED` |
| **Кто может изменять** | Любой администратор |
| **Переход из** | Начальный статус при создании |
| **Переход в** | READY (автоматически, когда сервер видит image + ≥1 active garment при любом обновлении) |
| **Обязательные проверки** | Нет |

#### READY

| Аспект | Правило |
|--------|---------|
| **Кто видит** | Только администратор |
| **Витрина** | Не отображается |
| **Кто может изменять** | Любой администратор |
| **Переход из** | DRAFT (авто) |
| **Переход в** | PUBLISHED (явный запрос admin) или DRAFT (если убрать image/деактивировать все variants — авто-пересчёт) |
| **Обязательные проверки** | Сервер проверяет: `mainImageUrl != null` AND `activeGarmentCount >= 1` AND у каждого active-варианта есть `≥1 цена (KZT)` |

#### PUBLISHED

| Аспект | Правило |
|--------|---------|
| **Кто видит** | Все пользователи, включая анонимных |
| **Витрина** | Отображается в каталоге. Если родительская коллекция или группа `active=false` → не отображается (каскадная логика) |
| **Кто может изменять** | Только администратор |
| **Переход из** | READY |
| **Переход в** | READY (снять) или ARCHIVED |
| **Обязательные проверки** | При автоматическом снятии (все варианты деактивированы) — сервер переводит в READY |
| **Запись** | `published_at = NOW()` при первой публикации (не перезаписывать при повторных) |

#### ARCHIVED

| Аспект | Правило |
|--------|---------|
| **Кто видит** | Только администратор, в отдельной вкладке «Архив» |
| **Витрина** | Не отображается |
| **Кто может изменять** | Только администратор |
| **Переход из** | PUBLISHED |
| **Переход в** | DRAFT (восстановить) — только явный admin-запрос |
| **Обязательные проверки** | При восстановлении: сбросить `archived_at = null` |
| **Запись** | `archived_at = NOW()` |

---

## 9. Правила публикации (publication guards)

Публикация (`status → PUBLISHED`) запрещена сервером, если:

| # | Условие | Проверка |
|---|---------|----------|
| 1 | Нет главного изображения | `design.mainImageUrl == null` |
| 2 | Нет активных вариантов | `design.garments.stream().noneMatch(g -> g.active)` |
| 3 | У хотя бы одного active-варианта нет цены в KZT | `garment.prices.stream().noneMatch(p -> p.currency == KZT)` |
| 4 | У хотя бы одного active-варианта нет ни одного цвета | `garment.colors.isEmpty()` |
| 5 | У хотя бы одного active-варианта нет ни одного размера | `garment.sizes.isEmpty()` |
| 6 | Суммарный остаток по всем ячейкам = 0 | `inventorySum == 0` (предупреждение, не блокировка — дизайн может быть pre-order) |

**Правило для pre-order:** Если инвентарь = 0, публикация разрешена, но admin видит **warning**, а на витрине кнопка «Добавить в корзину» заменяется на «Предзаказ» (реализуется в следующей фазе).

---

## 10. Backend Impact

### 10.1 Новые поля в entities

**Design.java**
```
+ private Integer sortOrder;
+ private LocalDateTime publishedAt;
+ private LocalDateTime archivedAt;
```

**DesignGarment.java**
```
+ private Integer sortOrder;
+ UNIQUE constraint: (design_id, garment_type)
```

**DesignGarmentPrice.java (опционально, не в MVP)**
```
+ private BigDecimal compareAtAmount;
```

### 10.2 Изменения в сервисах

**DesignService / DesignServiceImpl**

| Метод | Изменение |
|-------|-----------|
| `create()` | Уже ставит `DRAFT`. Добавить: `createdAt = NOW()` |
| `update()` | После изменения — вызвать `recalculateStatus()` для авто-перехода DRAFT↔READY |
| `publish(id)` | **ДОБАВИТЬ BACKEND VALIDATION** (guard checklist из раздела 9) |
| `archive(id)` | Добавить `archivedAt = NOW()` |
| `unpublish(id)` | Переводить в READY (не в DRAFT), если требования выполнены |
| `restore(id)` | ARCHIVED → DRAFT, сбросить `archivedAt` |
| `recalculateStatus(design)` | Новый private метод: вычисляет DRAFT или READY по текущим данным |
| `deactivateGarment(id)` | После деактивации — вызвать `recalculateStatus()` для дизайна |

**Метод `recalculateStatus` (логика):**
```
boolean hasImage = design.mainImageUrl != null
boolean hasActiveGarment = design.garments.any { it.active && it.prices.any { p -> p.currency == KZT } && it.colors.notEmpty && it.sizes.notEmpty }
return hasImage && hasActiveGarment ? READY : DRAFT
```
Вызывается только если текущий статус — DRAFT или READY (не PUBLISHED, не ARCHIVED).

### 10.3 Новые API Endpoints

| Method | Path | Описание |
|--------|------|----------|
| `PATCH` | `/admin/catalog/designs/{id}/publish` | Уже есть, добавить validation |
| `PATCH` | `/admin/catalog/designs/{id}/unpublish` | Новый — PUBLISHED → READY |
| `PATCH` | `/admin/catalog/designs/{id}/archive` | Уже есть, добавить `archivedAt` |
| `PATCH` | `/admin/catalog/designs/{id}/restore` | Новый — ARCHIVED → DRAFT |
| `PUT` | `/admin/catalog/designs/{id}/sort-order` | Новый — установить `sortOrder` |
| `GET` | `/api/v1/catalog/designs?status=PUBLISHED&sort=sortOrder` | Изменить — добавить параметр сортировки |

### 10.4 Новые Flyway миграции

**V26__design_publish_metadata.sql**
```sql
-- Добавить sortOrder, publishedAt, archivedAt к designs
ALTER TABLE designs
    ADD COLUMN sort_order   INTEGER,
    ADD COLUMN published_at TIMESTAMP,
    ADD COLUMN archived_at  TIMESTAMP;

-- Добавить sortOrder к design_garments
ALTER TABLE design_garments
    ADD COLUMN sort_order INTEGER;

-- Уникальность (design, garment_type)
ALTER TABLE design_garments
    ADD CONSTRAINT uq_design_garment_type UNIQUE (design_id, garment_type);

-- Индексы для сортировки и публикации
CREATE INDEX idx_designs_sort ON designs(collection_id, sort_order NULLS LAST);
CREATE INDEX idx_designs_published_at ON designs(published_at DESC) WHERE status = 'PUBLISHED';
```

**V27__color_hex_validation.sql**
```sql
-- Мягкая валидация: CHECK constraint (не сломает существующие записи без hex)
ALTER TABLE colors
    ADD CONSTRAINT chk_color_hex_format
    CHECK (hex_code IS NULL OR hex_code ~ '^#[0-9A-Fa-f]{6}$');
```

### 10.5 Изменения DTO

**DesignResponse (витрина)**
- Добавить: `sortOrder`, `publishedAt` (для отображения «Новинка»)
- Не добавлять: `archivedAt` (витрина не знает об архиве)

**AdminDesign (admin API)**
- Добавить: `sortOrder`, `publishedAt`, `archivedAt`
- Добавить: `publishReadiness` — объект с причинами, почему нельзя опубликовать (список строк)

**AdminGarment**
- Добавить: `sortOrder`

---

## 11. Frontend Impact

### 11.1 AdminDesignsPage.tsx

| Изменение | Описание |
|-----------|----------|
| `PublishGuidance` | Расширить проверки: `KZT цена`, `colors`, `sizes` (текущий компонент проверяет только image + garmentCount) |
| `StatusBadge` | Без изменений (DRAFT/READY/PUBLISHED/ARCHIVED уже реализованы) |
| Кнопка «Снять» | Добавить — сейчас есть только «Опубликовать» и «Архив», но нет «Снять с публикации» |
| Кнопка «Восстановить» | Добавить для ARCHIVED дизайнов |
| Сортировка | Drag-and-drop или поле `sortOrder` per design |
| Фильтр по статусу | Таб-переключатель: Все / Черновик / Готов / Опубликован / Архив |

### 11.2 AdminVariantsPage / Garment Editor

| Изменение | Описание |
|-----------|----------|
| Добавить `sortOrder` | Поле «Порядок» при создании/редактировании варианта |
| Предупреждение дубля | Если выбранный `garmentType` уже существует — показать ошибку до отправки |
| Валидация цен | Требовать KZT перед возможностью публикации (визуальный индикатор) |

### 11.3 DesignPage.tsx (витрина)

| Изменение | Описание |
|-----------|----------|
| Сортировка вариантов | `garments.sort((a, b) => (a.sortOrder ?? 999) - (b.sortOrder ?? 999))` |
| Новинка-badge | Если `publishedAt > 7 дней назад` → показать значок «Новинка» |
| Pre-order кнопка | Если `totalInventory === 0` → «Предзаказ» вместо «В корзину» (будущая фаза) |

### 11.4 CatalogPage (список дизайнов)

| Изменение | Описание |
|-----------|----------|
| Порядок | `order by sort_order ASC NULLS LAST, published_at DESC` |
| Фильтрация OOS | Уже реализовано (inventory filter) |

### 11.5 Каскадная видимость

Если `collection.active = false` или `catalogGroup.active = false` — дизайны не должны отображаться.

**Варианты реализации:**
- А) Frontend: при `isLoading=false` и пустом результате для коллекции — показать «Коллекция недоступна»
- Б) Backend: в `getCatalogDesigns` добавить JOIN на `collections.active = true AND catalog_groups.active = true`

**Рекомендация: вариант Б** — меньше трафика, правильная граница ответственности.

---

## 12. Admin Panel Impact

### 12.1 Создание дизайна (текущий workflow)

```
/admin/designs → форма → "Создать и перейти к вариантам" → /admin/designs/{id}/variants
```

Без изменений. Поле `sortOrder` добавить опционально (по умолчанию `NULL` = в конец).

### 12.2 Публикация (новый workflow после refactor)

```
AdminDesignsPage показывает:
  ┌─────────────────────────────────────────────────────┐
  │ Черновик (DRAFT)                                    │
  │ ❌ нет изображения                                  │
  │ ❌ нет активных вариантов с ценой KZT               │
  │ [Перейти к вариантам]                               │
  └─────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────┐
  │ ГОТОВ (READY)                                       │
  │ ✅ изображение есть                                 │
  │ ✅ 2 активных варианта с ценой                      │
  │ ⚠️ инвентарь = 0 (pre-order)                       │
  │ [Опубликовать] [Редактировать]                      │
  └─────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────┐
  │ ОПУБЛИКОВАН (PUBLISHED) — опубликован 19 июн        │
  │ ✅ видно на витрине                                 │
  │ [Снять с публикации] [Архивировать]                 │
  └─────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────┐
  │ АРХИВ (ARCHIVED) — архивирован 20 июн               │
  │ [Восстановить]                                      │
  └─────────────────────────────────────────────────────┘
```

### 12.3 Управление статусами (полная таблица доступных действий)

| Текущий статус | Доступные действия |
|----------------|-------------------|
| DRAFT | Редактировать, Перейти к вариантам |
| READY | Редактировать, Опубликовать, Перейти к вариантам |
| PUBLISHED | Редактировать, Снять с публикации, Архивировать |
| ARCHIVED | Восстановить (→ DRAFT) |

---

## 13. Migration Plan (пошаговый, без потери данных)

### Этап 1: Backend — Новые поля и валидация (V26, V27 миграции)

1. Написать `V26__design_publish_metadata.sql` — добавить поля, индексы, unique constraint
2. Обновить `Design.java` — добавить `sortOrder`, `publishedAt`, `archivedAt`
3. Обновить `DesignGarment.java` — добавить `sortOrder` + `@UniqueConstraint`
4. Обновить `DesignServiceImpl.publish()` — добавить server-side validation guard
5. Добавить `unpublish()` и `restore()` методы в сервис
6. Добавить `recalculateStatus()` вызов в `update()` и `deactivateGarment()`
7. Добавить новые endpoint handler-ы в `DesignController`
8. Добавить новые функции в `DesignApi` interface
9. Обновить `DesignResponse` и `AdminDesign` DTO — новые поля
10. Написать/обновить тесты:
    - `DesignPublicationIntegrationTest` — проверить все publish guards
    - `DesignStatusTransitionTest` — машина состояний
    - Обновить существующие тесты, если они проверяют статусы
11. Запустить все тесты — должно быть `GREEN`

### Этап 2: Frontend — Admin UI

1. Добавить кнопку «Снять с публикации» в `AdminDesignsPage` → вызов нового `unpublish` API
2. Добавить кнопку «Восстановить» для ARCHIVED дизайнов → вызов `restore` API
3. Расширить `PublishGuidance` — добавить проверки KZT, colors, sizes
4. Добавить вкладки фильтрации по статусу (All / DRAFT / READY / PUBLISHED / ARCHIVED)
5. Добавить поле `sortOrder` в форму создания/редактирования дизайна
6. Добавить `sortOrder` в форму создания/редактирования варианта
7. Добавить дату публикации / архивации в таблицу списка дизайнов

### Этап 3: Frontend — Витрина

1. Добавить сортировку вариантов по `sortOrder` в `DesignPage`
2. Добавить «Новинка»-badge (если `publishedAt < 7 days`)
3. Добавить каскадную проверку видимости коллекции в catalog API

### Этап 4: Backend — Каскадная видимость

1. Обновить `CatalogController.getDesigns()` — добавить JOIN check для `collection.active` и `group.active`
2. Добавить интеграционный тест: скрытая коллекция → дизайны не возвращаются

### Этап 5: Product legacy (низкий приоритет)

1. Убрать кнопку «Создать товар» из admin (оставить только просмотр legacy заказов)
2. Добавить пометку «Legacy» к product-based OrderItem в OrderHistory
3. Добавить `@Deprecated` аннотацию к `ProductController`, `ProductService`
4. НЕ удалять таблицу `products` и FK из `order_items.product_id`

---

## 14. Risk Analysis

### 14.1 Риски изменений

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| V26 миграция ломает UNIQUE constraint для существующих дублей `(design_id, garment_type)` | Средняя | HIGH | Перед миграцией: `SELECT design_id, garment_type, COUNT(*) FROM design_garments GROUP BY design_id, garment_type HAVING COUNT(*) > 1` — если есть, удалить дубли вручную или переименовать |
| `recalculateStatus()` переводит PUBLISHED дизайн в READY/DRAFT | Низкая | CRITICAL | Метод ЯВНО пропускает PUBLISHED и ARCHIVED — только DRAFT↔READY автоматически |
| Frontend «Снять с публикации» → дизайн исчезает из активных корзин | Низкая | MEDIUM | Дизайны в корзине хранятся по `designGarmentId` — покупатель сможет оформить заказ, но на витрине дизайн не отображается |
| Тест дублей garmentType ломает существующие тесты | Средняя | LOW | Пересмотреть seed данные в тестовых классах |
| `published_at` не заполнен для уже опубликованных дизайнов | Высокая | LOW | Backfill в V26: `UPDATE designs SET published_at = created_at WHERE status = 'PUBLISHED' AND published_at IS NULL` |

### 14.2 Данные под угрозой

**Возможные потери данных:**
1. Если в БД есть `design_garments` с дублями по `(design_id, garment_type)` — V26 упадёт. Нужен pre-check + ручная очистка перед деплоем.
2. Если `Color.hexCode` в БД не в формате `#RRGGBB` — V27 (CHECK constraint) упадёт. Pre-check: `SELECT * FROM colors WHERE hex_code !~ '^#[0-9A-Fa-f]{6}$'`

**Не затронуты:**
- Все существующие заказы (order_items)
- Все платежи
- Вся инвентаризация
- Все коллекции и группы

### 14.3 Rollback стратегия

| Этап | Rollback |
|------|---------|
| V26 (если упало до UNIQUE) | `ALTER TABLE designs DROP COLUMN sort_order, published_at, archived_at; ALTER TABLE design_garments DROP COLUMN sort_order;` |
| V26 (если упало на UNIQUE) | Удалить дубли → повторить миграцию |
| V27 | `ALTER TABLE colors DROP CONSTRAINT chk_color_hex_format;` |
| Backend код | Git revert на коммит до изменений |
| Frontend | Git revert на коммит до изменений |

### 14.4 Что тестировать перед release

- [ ] `GET /api/v1/catalog/designs` возвращает только PUBLISHED
- [ ] `PATCH /publish` с design без image → HTTP 422
- [ ] `PATCH /publish` с design без вариантов → HTTP 422
- [ ] `PATCH /publish` с design без KZT цены → HTTP 422
- [ ] `PATCH /archive` → status = ARCHIVED, archivedAt заполнен
- [ ] `PATCH /restore` → status = DRAFT, archivedAt = null
- [ ] Деактивация последнего варианта PUBLISHED дизайна → автоперевод в READY (если это требуется)
- [ ] Duplicate garmentType для одного design → HTTP 409
- [ ] Дизайн в скрытой коллекции не виден на витрине
- [ ] sortOrder влияет на порядок в ответе API

---

## 15. Объём работ (оценка)

| Этап | Backend | Frontend | Тесты | Итого |
|------|---------|----------|-------|-------|
| Этап 1 (backend validation) | 8 ч | — | 4 ч | 12 ч |
| Этап 2 (admin UI) | — | 5 ч | — | 5 ч |
| Этап 3 (витрина) | — | 2 ч | — | 2 ч |
| Этап 4 (cascade visibility) | 3 ч | 1 ч | 2 ч | 6 ч |
| Этап 5 (product legacy) | 1 ч | 2 ч | — | 3 ч |
| **Итого** | **12 ч** | **10 ч** | **6 ч** | **28 ч** |

---

## 16. Что НЕ включено в данный план (вне scope)

- Scheduled publishing (запланированная дата публикации) — следующая фаза
- Pre-order flow (кнопка «Предзаказ» при нулевом инвентаре) — следующая фаза
- Design tags / фильтры по тегам — следующая фаза
- `compare_at_amount` / скидки — следующая фаза
- Перенос legacy `Product` данных в `Design` — нет данных для миграции, откладывается
- Удаление таблицы `products` — запрещено (FK из order_items)
