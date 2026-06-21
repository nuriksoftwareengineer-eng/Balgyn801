# DATABASE_REVIEW.md
> Balgyn — Pre-Launch Database Review · 2026-06-21

---

## 1. Overview

| Item | Detail |
|------|--------|
| Engine | PostgreSQL 16 |
| Migration tool | Flyway |
| Total tables | 30 |
| Total migrations | V1–V26 (26 files) |
| DDL strategy | `ddl-auto=validate` — Flyway owns schema |
| Baseline | `baseline-on-migrate=true`, `baseline-version=0` |

---

## 2. Schema Overview

```
USERS / AUTH
  users (id, email UNIQUE, password_hash, created_at)
  user_roles (user_id → users, role)
  user_addresses (id, user_id → users, ...)

CATALOG
  catalog_groups (id, name, slug UNIQUE, sort_order, active)
  collections (id, catalog_group_id → catalog_groups, slug UNIQUE, cover_image_url, banner_image_url, description, sort_order, active)
  designs (id, collection_id → collections, name, slug UNIQUE, status [DRAFT|READY|PUBLISHED|ARCHIVED], sort_order, main_image_url, gallery JSONB, published_at, archived_at, created_at)
  design_garments (id, design_id → designs, garment_type, active, sort_order, UNIQUE(design_id, garment_type))
  design_garment_prices (id, design_garment_id → design_garments, currency, price)
  design_garment_colors (design_garment_id, color_id)
  design_garment_sizes (design_garment_id, size_id)
  colors (id, name, hex_code)
  sizes (id, label)
  size_chart_images (id, garment_type, image_url)

INVENTORY
  inventory (id, design_garment_id → design_garments, color_id → colors, size_id → sizes, quantity, UNIQUE(design_garment_id, color_id, size_id))

ORDERS
  customers (id, name, phone, telegram_user_name, create_at)
  orders (id, customer_id → customers, user_id → users nullable, status, total_price, delivery_fee, delivery_type, comment, created_at, updated_at, ...)
  order_items (id, order_id → orders, product_id nullable, design_garment_id nullable, color_id nullable, size_id nullable, quantity, unit_price, size_label, color_name, currency)
  order_history (id, order_id → orders, status, date_added)
  delivery_addresses (id, order_id → orders UNIQUE, city, street, apartment, postal_code, recipient_name, recipient_phone, ...)
  garment_type_weights (id, garment_type, weight_kg)
  countries (id, name, iso2, shipping_zone)
  delivery_settings (id, flat_kzt, ...)
  delivery_tariffs (id, kind, upto_kg, price_kzt, UNIQUE(kind, upto_kg))

CDEK
  cdek_shipments (id, order_id → orders UNIQUE, cdek_order_uuid, status, ...)

PAYMENTS
  payments (id, order_id → orders, provider, status, amount, currency, provider_payment_id, payment_url, webhook_event_id, last_webhook_payload, created_at, updated_at)
  processed_webhook_events (id, payment_id → payments, provider, event_id, processed_at, UNIQUE(provider, event_id))

EXCHANGE RATES
  exchange_rates (id, currency_pair, rate, updated_at)

CUSTOM DESIGN
  custom_designs (id, customer_id → customers, description, reference_image_url, status, created_at)

REVIEWS
  reviews (id, design_id → designs, user_id → users, rating CHECK(1..5), comment, created_at, UNIQUE(design_id, user_id))

LEGACY (kept, not migrated)
  products (id, title, description, price, image_url, in_stock, category, sizes JSONB, colors JSONB, created_at)
```

---

## 3. Migration Audit

