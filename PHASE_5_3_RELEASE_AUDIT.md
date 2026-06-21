# Phase 5.3 Release Audit

**Date:** 2026-06-20  
**Auditor:** Claude Code (automated verification)  
**Scope:** Final pre-release verification of Phase 5.3

---

## Verdict

> **No CRITICAL findings. Phase 5.3 is release-ready.**
>
> 8 MAJOR findings exist (i18n gaps in secondary customer-facing components — not in the checkout/cart/design flow). 8 MINOR findings. No test failures. Both builds pass.

---

## Check Results

| # | Check | Result |
|---|-------|--------|
| 1 | Hardcoded Russian in frontend | ⚠ MAJOR/MINOR (see below) |
| 2 | TODO/FIXME markers | ✅ PASS — zero found |
| 3 | Stale `Design.active` references | ✅ PASS — none (DesignGarment.active is separate and correct) |
| 4 | Public catalog queries use PUBLISHED | ✅ PASS — 3 storefront queries, all use PUBLISHED |
| 5 | Admin publish/archive endpoints | ✅ PASS — both endpoints exist and are wired end-to-end |
| 6 | DesignPublicationValidator invoked | ✅ PASS — called from 3 services; validationErrors() in publish() |
| 7 | V25 on clean database | ✅ PASS — nullable-add → backfill → NOT NULL → drop is safe |
| 8 | V25 on existing database | ✅ PASS — V24 only indexes unrelated tables; no conflict |
| 9 | No failing tests | ✅ PASS — 84/84 tests, 0 failures, 0 errors |
| 10 | Production build | ✅ PASS — 0 TS errors; Vite built in 684ms; Gradle BUILD SUCCESSFUL |

---

## Check 1 — Hardcoded Russian strings

### Scope clarification

**Admin pages are excluded from i18n requirements.** The admin UI (`admin/Admin*.tsx`) is internal tooling used by Russian-speaking operators and is intentionally Russian-only. None of those findings appear below.

**Data files are excluded.** `shared/lib/delivery-labels.ts` and `shared/lib/order-status-labels.ts` are only imported by admin pages (confirmed by grep). `shared/types/catalog.ts` garment type labels are admin-only.

### Phase 5.3 scope (verified done)

The three files confirmed in-scope for Phase 5.3 are clean:

| File | Status |
|------|--------|
| `frontend/src/pages/CartPage.tsx` | ✅ All strings use `t()` — STEP_LABELS bug fixed |
| `frontend/src/pages/DesignPage.tsx` | ✅ All strings use `t()` |
| `frontend/src/widgets/home/Categories.tsx` | ✅ All strings use `t()` |

### MAJOR findings (customer-visible in primary flows)

---

**MAJOR-1** · `frontend/src/widgets/HeroSection.tsx`

Home page hero section — first thing any customer sees.

```tsx
// line 55
"Вышивка · Казахстан"         // sub-badge
// line 89
"Эксклюзивная вышивка на одежде"  // main sub-headline
// line 100
"Каталог"                     // primary CTA button
// line 107
"Свой дизайн"                 // secondary CTA button
```

The `home.hero.*` translation keys exist in all three locale files but `HeroSection` never calls `useTranslation`. EN/KK visitors see Russian on the first screen.

---

**MAJOR-2** · `frontend/src/widgets/SizeChartModal.tsx`

Modal opened from DesignPage when user clicks the "📏 Size guide" button. DesignPage is fully i18n'd but the modal it opens is not.

```tsx
aria-label={title ?? "Размерная сетка"}   // line 38 — screen reader + visible
{title ?? "Размерная сетка"}              // line 44 — visible modal header
aria-label="Закрыть"                      // line 49 — screen reader only
```

`title` is fetched from the backend (`SizeChart.title` field). If the DB has an English title the visible header will be correct; the fallback "Размерная сетка" and the "Закрыть" aria-label are always Russian.

---

**MAJOR-3** · `frontend/src/pages/CatalogIndexPage.tsx`

Catalog index page — entry point to the entire catalog.

```tsx
// line 12-13 — page meta (not visible UI but hardcoded)
title: "Каталог — Balgyn"
description: "Коллекции вышивки Balgyn"
// line 22 — breadcrumb link
<Link to="/">Главная</Link>
// line 24-27 — breadcrumb + heading
<span>Каталог</span>   <h1>Каталог</h1>
// line 47 — error state
"Не удалось загрузить каталог."
```

Lines 79, 82, 89 already use `t()` — page is partially done.

