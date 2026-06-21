# Production Readiness Audit
**Date:** 2026-06-20  
**Scope:** Phase 5 (I18N / Header UX) + Phase 5.2 (Catalog Admin UX)  
**Method:** Static code analysis — no code was modified during this audit

---

## Severity Scale

| Level | Meaning |
|-------|---------|
| **CRITICAL** | Blocks production launch. Active bug or data exposure. |
| **MAJOR** | Must fix before launch. Significant user-facing or security gap. |
| **MINOR** | Fix soon. Low-impact polish or technical debt. |
| **PASS** | Verified working correctly. |

---

## Finding Index

| # | Area | Severity | Summary |
|---|------|----------|---------|
| F-01 | I18N | **CRITICAL** | ~85% of customer-facing strings are hardcoded Russian |
| F-02 | I18N | **MAJOR** | SiteNavbar aria-labels and cart suffix hardcoded Russian |
| F-03 | Publish flow | **MAJOR** | Backend `setActive()` has zero server-side validation |
| F-04 | Publish flow | **MAJOR** | Publish guard missing: price, sizes, colors, stock |
| F-05 | Domain model | **MAJOR** | `Design.active` Boolean cannot represent DRAFT/READY/PUBLISHED cleanly |
| F-06 | Security | **MAJOR** | `DesignController` has no `@PreAuthorize` (no defense-in-depth) |
| F-07 | Performance | **MINOR** | Potential N+1 in `getCollectionBySlug` public endpoint |
| F-08 | Draft access | **PASS** | Unpublished designs return 404 on all public endpoints |
| F-09 | Authorization | **PASS** | Admin publish endpoint is URL-guarded at security config |
| F-10 | Admin I18N | **MINOR** | Admin panel 100% hardcoded Russian (intentional, internal tool) |

---

## F-01 — Customer-Facing Hardcoded Russian Strings
**Severity: CRITICAL**

The language switcher (`🌐 RU ▼`) was implemented and works mechanically. However, an estimated 85% of all visible customer-facing text is hardcoded Russian and does not respond to language selection.

### Most impacted files

**`frontend/src/pages/CartPage.tsx`** (entire purchase funnel)
```
Line 36–40:   DELIVERY_LABELS = { PICKUP: "Самовывоз", TAXI: "Такси / курьер", CDEK: "СДЭК", ... }
Line 48–50:   DELIVERY_REGIONS labels/hints: "Казахстан", "РФ / СНГ", "Другие страны"
Lines 267–430: ALL step-content labels, field placeholders, error messages
Lines 471–513: Payment option descriptions ("Карты Казахстана", "Международные карты")
Lines 1729–1898: Cart review screen ("Корзина", "Оформить заказ", "Пока пусто", plural forms)
```

**`frontend/src/pages/DesignPage.tsx`** (core product page)
```
Line 165: "Дизайн не найден"
Line 174: "← Назад"
Line 246: "📏 Размерная сетка"
Line 300: "Нет доступных вариантов"
Lines 307–417: "Изделие", "Цвет", "Размер", "В корзину", "Выберите цвет", "Выберите размер"
```

**`frontend/src/widgets/AnnouncementBar.tsx`**
```
Line 5: "Бесплатная доставка от суммы заказа · Новые дропы каждую неделю"
```

**`frontend/src/widgets/ProductCard.tsx`**
```
Lines 43–65: "В наличии", "Нет в наличии", "Выбрать размер", "В корзину"
```

**`frontend/src/widgets/TrustStrip.tsx`**
```
All 4 feature labels and subtitles: "Доставка", "По Казахстану и за рубеж", etc.
```

**`frontend/src/widgets/SiteFooter.tsx`**
```
All headings, links, and copyright text: "Магазин", "Каталог", "Свой дизайн", etc.
```

**`frontend/src/widgets/home/Hero.tsx`**
```
Line 38: "Доставка по всему миру!"
Line 56: "Приносим свежесть в твой стиль."
Line 68: "Смотреть коллекцию"
```

