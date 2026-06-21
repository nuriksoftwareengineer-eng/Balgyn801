# CATALOG_CONTENT_STATUS

**Дата анализа:** 2026-06-21  
**Источник данных:** `seed_catalog.sql` + Flyway миграции V1–V28

---

## Категории (catalog_groups) — 5

| # | Название | Slug | Порядок |
|---|----------|------|---------|
| 1 | Аниме    | `anime`  | 1 |
| 2 | Игры     | `games`  | 2 |
| 3 | Музыка   | `music`  | 3 |
| 4 | Спорт    | `sport`  | 4 |
| 5 | Кино     | `movies` | 5 |

---

## Коллекции (collections) — 14

| Категория | Коллекция       | Slug              |
|-----------|-----------------|-------------------|
| Аниме     | Берсерк         | `berserk`         |
| Аниме     | Наруто          | `naruto`          |
| Аниме     | Атака Титанов   | `attack-on-titan` |
| Игры      | CS2             | `cs2`             |
| Игры      | Dota 2          | `dota-2`          |
| Игры      | Valorant        | `valorant`        |
| Игры      | GTA             | `gta`             |
| Игры      | Minecraft       | `minecraft`       |
| Музыка    | Metallica       | `metallica`       |
| Музыка    | Linkin Park     | `linkin-park`     |
| Спорт     | ММА             | `mma`             |
| Спорт     | Футбол          | `football`        |
| Кино      | Marvel          | `marvel`          |
| Кино      | Star Wars       | `star-wars`       |

---

## Дизайны (designs) — 2

| Дизайн               | Коллекция | Статус    | Фото | Одежда                          |
|----------------------|-----------|-----------|------|---------------------------------|
| Brand of Sacrifice   | Берсерк   | PUBLISHED | ❌ нет | Hoodie 12 000₸ / T-Shirt 8 500₸ / Sweatshirt 10 500₸ |
| Counter-Strike       | CS2       | PUBLISHED | ❌ нет | Hoodie 11 000₸ / T-Shirt 7 500₸ |

- **Опубликовано:** 2  
- **Черновики (DRAFT):** 0  
- **Архив (ARCHIVED):** 0

---

## Цвета (colors) — 4

| Название | Hex      |
|----------|----------|
| Black    | #000000  |
| White    | #FFFFFF  |
| Navy     | #1B2A4A  |
| Red      | #C0392B  |

---

## Размеры (sizes) — 5

S · M · L · XL · XXL

---

## Важные замечания

### ⚠ seed_catalog.sql несовместим с текущей схемой

Файл `seed_catalog.sql` содержит INSERT в таблицу `designs` с колонкой `active`:

```sql
INSERT INTO designs (collection_id, name, slug, description, main_image_url, active, created_at)
```

Миграция **V25** удалила колонку `active` из таблицы `designs` и заменила её на `status VARCHAR(20)`.  
Запуск `seed_catalog.sql` на актуальной базе вызовет ошибку.

**Исправление перед запуском сида:**
```sql
-- Заменить в seed_catalog.sql:
INSERT INTO designs (collection_id, name, slug, description, main_image_url, status, created_at)
SELECT c.id, 'Brand of Sacrifice', 'brand-of-sacrifice', '...', NULL, 'PUBLISHED', NOW()
```

### ⚠ Оба дизайна не имеют изображений

`main_image_url = NULL` для обоих дизайнов. Без изображений карточки дизайнов в каталоге покажут заглушку.

### ⚠ Каталог не наполнен через UI

Текущий каталог — только сид. Реальные данные нужно вводить через:  
`/admin/catalog` → Группы → Коллекции → Дизайны → Варианты → Фото.