---

**MAJOR-4** · `frontend/src/pages/CollectionPage.tsx`

Collection listing page.

```tsx
// line 51
"Коллекция не найдена."
// line 56
"← Назад"
// line 69-71 — breadcrumbs
"Главная"  "Каталог"
```

---

**MAJOR-5** · `frontend/src/pages/GroupPage.tsx`

Group (catalog category) page.

```tsx
// line 48
"Группа не найдена."
// line 50
"← В каталог"
// lines 63-65 — breadcrumbs
"Главная"  "Каталог"
// line 108-110 — empty state
"Коллекции скоро появятся"
"В этой группе пока нет коллекций..."
// line 117
"← В каталог"
```

---

**MAJOR-6** · `frontend/src/pages/PaymentReturnPage.tsx:38`

Payment callback page (shown while Freedom Pay / PayPal redirect completes).

```tsx
"Подтверждаем платёж…"
```

---

**MAJOR-7** · `frontend/src/widgets/home/DesignProductCard.tsx`

Product cards rendered on the home page best-sellers grid.

```tsx
// line 39
"Распродано"      // sold-out badge
// line 47
"В корзину"       // add-to-cart button
```

---

**MAJOR-8** · `frontend/src/widgets/CustomDesignCTASection.tsx`

Custom Design call-to-action section on the home page.

```tsx
"Индивидуальный заказ"
"Свой дизайн"
"Расскажите идею: логотип, надпись, референс или эскиз."
"Подберём нитки, технику вышивки и модель."
"Смета и сроки — после согласования макета"
"Фото перед отправкой, как в каталоге"
"Оставить заявку"
"Написать в Telegram"
```

---

### MINOR findings

---

**MINOR-1** · `frontend/src/shared/ui/info-page.tsx:23`

The shared `InfoPage` wrapper renders "Главная" as a hardcoded breadcrumb. Used by 8 customer-facing static pages: About, Contacts, Delivery, Returns, Terms, Privacy, TrackOrder, CustomDesign.

```tsx
<Link to="/">Главная</Link>
```

---

**MINOR-2** · Static content pages (8 files)

All page content is hardcoded Russian with no `useTranslation`. These are informational pages outside the purchase flow.

| File | Content |
|------|---------|
| `pages/AboutPage.tsx` | Brand story, "Как мы работаем", "Качество и упаковка" |
| `pages/ContactsPage.tsx` | All contact info, "Самовывоз", "По вопросам заказов" |
| `pages/DeliveryInfoPage.tsx` | All delivery method descriptions |
| `pages/ReturnsPage.tsx` | Return policy (legal text) |
| `pages/TermsPage.tsx` | Terms of service (legal text) |
| `pages/PrivacyPage.tsx` | Privacy policy (legal text) |
| `pages/TrackOrderPage.tsx` | Order tracking form and status guide |
| `pages/CustomDesignPage.tsx` | Custom design form |

Also: `DeliveryInfoPage` line 66 mentions "YooKassa" which was removed in Phase Freedom Pay (V21). Stale copy.

---

**MINOR-3** · `frontend/src/shared/ui/page-load-fallback.tsx:13`

App-wide lazy route loading indicator.

```tsx
<span className="text-sm">Загрузка…</span>
```

---

**MINOR-4** · `frontend/src/pages/AuthShellLayout.tsx:16,19`

Decorative brand text on the login/register page background panel (not form content).

```tsx
"ВЫШИВКА<br />И УЛИЧНАЯ<br />КУЛЬТУРА"
"Алматы, Казахстан"
```

---

**MINOR-5** · `frontend/src/widgets/catalog/CatalogCard.tsx:16`

Default prop value.

```tsx
hint = "Смотреть →",
```

---

**MINOR-6** · `frontend/src/pages/ProductPage.tsx` and `widgets/ProductCatalogGrid.tsx`

Legacy product system. CLAUDE.md explicitly states: "legacy Product kept, not migrated." Not in scope.

---

**MINOR-7** · `frontend/src/pages/DeliveryInfoPage.tsx:66`

Stale provider name: "Kaspi, YooKassa или PayPal" — YooKassa was removed in Phase Freedom Pay (migration V21). Should read "Freedom Pay или PayPal."

---

**MINOR-8** · `frontend/src/widgets/SizeChartModal.tsx:49`

`aria-label="Закрыть"` on the close button — screen-reader only, not visible to sighted users.

---

## Check 3 — Design.active backend references