| Migration | Risk | Key Notes |
|-----------|------|-----------|
| V1 — Baseline | Safe | All `CREATE TABLE IF NOT EXISTS`; baseline snapshot |
| V2 — Catalog groups | Safe | Clean DDL |
| V3 — Designs | Safe | Slug UNIQUE constraint ✓ |
| V4 — Colors/sizes | Safe | — |
| V5 — Design garments | Safe | — |
| V6 — Inventory | Safe | UNIQUE(garment, color, size) ✓ |
| V7 — Order items design link | **MEDIUM** | `ON DELETE SET NULL` on `color_id`, `size_id`, `design_garment_id` — deleting catalog data nullifies historical order items silently; no cascade warning |
| V8 — User addresses | Safe | — |
| V9 — Orders user link | Safe | Nullable FK ✓ |
| V10 — Reviews | Safe | UNIQUE(design, user) ✓; CHECK (1..5) ✓ |
| V11 — Rating type fix | Safe | SMALLINT → INTEGER; check constraint preserved ✓ |
| V12 — Garment weights | Safe | — |
| V13 — Countries | Safe | — |
| V14 — Order delivery snapshot | Safe | All nullable; 2 indexes ✓ |
| V15 — Delivery settings | Safe | Bootstrap rows with ON CONFLICT ✓ |
| V16 — Delivery tariffs | Safe | — |
| V17 — Payment security | Safe | Partial UNIQUE index (PENDING) ✓; idempotency table ✓ |
| V18 — CDEK shipment | Safe | All nullable ✓ |
| V19 — Size chart images | Safe | — |
| V20 — Collection media + design gallery | Safe | Gallery NOT NULL DEFAULT '[]' ✓ |
| V21 — Freedom Pay migration | **MEDIUM** | Hard `DELETE` of payment records (assumed empty); irreversible; no guard |
| V22 — Schema version marker | Safe | No-op `SELECT 1` — unusual practice |
| V23 — EUR/RUB rates | Safe | `ON CONFLICT DO NOTHING` ✓ |
| V24 — Performance indexes | Safe | 8 indexes + 2 unique constraints; all `IF NOT EXISTS` ✓ |
| V25 — Design status enum | **MEDIUM** | Drops `active` column (irreversible); backfill correct ✓; `NOT NULL` after backfill ✓ |
| V26 — Design hardening | Low-Medium | Pre-flight duplicate check ✓; UNIQUE(design_id, garment_type) ✓; `published_at` backfilled from `created_at` ✓ |

---

## 4. Index Coverage

### Existing indexes (V1–V26)

| Table | Index / Constraint | Added in |
|-------|--------------------|----------|
| `users` | UNIQUE email | V1 |
| `catalog_groups` | UNIQUE slug, `idx_catalog_groups_sort` | V2 |
| `collections` | UNIQUE slug, `idx_collections_catalog_group_id` | V2 |
| `designs` | UNIQUE slug, `idx_designs_collection_id`, `idx_designs_sort`, `idx_designs_published_at` | V3, V26 |
| `design_garments` | UNIQUE(design_id, garment_type) | V26 |
| `inventory` | UNIQUE(design_garment_id, color_id, size_id) | V6 |
| `orders` | `idx_orders_user_id`, `idx_orders_status`, `idx_orders_created_at` | V9, V14 |
| `order_items` | `idx_order_items_order_id` | V24 |
| `order_history` | `idx_order_history_order_id` | V24 |
| `payments` | `idx_payments_order_id`, UNIQUE(order_id, provider) WHERE status='PENDING', UNIQUE(order_id, provider) WHERE status='SUCCEEDED' | V1, V17, V24 |
| `processed_webhook_events` | UNIQUE(provider, event_id), `idx_processed_webhook_events_payment_id` | V17, V24 |
| `delivery_addresses` | UNIQUE order_id | V24 |
| `cdek_shipments` | UNIQUE order_id | V24 |
| `delivery_tariffs` | UNIQUE(kind, upto_kg) | V24 |
| `user_roles` | `idx_user_roles_user_id` | V24 |
| `custom_designs` | `idx_custom_designs_customer_id` | V24 |

### Missing indexes (gaps)

| Table | Missing Index | Impact |
|-------|--------------|--------|
| `cdek_shipments` | `cdek_order_uuid` | CRITICAL: `CdekWebhookServiceImpl.findAll()` scans full table on every webhook |
| `orders` | No composite index for admin list query | MEDIUM: full-status-filter scan grows with order volume |
| `customers` | No index on `phone` or `name` | LOW: customer lookup by phone is a full scan |
| `design_garment_prices` | No index on `design_garment_id` | LOW: price lookup per garment scans all prices |

---

## 5. Constraint Gaps

