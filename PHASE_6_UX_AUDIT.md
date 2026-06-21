# Phase 6 UX, Catalog & Admin Polish — Audit

**Date:** 2026-06-20  
**Status:** Pre-implementation audit  

---

## Task 1 — Product Legacy Audit

### Files referencing Product

**Backend (18 Java files):**
`Product.java`, `ProductRepository.java`, `ProductService.java`, `ProductServiceImpl.java`,
`ProductController.java`, `ProductApi.java`, `ProductMapper.java`, `ProductResponse.java`,
`CreateProductRequest.java`, `ProductColorOption.java`, `StoreCategories.java`,
`OrderItem.java` (product_id FK), `OrderServiceImpl.java` (legacy routing),
`CdekDeliveryService.java` (dimensions), `CdekOrderItemRequest.java`,
`OrderItemResponse.java`, `OrderItemRequest.java`, `MediaStorageService.java`

**Frontend (5 TS/TSX files):**
`ProductPage.tsx`, `ProductCatalogGrid.tsx`, `CatalogParamPage.tsx` (numeric slug dispatch),
`AdminLayout.tsx` (nav link), `AdminDashboardPage.tsx` (quick link)

### Key finding

`OrderItem` carries both a `product_id` FK (legacy) and a `design_garment_id` FK (new catalog).
`OrderServiceImpl` routes by checking `designGarmentId != null`. Both paths are live.
Real orders in the DB may reference legacy products — hard removal would break order history.

### Verdict: **B — Deprecate Product**

Do NOT remove yet. Safe deprecation path:
1. Remove "Товары (legacy)" from admin sidebar nav
2. Keep `products` table, `OrderItem.product_id`, `ProductPage` (read-only fallback for legacy URLs)
3. Block new product creation (hide `AdminProductsPage` form)
4. Once confirmed zero rows in `products` — schedule full removal with Flyway migration

---

## Task 2 — Language Switcher Visibility

### Issues found

| Severity | Location | Issue |
|---|---|---|
| Medium | SiteNavbar:44 | Hardcoded `#7A7A7A` instead of `--color-muted` CSS variable |
| Medium | SiteNavbar:79 | Emoji `🌐` may render as box on some Windows/Android fonts |
| Low | SiteNavbar mobile | Inactive language `text-white/40` — marginally low contrast on mobile menu |
| Low | SiteNavbar transparent | `text-white/70` on language trigger — passes AA only at larger sizes |

### Fix plan
- Replace emoji with a proper SVG Globe icon (Lucide `Globe`)
- Replace `#7A7A7A` with `var(--color-muted)` or `text-[--color-muted]`
- Bump mobile inactive from `/40` to `/60`

---

## Task 3 — Checkout Redesign

### Issues found

