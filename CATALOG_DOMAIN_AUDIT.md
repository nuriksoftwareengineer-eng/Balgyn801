# Catalog Domain Audit — Phase 5.2 (2026-06-20)

## Current Architecture

```
CatalogGroup (категория)
  └── Collection (коллекция)
        └── Design (дизайн, the product)
              └── DesignGarment (вариант)
                    ├── prices: DesignGarmentPrice[]  (per currency)
                    ├── colors: Color[]               (M:N via join table)
                    └── sizes: Size[]                 (M:N via join table)
                          └── Inventory[color × size] (qty per cell)
```

**Legacy system (unchanged):** `Product` entity from 02.06 baseline. Not migrated.

---

## UX Issues Found

### Issue 1: New designs immediately appeared in catalog (CRITICAL)

**Before:** `Design.active = true` by default.  
Admin creates design → it immediately appears on storefront → customers see "Нет доступных вариантов".

**After:** `Design.active = false` by default (DRAFT).  
Design is invisible until admin explicitly publishes it via the new PATCH endpoint.

### Issue 2: No guidance after design creation

**Before:** After "Создать", form resets and user stays on the page. No indication that variants are needed.

**After:** After "Создать и перейти к вариантам →", page navigates to `/admin/designs/{id}/variants` automatically.

### Issue 3: No visibility into design completeness

**Before:** All designs looked the same in the list table.

**After:** Status badge per design:
- **ЧЕРНОВИК** (gray) — `active=false`, no garments or no image
- **ГОТОВ** (blue) — `active=false`, has image + has active garments, ready to publish
- **ОПУБЛИКОВАН** (green) — `active=true`

Variant count shown in table column.

### Issue 4: Publication without validation

**Before:** No publish/unpublish concept existed in admin. `active` was not toggleable from admin UI.

**After:**
- **"Опубликовать"** button visible per design
- Validates before publishing: requires image + at least 1 active garment
- Shows inline error if requirements not met
- **"Снять"** button to unpublish (no validation — safe to always allow)

---

## Recommended Architecture (current state after Phase 5.2)

### Design Status State Machine

```
DRAFT (active=false, no garments)
  │
  ├── [add image + create variants] →
  │
READY (active=false, has image + has active garments)
  │
  ├── [Опубликовать] →
  │
PUBLISHED (active=true)
  │
  └── [Снять] → back to READY or DRAFT
```

Status is computed client-side from `active` + `mainImageUrl` + `activeGarmentCount`.
No new DB column added — derives from existing fields.

### New Backend Endpoint

```
PATCH /api/v1/admin/catalog/designs/{id}/active
Body: { "active": true | false }
```

Guards: Admin role required (inherits from `/api/v1/admin/**` security config).  
Validation: Frontend-side only (image + garments). Server sets `active` unconditionally — admin is trusted.

### Backend Changes Summary

| File | Change |
|------|--------|
| `domain/Design.java` | `active = false` default (was `true`) |
| `dto/responce/DesignResponse.java` | Added `activeGarmentCount: Integer` |
| `api/DesignApi.java` | Added `PATCH /{id}/active` |
| `service/DesignService.java` | Added `setActive(Long id, boolean active)` |
| `service/Impl/DesignServiceImpl.java` | Implement `setActive`; `create()` explicitly sets `active=false`; `getAll()` uses `@EntityGraph` to eager-load garments for count |
| `controller/DesignController.java` | Implement `setActive` handler |
| `repositories/DesignRepository.java` | Added `findAllWithGarments()` + `findByCollectionIdWithGarments()` with `@EntityGraph` to avoid N+1 |

---

## Design vs Product: Should They Be Merged?

**Verdict: NO. Keep separate.**

| | Product (legacy) | Design (current) |
|--|--|--|
| Created | 02.06.2026 | 09.06.2026 |
| Purpose | Generic product catalog | Embroidery design catalog |
| Variants | None (monolithic) | DesignGarment with colors/sizes/inventory |
| Active orders | Unknown | All new orders use Design |
| Migration cost | High | — |

**Recommendation:** Mark `Product` as deprecated in a future phase. Migrate any legacy `Product`-based orders to read-only history. Do NOT delete — foreign keys from old orders exist.

---

## Admin Workflow After Phase 5.2

```
1. Admin → /admin/designs → "Создать и перейти к вариантам →"
   └── Form: category, collection, name, slug, image, gallery
   
2. After creation → auto-navigate to /admin/designs/{id}/variants
   └── Add garment types (hoodie, sweatshirt, t-shirt)
   └── Set prices per garment
   └── Assign colors + sizes
   └── Set inventory per color×size
   
3. Return to /admin/designs
   └── Status shows ГОТОВ (blue) — image + variants present
   └── Click "Опубликовать"
   └── Status changes to ОПУБЛИКОВАН (green)
   └── Design appears in public catalog
```

---

## What Remains

- [ ] Public storefront: optionally filter designs with 0 active garments from list (currently handled by DRAFT flow)
- [ ] Design status field in DB (enum) for fine-grained tracking — currently computed from existing fields
- [ ] Scheduled publishing (date/time to go live)
- [ ] Product → Design migration for legacy data