**`frontend/src/widgets/home/ValueStrip.tsx`, `AboutBand.tsx`, `FeaturedCollection.tsx`**
```
All body content, CTAs, empty states hardcoded Russian
```

**`frontend/src/shared/ui/page-load-fallback.tsx`**
```
Line 13: "Загрузка…" — shown to all users during lazy route loading
```

**`frontend/src/shared/types/catalog.ts`**
```
Lines 106–111: garment type display names ("Футболка", "Худи", "Свитшот"…) — rendered in DesignPage
```

**`frontend/src/shared/api/http.ts`**
```
Line 88: "Запрос завершился с кодом ${response.status}" — raw error shown to users
```

### Impact
A user who selects `EN` or `KK` will see: English nav links and page headings (translated), but Russian product names, checkout labels, delivery options, payment labels, footer, and error messages. The language switcher is cosmetically functional but practically ineffective.

### Fix required
All strings listed above must be moved into `translation.json` and replaced with `t()` calls. CartPage and DesignPage are the highest priority (purchase funnel).

---

## F-02 — SiteNavbar Hardcoded aria-labels and Cart Suffix
**Severity: MAJOR**

The live header component (`src/widgets/SiteNavbar.tsx`) contains hardcoded Russian in accessibility-critical attributes:

```tsx
// Line 114 — cart button aria-label mixes t() and hardcoded Russian
aria-label={`${t("nav.cart")}, ${totalQty} поз.`}  // "поз." = "positions"

// Line 141 — hamburger button
aria-label="Открыть меню"

// Line 169 — mobile close button
aria-label="Закрыть меню"
```

These fail WCAG 2.1 for EN/KK users: screen readers will announce Russian text in non-Russian locale.

Note: `SiteHeader.tsx` already has correct `t("header.openMenu")` / `t("header.closeMenu")` — but `SiteHeader.tsx` is NOT mounted anywhere. `MainLayout.tsx` uses `SiteNavbar.tsx` exclusively.

### Fix required
Replace the three hardcoded strings in `SiteNavbar.tsx` with the translation keys already defined in all three locale files.

---

## F-03 — Backend `setActive()` Has Zero Server-Side Validation
**Severity: MAJOR**

The publish endpoint performs no business rule validation:

**`src/main/java/com/nurba/java/service/Impl/DesignServiceImpl.java:89–93`**
```java
@Transactional
public DesignResponse setActive(Long id, boolean active) {
    Design entity = findOrThrow(id);
    entity.setActive(active);              // unconditional
    return mapper.toResponse(repository.save(entity));
}
```

Any authenticated admin can `PATCH /api/v1/admin/catalog/designs/{id}/active` with `{"active": true}` and publish a design with no image, no garments, no price — bypassing all frontend validation. The frontend guards (`handleToggleActive`) are the **only** validation layer and can be trivially skipped with curl.

### Fix required
Add server-side pre-publish validation in `setActive()` when `active = true`:
- At least one `DesignGarment` with `active = true`
- At least one `DesignGarmentPrice` per active garment
- `design.mainImageUrl` not null

---

## F-04 — Publish Guard Missing: Price, Sizes, Colors, Stock
**Severity: MAJOR**

The frontend's `handleToggleActive` function in `AdminDesignsPage.tsx` validates:

```typescript
if (!d.mainImageUrl) issues.push("нет главного изображения");   // ✓ checked
if (d.activeGarmentCount === 0) issues.push("нет активных вариантов");  // ✓ checked
```

The following are **NOT validated** before publish:

| Check | Validated? | Risk if missing |
|-------|-----------|-----------------|
| `mainImageUrl` present | ✓ Yes | — |
| At least 1 active garment | ✓ Yes | — |
| At least 1 price set per garment | ✗ No | Customer sees ₸0 price, can order at zero cost |
| At least 1 size assigned | ✗ No | Size selector shows empty, add-to-cart fails silently |
| At least 1 color assigned | ✗ No | Color selector shows empty, design unusable |
| Stock > 0 for at least one cell | ✗ No | All sizes show "out of stock" on published design |

The `getStatus()` badge (DRAFT/ГОТОВ/PUBLISHED) also does not check price/size/color — so ГОТОВ status gives false confidence to admin.