| Severity | Location | Issue |
|---|---|---|
| High | CartPage `StepIndicator` line 160 | Step labels `text-[0.5rem]` (8px) — illegible on mobile |
| High | CartPage `SummarySidebar` | `hidden lg:block` — no price summary visible on mobile/tablet during checkout |
| High | CartPage payment section | `text-white/70` at `text-[11px]` fails WCAG AA on selected card |
| Medium | CartPage step 4 | Skipped step ghosted at `opacity-25` rather than hidden — confusing |
| Low | StepIndicator | Missing `aria-current="step"` on active node |
| Low | SummarySidebar | `grandTotal` variable name misleading (it's subtotal only) |

### Fix plan
- Step labels: `text-[0.5rem]` → `text-[0.65rem]`
- Mobile summary: add compact sticky bottom bar on `<lg` screens showing item count + total
- Payment bullets: `text-[11px] text-white/70` → `text-[12px] text-white/80`
- Payment section: redesign provider cards to show card brand names + logos, not gateway names
- Skipped step: completely hide when not applicable rather than ghost it

---

## Task 4 — Admin Panel Redesign

### Issues found

| Severity | Issue |
|---|---|
| High | `AdminOrdersPage`: order status has NO badge/color — raw text only |
| High | `AdminDesignVariantsPage`: `inputClass` constant duplicated in two files |
| Medium | Corner radius vocabulary: 4 different values used across admin (`rounded-[10px]`, `rounded-[14px]`, `rounded-lg`, `rounded-full`) |
| Medium | Page heading scale: `text-4xl font-display` vs `text-2xl font-bold` — no standard |
| Medium | `AdminDashboardPage`: no KPI metric cards — placeholder text only |
| Medium | `AdminDesignVariantsPage`: active/inactive is text-only (`● активен`) — needs toggle |
| Low | `AdminLayout` sidebar: "Товары (legacy)" nav item should be removed or marked deprecated |
| Low | Shared `Badge` component at `components/ui/badge.tsx` exists but never used in admin |
| Low | `AdminDashboardPage`: role-management form misplaced on the dashboard |

### Fix plan
- Add status color badges to `AdminOrdersPage` matching design from `AdminDesignsPage`
- Standardize page headings to `text-2xl font-bold`
- Standardize corner radius to `rounded-lg` everywhere in admin
- Add 4 KPI stat cards to Dashboard (total orders, pending, revenue, customers)
- Remove "Товары (legacy)" from sidebar or demote to deprecated section
- Extract shared `inputClass` to `admin/shared/adminStyles.ts`

---

## Task 5 — Inventory UX

### Issues found

| Severity | Issue |
|---|---|
| CRITICAL | `DesignPage`: zero-stock size buttons are fully clickable — no disabled/hidden state |
| CRITICAL | `DesignDetailResponse` / storefront DTO carries no inventory quantity data |
| High | When order fails due to stock (backend validation), error is Russian-only, generic, with no guidance |
| Low | Legacy `Product` path uses boolean `inStock` only — no quantity check |

### Backend inventory validation (GOOD)
`OrderServiceImpl.buildDesignOrderItem()` uses `SELECT FOR UPDATE` → throws if qty=0 or qty<requested. This is correct and prevents overselling. The frontend UX around it is the problem.

### Fix plan
1. **Backend**: Add `stockMap: { [colorId: number]: { [sizeId: number]: number } }` to `DesignDetailResponse` — populated from `inventoryRepository.findByGarment_Id(garmentId)`
2. **Frontend**: In `DesignPage`, after color selection, look up stock for each size. If stock=0 → render size button with `opacity-40 cursor-not-allowed line-through` and exclude from `canAdd` check
3. **Preferred**: Hide zero-stock sizes completely (per task spec "Preferred: Hide unavailable size completely")

---

## Task 6 — Color Swatches

### Issues found

| Severity | Issue |
|---|---|
| CRITICAL | White/light color swatches have `border-transparent` — invisible on white page background |
| Medium | No fallback for missing `hexCode` (renders as transparent swatch) |

Specific code at `DesignPage.tsx` lines 354–359:
```tsx
// Unselected: border-transparent = invisible white on white bg
className={cn("h-9 w-9 rounded-none border-2 transition",
  selectedColorId === c.id
    ? "border-black scale-110"
    : "border-transparent hover:border-zinc-300"
)}
```

### Fix plan
- Replace static `border-transparent` with a dynamic border based on color brightness
- All swatches get `ring-1 ring-zinc-200` outline, overridden to `ring-black ring-2` when selected
- Alternative simple fix: always render `border border-zinc-200` on unselected, `border-2 border-black` on selected

---

## Task 7 — Payment UX

### Issues found

| Severity | Issue |
|---|---|
| High | "Freedom Pay" displayed as primary card title — customers don't know this brand |
| High | No payment provider logos — reduces trust signal |
| High | Bullet text `text-[11px] text-white/70` on selected card — fails WCAG AA at that size |
| Medium | No currency conversion warning when PayPal selected (KZT → USD) |
| Medium | Wide letter-spacing + uppercase on provider name makes it hard to read |

### Required redesign
- Card 1: Show "Bank Card" as primary label + Visa/Mastercard SVG logos. Sub-text: "Secure payment via Freedom Pay". Supported logos: Visa, Mastercard, local bank cards.
- Card 2: Show "PayPal" as primary label + PayPal SVG logo. Sub-text: "International payments · Converted to USD".
- Remove "Freedom Pay" from primary heading; keep as small sub-label.

---

## Task 8 — Frontend Polish

### Issues found

| File | Issue |
|---|---|
| `HeroSection.tsx` | ALL strings hardcoded Russian — `useTranslation` not imported at all (Phase 5 regression) |
| `info-page.tsx:22` | "Главная" breadcrumb hardcoded Russian |
| `CatalogIndexPage.tsx` | Breadcrumbs + error message hardcoded |
| `CollectionPage.tsx` | Breadcrumbs, error, back button hardcoded |
| `GroupPage.tsx` | Breadcrumbs, error, empty state hardcoded |
| `PaymentReturnPage.tsx:38` | "Подтверждаем платёж…" hardcoded |
| `DesignProductCard.tsx` | "Распродано", "В корзину" hardcoded |
| `CustomDesignCTASection.tsx` | All text hardcoded Russian (home page section) |
| `SiteNavbar.tsx:44` | `#7A7A7A` hardcoded instead of CSS variable |

### Breadcrumb strategy
All catalog/info pages need `t("nav.home")` for the home breadcrumb.
`info-page.tsx` fix is one line — highest ROI.

---

## Task 9 — Profile UX Cleanup

### Issues found

| Severity | Issue |
|---|---|
| High | `ProfilePage.tsx` lines 39–46: renders `user.roles` verbatim (e.g. `ROLE_USER`, `ROLE_ADMIN`) |
| Medium | No redirect when logged-out user visits `/profile` — page renders blank |
| Medium | No admin shortcut link for admin users |
| Low | Role badge `text-[10px]` is below comfortable reading size |

### Fix plan
- Remove the entire roles display section from ProfilePage for non-admin users
- Add conditional: if user has `ROLE_ADMIN` role → show a styled "Admin Panel" button/link
- Add `if (!user) navigate("/login")` guard
- Show only: name (if available), email, order history link, logout

---

## Implementation Priority

### Immediate (quick wins, high impact)
1. Color swatches — white border fix (Task 6)
2. Profile role cleanup (Task 9)
3. Language switcher globe icon + contrast (Task 2)
4. Step label size fix `0.5rem` → `0.65rem` (Task 3)
5. info-page.tsx "Главная" → `t("nav.home")` (Task 8)
6. HeroSection i18n wiring (Task 8)
7. Admin: remove "Товары (legacy)" from sidebar (Task 1 + Task 4)

### Medium (UX improvements, 1-4 hours each)
8. Payment UX redesign — card labels + logos (Task 7)
9. Inventory UX — add stockMap to DesignDetailResponse + hide OOS sizes (Task 5)
10. Admin order status badges (Task 4)
11. Admin dashboard KPI cards (Task 4)
12. Checkout mobile summary bar (Task 3)

### Documentation
13. PRODUCT_LEGACY_AUDIT.md
14. PHASE_6_COMPLETION_REPORT.md (after implementation)