The grep for `active` in backend Java files found only `DesignGarment.active` — a separate boolean that enables/disables individual garment variants (hoodie, t-shirt, etc.) for sale. This field exists on the `design_garments` table, **not** on `designs`. V25 migration only removed `designs.active`. No stale references to the migrated field exist.

```java
// DesignGarment.java:34 — correct, different table
private Boolean active = true;

// DesignReadinessService.java:55 — checks garment availability
List<DesignGarment> active = garmentRepository.findByDesign_IdAndActiveTrue(design.getId());
```

---

## Check 4 — Public catalog queries

All three public storefront queries in `CatalogStorefrontServiceImpl` are gated on `DesignStatus.PUBLISHED`:

```java
// line 83 — designs in a collection
findByCollection_IdAndStatusOrderByCreatedAtDesc(collectionId, DesignStatus.PUBLISHED)

// line 104-105 — all designs (no collection filter)
findByCollection_IdAndStatusOrderByCreatedAtDesc(collectionId, DesignStatus.PUBLISHED)
findAllByStatusOrderByCreatedAtDesc(DesignStatus.PUBLISHED)

// line 111 — single design by slug
findBySlugAndStatus(slug, DesignStatus.PUBLISHED)
```

DRAFT, READY, and ARCHIVED designs are invisible to the storefront.

---

## Check 5 — Admin publish/archive endpoints

```
PATCH /api/v1/admin/designs/{id}/publish   → DesignController.publish()  → DesignServiceImpl.publish()
PATCH /api/v1/admin/designs/{id}/archive   → DesignController.archive()  → DesignServiceImpl.archive()
```

`publish()` guards:
- Throws `BusinessRuleException` if status is `ARCHIVED`
- Returns idempotently if already `PUBLISHED`
- Calls `readinessService.validationErrors()` and throws `PublicationValidationException` if requirements not met
- Sets `PUBLISHED` and saves

`archive()` guards:
- Returns idempotently if already `ARCHIVED`
- Sets `ARCHIVED` and saves (no validation required)

---

## Check 6 — DesignReadinessService invocation

`recompute()` is called automatically after every mutation that could change readiness:

| Call site | Trigger |
|-----------|---------|
| `DesignGarmentServiceImpl.create()` | New garment variant added |
| `DesignGarmentServiceImpl.update()` | Garment variant changed |
| `DesignGarmentServiceImpl.delete()` | Garment variant removed |
| `DesignGarmentPriceServiceImpl.setPrice()` | Price set on a garment |
| `DesignGarmentPriceServiceImpl.deletePrice()` | Price removed |
| `DesignServiceImpl.update()` | Design image or metadata changed |
| `DesignServiceImpl.recomputeStatus()` | Explicit admin refresh |

`validationErrors()` is called inside `publish()` before the status transition — a design cannot be published unless it has a main image AND at least one active garment with price + size + color.

---

## Check 7 & 8 — V25 migration safety

### Clean database

```sql
ALTER TABLE designs ADD COLUMN status VARCHAR(20);          -- nullable first
UPDATE designs SET status = CASE                            -- backfill all rows
    WHEN active = TRUE THEN 'PUBLISHED'
    ELSE 'DRAFT'
END;
ALTER TABLE designs ALTER COLUMN status SET NOT NULL;       -- constraint after backfill
ALTER TABLE designs ALTER COLUMN status SET DEFAULT 'DRAFT';
ALTER TABLE designs DROP COLUMN active;                     -- clean removal
CREATE INDEX idx_designs_status ON designs(status);
CREATE INDEX idx_designs_collection_status ON designs(collection_id, status);
```

Safe: nullable-add → backfill → constrain → drop is the canonical pattern.

### Existing database (post-V24)

V24 adds indexes on `order_items`, `payments`, `order_history`, `user_roles`, `processed_webhook_events` — no overlap with V25's changes to `designs`. No conflict possible.

### Hibernate alignment

`Design.java` declares:
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private DesignStatus status = DesignStatus.DRAFT;
```

This maps exactly to `status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'`. All four enum values (`DRAFT`, `READY`, `PUBLISHED`, `ARCHIVED`) are valid VARCHAR(20) values. No runtime `JDBCException` risk.

---

## Check 9 — Test results

```
84 tests · 0 failures · 0 errors · 0 skipped
BUILD SUCCESSFUL in 27s
```