### Fix required
Extend the frontend pre-publish guard and the backend `setActive()` validation to include price ≠ null/0, at least one size, and at least one color per active garment.

---

## F-05 — `Design.active` Boolean Cannot Represent 3 States
**Severity: MAJOR**

**`src/main/java/com/nurba/java/domain/Design.java`**
```java
@Column(nullable = false)
private Boolean active = false;
```

The frontend derives a 3-state status from 2 fields:
```typescript
function getStatus(d: AdminDesign): DesignStatus {
  if (d.active) return "published";
  if (d.mainImageUrl && d.activeGarmentCount > 0) return "ready";
  return "draft";
}
```

Problems:
1. **No true DRAFT vs READY distinction in DB** — a brand-new design and a "configured but unpublished" design are both `active = false`. You cannot query "designs ready to publish" without recalculating client-side heuristics.
2. **Unpublish destroys state context** — when a PUBLISHED design is unpublished, it becomes indistinguishable from a new DRAFT. There is no "was published, now paused" state.
3. **No DB-level transition guards** — the database cannot enforce "cannot set active=true without variants". This must be done in code (currently not done — see F-03).
4. **Inconsistent with other entities** — `Collection`, `CatalogGroup`, `DesignGarment` also use Boolean `active` rather than a status enum.

### Recommendation
Add a Flyway migration to replace `active BOOLEAN` with `status VARCHAR(20)` mapped to a `DesignStatus` enum: `DRAFT → READY → PUBLISHED`. This is a breaking schema change requiring a migration that backfills `active = true` → `PUBLISHED` and `active = false` → `DRAFT`. Not urgent for MVP if business volume is low, but becomes painful at scale.

---

## F-06 — `DesignController` Has No `@PreAuthorize` Annotation
**Severity: MAJOR**

The admin publish endpoint is protected solely by the URL prefix rule in `SecurityConfig.java:121`:
```java
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
```

`DesignController` has **zero** `@PreAuthorize` or `@Secured` annotations. Evidence:
```
grep "@PreAuthorize" DesignController.java → No matches found
```

By contrast, `SizeChartController.java` does add defense-in-depth:
```java
@PreAuthorize("hasRole('ADMIN')")  // line 38
@PreAuthorize("hasRole('ADMIN')")  // line 52
```

This inconsistency means:
- **Today:** No exploitable vulnerability — the URL filter catches all requests.
- **Risk:** If a future refactor moves the controller to a different path (e.g. `/api/v2/...`), or a Spring Security configuration update changes precedence, the controller has no fallback protection.

### Fix required
Add `@PreAuthorize("hasRole('ADMIN')")` at the class level on `DesignController`:
```java
@PreAuthorize("hasRole('ADMIN')")
public class DesignController implements DesignApi { ... }
```

---

## F-07 — Potential N+1 in Public `getCollectionBySlug`
**Severity: MINOR**

**Admin path: SAFE.** `DesignRepository.findAllWithGarments()` and `findByCollectionIdWithGarments()` both use `@EntityGraph(attributePaths = "garments")`, loading garments in a single JOIN. `activeGarmentCount` is computed in-memory.

**Public path — single design: SAFE.** `getDesignBySlug()` issues 2 queries (design + garments). Not an N+1 for a single item.

**Public path — collection page: UNVERIFIED RISK.**  
`getCollectionBySlug()` (CatalogStorefrontServiceImpl.java:75–95) loads all designs for a collection using `findByCollection_IdAndActiveTrueOrderByCreatedAtDesc()` — a plain JPA method with **no `@EntityGraph`**. It then calls `designMapper.toResponse(design)` for each design. If `DesignMapper` accesses any lazy-loaded association (e.g. `design.getCollection()` or `design.getGarments()`), this becomes an N+1 query — one extra SQL per design in the collection.

The `DesignMapper` was not fully audited but the risk is real if it maps garment count or collection fields.

### Fix required
Verify `DesignMapper.toResponse()` does not touch lazy associations. If it does, add `@EntityGraph` to the `findByCollection_IdAndActiveTrueOrderByCreatedAtDesc()` query.