| Table | Missing Constraint | Risk |
|-------|--------------------|------|
| `inventory.quantity` | No `CHECK (quantity >= 0)` | Negative inventory possible under edge-case concurrency |
| `orders.status` | No `CHECK` for valid enum values | Invalid status strings can be stored |
| `orders.delivery_type` | No `CHECK` for valid enum values | Invalid delivery type can be stored |
| `order_items` | No `CHECK (product_id IS NOT NULL OR design_garment_id IS NOT NULL)` | Orphaned order item with no product reference possible |
| `orders.created_at` | `nullable` in entity despite always being set | Could store NULL created_at |

---

## 6. Data Integrity Issues

### DB-01 — V7 ON DELETE SET NULL silently corrupts order history
`order_items.design_garment_id`, `.color_id`, `.size_id` all have `ON DELETE SET NULL`. If an admin deletes a `DesignGarment`, `Color`, or `Size` from the catalog, all order items referencing it silently lose their reference — making historical orders un-reconstructable. No warning is given in the admin UI before deletion.

**Recommendation:** Change to `ON DELETE RESTRICT` to prevent deletion of catalog items that have been ordered, or implement soft-delete.

### DB-02 — Customer deduplication missing
A new `Customer` row is created for every order, even for the same phone/name combination. Returning customers with the same phone will accumulate duplicate customer records. The `customers` table has no unique constraint on phone or email.

### DB-03 — No DB-level enum enforcement
`orders.status` stores `VARCHAR(255)`. There is no `CHECK` constraint limiting values to valid enum members. If Hibernate or a direct SQL insert uses an invalid status string, the row is stored without error and breaks the state machine.

**Recommendation:** Use a PostgreSQL `ENUM` type or add CHECK constraints matching the Java enum values.

---

## 7. Migration Risks

### DB-04 — V21 irreversible DELETE
V21 deletes all `YOOKASSA` and `PAYPAL` payment records and processed webhook events. The assumption is these tables were empty (provider was switched before any real payments). This is correct if verified, but there is no guard (e.g., `DO $$ IF count > 0 THEN RAISE EXCEPTION...$$`). If there were any real payments under these providers, they are permanently lost.

### DB-05 — V25 DROP COLUMN
V25 drops `designs.active` after migrating to `status`. This is irreversible without a new migration. The backfill logic is correct, but if run against a production database where `active=false` designs should have been `ARCHIVED`, they would be set to `DRAFT` (the migration sets `DRAFT` for all non-active designs). Verify the intended state before running.

---

## 8. Performance Observations

### DB-06 — Unbounded admin list queries
`OrderRepository.findByStatusNotInOrderByCreatedAtDesc()` and `DesignRepository.findAllWithGarments()` return unbounded `List<>`. As the business grows, these will cause increasing memory pressure and response latency.

**Recommendation:** Add `Pageable` support to these queries and update admin endpoints to accept `page` + `size` parameters.

### DB-07 — N+1 on order list
The admin order list (`getAll()`) triggers N+1 lazy loads: each `Order` lazily loads `customer`, `deliveryAddress`, and `orderItems`. With 1000 orders, this is ~3000 extra queries.

**Recommendation:** Add `@EntityGraph(attributePaths = {"customer", "deliveryAddress", "orderItems"})` to the order list query.

### DB-08 — N inventory queries on design page
`getDesignBySlug()` queries inventory in a stream loop: one `SELECT` per garment. With 3 garments: 4 queries total. Add a batch `findByDesignGarment_IdIn(List<Long> ids)` query.

---

## 9. Positive Patterns

- `ddl-auto=validate` — Flyway owns schema; Hibernate only validates ✓
- `baseline-on-migrate=true` — safe for existing databases ✓
- `processed_webhook_events` idempotency table with unique constraint ✓
- Partial UNIQUE index for PENDING and SUCCEEDED payments per order+provider ✓
- `inventory` pessimistic lock with 3000ms timeout ✓
- Performance index V24 covers all hot FK columns ✓
- Pre-flight duplicate check in V26 (`DO $$ ... RAISE EXCEPTION ... $$`) ✓
- `delivery_tariffs` UNIQUE constraint prevents non-deterministic tariff lookup ✓