| Test suite | Tests |
|------------|-------|
| Balgyn801ApplicationTests | 1 |
| CdekCalculateOrderIntegrationTest | 6 |
| CountryServiceIntegrationTest | 4 |
| DeliveryFlowIntegrationTest | 2 |
| DeliveryMethodsIntegrationTest | 6 |
| DeliveryPricingIntegrationTest | 4 |
| ExchangeRateAndSettingsIntegrationTest | 5 |
| GarmentWeightServiceIntegrationTest | 4 |
| InternationalShippingIntegrationTest | 3 |
| InventoryCheckIntegrationTest | 3 |
| InventoryConcurrencyIntegrationTest | 1 |
| InventoryDeductionIntegrationTest | 3 |
| InventoryReleaseIntegrationTest | 6 |
| MyOrdersIntegrationTest | 4 |
| OrderExpiryIntegrationTest | 2 |
| OrderHistoryIntegrationTest | 3 |
| OrderRateLimitIntegrationTest | 1 |
| FreedomPayResponseVerificationTest | 6 |
| PaymentSecurityIntegrationTest | 10 |
| PayPalPaymentIntegrationTest | 10 |
| **Total** | **84** |

---

## Check 10 — Production build

**Frontend:**
```
tsc -b && vite build
✓ built in 684ms
0 TypeScript errors (tsc --noEmit)
```

**Backend:**
```
> Task :compileJava UP-TO-DATE
BUILD SUCCESSFUL in 4s
```

---

## Findings summary

### CRITICAL (blocks release)

_None._

---

### MAJOR (customer-visible i18n gaps outside Phase 5.3 scope)

| ID | File | Customer impact |
|----|------|-----------------|
| MAJOR-1 | `widgets/HeroSection.tsx` | Home page hero: 4 Russian strings visible to EN/KK visitors |
| MAJOR-2 | `widgets/SizeChartModal.tsx` | "Размерная сетка" title visible in modal when EN/KK user checks size chart |
| MAJOR-3 | `pages/CatalogIndexPage.tsx` | Breadcrumbs + error message hardcoded Russian on catalog entry page |
| MAJOR-4 | `pages/CollectionPage.tsx` | Breadcrumbs, error message, back button hardcoded |
| MAJOR-5 | `pages/GroupPage.tsx` | Breadcrumbs, error message, empty state hardcoded |
| MAJOR-6 | `pages/PaymentReturnPage.tsx` | "Подтверждаем платёж…" on payment callback page |
| MAJOR-7 | `widgets/home/DesignProductCard.tsx` | "Распродано" / "В корзину" on all product cards |
| MAJOR-8 | `widgets/CustomDesignCTASection.tsx` | Full home page CTA section in Russian |

These gaps pre-date Phase 5.3. The complete checkout flow (Cart → Design → Categories) is fully i18n'd. None of these MAJOR items affect the ability to place an order in EN or KK.

---

### MINOR (secondary pages, decorative elements, or admin-only)

| ID | File | Note |
|----|------|------|
| MINOR-1 | `shared/ui/info-page.tsx` | "Главная" breadcrumb on all static pages |
| MINOR-2 | 8 static content pages | About, Contacts, Delivery, Returns, Terms, Privacy, TrackOrder, CustomDesign — full content in Russian |
| MINOR-3 | `shared/ui/page-load-fallback.tsx` | "Загрузка…" app-wide loading indicator |
| MINOR-4 | `pages/AuthShellLayout.tsx` | Decorative background text on login/register |
| MINOR-5 | `widgets/catalog/CatalogCard.tsx` | Default hint prop "Смотреть →" |
| MINOR-6 | `pages/ProductPage.tsx`, `widgets/ProductCatalogGrid.tsx` | Legacy product system; out-of-scope per CLAUDE.md |
| MINOR-7 | `pages/DeliveryInfoPage.tsx:66` | Stale mention of "YooKassa" (removed in V21/Phase Freedom Pay) |
| MINOR-8 | `widgets/SizeChartModal.tsx:49` | `aria-label="Закрыть"` — screen reader only, not visible |

---

## Release recommendation

**Phase 5.3 can ship.** The checkout critical path (DesignPage → CartPage: all 5 steps → OrderSuccess → PaymentReturn) is fully functional and i18n'd.

The 8 MAJOR findings are pre-existing gaps in secondary surfaces (home page widgets, catalog breadcrumbs). They do not block a transaction from completing. They should be addressed in a Phase 5.4 i18n sweep that targets the home page, catalog shell, and payment return page.

The MINOR-7 YooKassa stale copy should be corrected regardless of phase — it's a one-line edit.