---

## F-08 — Unpublished Designs Are Inaccessible on All Public Paths
**Severity: PASS ✓**

All public storefront repository methods use `ActiveTrue` variants. Evidence:

| Endpoint | Repository method | Active filter? |
|----------|------------------|----------------|
| GET /catalog/designs (list) | `findAllByActiveTrueOrderByCreatedAtDesc()` | ✓ Yes |
| GET /catalog/designs?collectionId=N | `findByCollection_IdAndActiveTrueOrderByCreatedAtDesc()` | ✓ Yes |
| GET /catalog/collections/{slug} | `findBySlugAndActiveTrue()` + `findByCollection_IdAndActiveTrueOrderByCreatedAtDesc()` | ✓ Yes |
| GET /catalog/designs/{slug} (direct URL) | `findBySlugAndActiveTrue()` → NotFoundException → 404 | ✓ Yes |
| Search | No search endpoint exists in public API | N/A |

A customer visiting `/catalog/group/collection/draft-slug` receives a 404. No draft leakage.

---

## F-09 — Admin Publish Endpoint is URL-Protected
**Severity: PASS ✓ (with caveat — see F-06)**

`SecurityConfig.java:121`:
```java
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
```

| Caller | HTTP Response |
|--------|--------------|
| Unauthenticated | 401 Unauthorized |
| Authenticated non-admin | 403 Forbidden |
| Authenticated admin | 200 OK |

Protection is real and working. Caveat: no method-level annotation (F-06).

---

## F-10 — Admin Panel 100% Hardcoded Russian
**Severity: MINOR**

All admin pages (`AdminDesignsPage`, `AdminUsersPage`, `AdminCollectionsPage`, etc.) use hardcoded Russian labels, placeholders, error messages, and status text. Since the admin panel is an internal tool used only by store staff (Russian-speaking), this is **intentional and acceptable** for the current stage.

Notable: status badge labels in `AdminDesignsPage.tsx` (`"ЧЕРНОВИК"`, `"ГОТОВ"`, `"ОПУБЛИКОВАН"`) are hardcoded Russian inline strings rather than constants — minor maintainability issue.

---

## Summary Scorecard

| Area | Status | Severity |
|------|--------|----------|
| Customer i18n coverage | 85% strings hardcoded, language switcher non-functional | **CRITICAL** |
| Accessibility (aria-labels) | 3 hardcoded Russian aria-labels in live header | **MAJOR** |
| Backend publish validation | Zero server-side validation, all guards client-only | **MAJOR** |
| Publish completeness checks | Image + variant-count only; price/size/color/stock missing | **MAJOR** |
| Domain model (active vs enum) | Boolean cannot express 3 states cleanly | **MAJOR** |
| Admin endpoint defense-in-depth | URL-guarded only, no @PreAuthorize on controller | **MAJOR** |
| N+1 queries (admin) | Protected with @EntityGraph | PASS |
| N+1 queries (public collection) | Unverified — mapper may access lazy relations | **MINOR** |
| Draft leakage on direct URL | 404 returned correctly | PASS |
| Draft leakage in search | No search endpoint exists | PASS |
| Admin panel i18n | 100% Russian (intentional for internal tool) | **MINOR** |

---

## Recommended Fix Priority

### Before launch (blockers)
1. **F-01** — i18n CartPage.tsx and DesignPage.tsx (purchase funnel)
2. **F-03** — Add server-side validation to `DesignServiceImpl.setActive()`
3. **F-04** — Extend publish guard to check price ≥ 1, sizes > 0, colors > 0

### Before launch (important)
4. **F-02** — Fix SiteNavbar 3 hardcoded aria-labels
5. **F-06** — Add `@PreAuthorize("hasRole('ADMIN')")` to `DesignController`

### After launch (backlog)
6. **F-07** — Verify `DesignMapper` for N+1 on collection page
7. **F-01 (remaining)** — i18n AnnouncementBar, ProductCard, footer, home widgets
8. **F-05** — Migrate `Design.active` to `DesignStatus` enum (schema migration)
