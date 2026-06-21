# Publication Rules — Design Lifecycle

**Дата:** 2026-06-21  
**Версия:** 1.0

---

## Обязательные условия публикации

Дизайн можно перевести в статус `PUBLISHED` только если **все** условия ниже выполнены.

| # | Правило | Код ошибки | HTTP |
|---|---------|-----------|------|
| 1 | Есть главное изображение (`main_image_url != null`, не пустая строка) | `main_image_missing` | 400 |
| 2 | Есть хотя бы один **активный** вариант (`DesignGarment.active = true`) | `no_active_garments` | 400 |
| 3 | Хотя бы один активный вариант имеет цену в KZT, хотя бы один цвет и хотя бы один размер | `no_variant_with_kzt_price_size_color` | 400 |

### Предупреждение (не блокировка)

| # | Условие | Поведение |
|---|---------|-----------|
| W1 | Суммарный инвентарь всех активных вариантов = 0 | Публикация разрешена; на витрине кнопка «В корзину» недоступна; товар доступен для pre-order (будущая фаза) |

---

## Где проверяется каждое правило

### Правило 1 — Главное изображение

| Слой | Реализация | Файл |
|------|-----------|------|
| **Database** | `main_image_url VARCHAR(512)` — nullable, нет DB-constraint | `V3__designs.sql` |
| **Backend** | `DesignReadinessService.validationErrors()`: `design.getMainImageUrl() == null \|\| isBlank()` → добавляет `"main_image_missing"` в errors. `DesignServiceImpl.publish()` вызывает этот метод и бросает `PublicationValidationException` | `DesignReadinessService.java:51-53` |
| **Frontend (admin)** | `PublishGuidance` компонент: `if (!d.mainImageUrl) issues.push("нет главного изображения")` | `AdminDesignsPage.tsx:48` |
| **Frontend (storefront)** | Не проверяется — ответственность на backend | — |

### Правило 2 — Хотя бы один активный вариант

| Слой | Реализация | Файл |
|------|-----------|------|
| **Database** | `design_garments.active BOOLEAN NOT NULL DEFAULT TRUE` | `V5__design_garments.sql` |
| **Backend** | `DesignReadinessService.validationErrors()`: `garmentRepository.findByDesign_IdAndActiveTrue(id)` → если пусто → `"no_active_garments"` | `DesignReadinessService.java:55-57` |
| **Frontend (admin)** | `PublishGuidance`: `if (d.activeGarmentCount === 0) issues.push("нет активных вариантов")` | `AdminDesignsPage.tsx:49` |

### Правило 3 — Цена KZT + хотя бы один цвет + хотя бы один размер

| Слой | Реализация | Файл |
|------|-----------|------|
| **Database** | `UNIQUE (design_garment_id, currency)` в `design_garment_prices`; nullable по умолчанию — DB не блокирует отсутствие KZT строки | `V5__design_garments.sql` |
| **Backend** | `DesignReadinessService.validationErrors()`: `active.stream().anyMatch(g -> hasKztPrice(g) && !g.getSizes().isEmpty() && !g.getColors().isEmpty())` → если нет ни одного → `"no_variant_with_kzt_price_size_color"` | `DesignReadinessService.java` |
| **Frontend (admin)** | `PublishGuidance`: **[TODO в V26]** расширить проверки — сейчас не проверяет KZT отдельно | `AdminDesignsPage.tsx:45-55` |

---

## Переходы статусов

```
DRAFT  ──[auto: image+garment добавлены]──▶  READY
READY  ──[admin: Опубликовать, guard pass]──▶  PUBLISHED
PUBLISHED  ──[admin: Снять]──────────────────▶  READY   (если требования выполнены)
PUBLISHED  ──[admin: Архивировать]───────────▶  ARCHIVED
ARCHIVED   ──[admin: Восстановить]───────────▶  DRAFT
```

### Автоматические переходы (DRAFT ↔ READY)

Метод `DesignReadinessService.recompute(designId)` вызывается:
- После `DesignServiceImpl.update()` — admin изменил image/metadata
- После `DesignGarmentServiceImpl.deactivateGarment()` — вариант деактивирован

Метод **никогда** не трогает `PUBLISHED` и `ARCHIVED` автоматически.

---

## Поля записи событий

| Событие | Поле | Кто записывает |
|---------|------|---------------|
| Первая публикация | `published_at = NOW()` (не перезаписывается при повторах) | `DesignServiceImpl.publish()` |
| Архивация | `archived_at = NOW()` | `DesignServiceImpl.archive()` |
| Восстановление из архива | `archived_at = NULL` | `DesignServiceImpl.restore()` |

---

## Формат ответа при нарушении правил публикации

HTTP 400 с телом:

```json
{
  "type": "about:blank",
  "title": "Design Not Ready",
  "status": 400,
  "detail": "Design cannot be published",
  "code": "DESIGN_NOT_READY",
  "errors": ["main_image_missing", "no_active_garments"]
}
```

Обработчик: `RestExceptionHandler.handlePublicationValidation()` → `PublicationValidationException`

---

## Каскадная видимость

Дизайн со статусом `PUBLISHED` **не отображается на витрине**, если:
- `designs.collection.active = false` — коллекция скрыта
- `designs.collection.catalogGroup.active = false` — группа скрыта

**Текущая реализация:** проверяется на уровне JOIN в `CatalogController`  
**[TODO V27+]:** добавить JOIN-фильтр в `getCatalogDesigns()` и `getCatalogDesign()` по single slug

---

## Unique constraint на вариантах

С миграции **V26** действует:

```sql
UNIQUE (design_id, garment_type)  -- в таблице design_garments
```

При попытке создать второй HOODIE для того же дизайна — DB выбросит `ConstraintViolationException`, которая обрабатывается как HTTP 409 (конфликт) в `RestExceptionHandler`.

---

## Checklist перед деплоем V26

```sql
-- 1. Проверить дубли (должно вернуть 0 строк)
SELECT design_id, garment_type, COUNT(*) cnt
FROM design_garments
GROUP BY design_id, garment_type
HAVING COUNT(*) > 1;

-- 2. Проверить невалидные hex (должно вернуть 0 строк)
SELECT * FROM colors WHERE hex_code IS NOT NULL AND hex_code !~ '^#[0-9A-Fa-f]{6}$';
```
