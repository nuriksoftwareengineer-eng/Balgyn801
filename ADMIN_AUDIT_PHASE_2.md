# Admin Panel Audit — Phase 2

**Date:** 2026-06-20  
**Auditor:** Claude Code

---

## 1. Pages & Status

| Page | Route | Status | Notes |
|------|-------|--------|-------|
| Dashboard | `/admin` | ✅ Working | Role management (grant/revoke ADMIN) |
| Orders | `/admin/orders` | ✅ Working | List + sort by date, links to detail |
| Order Detail | `/admin/orders/:id` | ✅ Working | Full order view, CDEK tracking |
| Payments | `/admin/payments` | ✅ Working | Filter by provider/status |
| Exchange Rate | `/admin/exchange-rate` | ✅ Working | Set KZT/USD, freeze, trigger refresh |
| Catalog Groups | `/admin/catalog` | ✅ Working | CRUD for groups |
| Collections | `/admin/catalog/:groupId` | ✅ Working | CRUD for collections |
| Designs | `/admin/designs/:collectionId` | ✅ Working | CRUD for designs |
| Design Variants | `/admin/designs/:id/variants` | ✅ Working | Garments, prices, colors, sizes |
| Products (legacy) | `/admin/products` | ✅ Working | Legacy product CRUD, not linked from catalog |
| Customers | `/admin/customers` | ✅ Working | Customer list (admin only) |
| Categories (legacy) | `/admin/categories` | ✅ Working | Legacy category CRUD |
| Size Charts | `/admin/size-charts` | ✅ Working | Size chart management |

---

## 2. Working ✅

- **Auth guard** — `RequireAdmin` wraps all admin routes; redirects unauthorized users
- **Order management** — full order list with sorting, delivery type, status, total
- **Payment management** — Freedom Pay + PayPal payments visible with filter
- **Catalog admin** — full `CatalogGroup → Collection → Design → DesignGarment` tree, all CRUD operations functional and E2E validated (Phase B)
- **Exchange rate admin** — manual set + freeze for KZT/USD; scheduled hourly auto-refresh from NBK; trigger refresh button
- **Role management** — grant/revoke ADMIN by email on the dashboard

---

## 3. Missing / Needs Work ❌

### 3.1 Exchange Rates — EUR and RUB not admin-editable
Phase 2 added EUR/RUB rates (V23 migration, NBK provider, `/api/v1/exchange-rates` endpoint).  
The admin panel only allows editing **KZT/USD**. EUR and RUB are updated automatically from NBK but cannot be manually set or frozen.

**Recommendation:** Extend `AdminExchangeRatePage` to display/edit all three pairs.

### 3.2 No active nav indicator in admin sidebar
Admin layout has no visual indication of which section is currently active.

**Recommendation:** Use `NavLink` with active class in `AdminLayout`.

### 3.3 Payment refunds not available
The `AdminPaymentsPage` shows payment status but has no refund button.  
Refund APIs for Freedom Pay and PayPal are not implemented on the backend.

**Recommendation:** Add refund endpoints and admin UI in Phase 3.

### 3.4 No order status bulk-update
Orders can only be updated one-by-one via Order Detail.

**Recommendation:** Add bulk status change in `AdminOrdersPage`.

### 3.5 No CDEK shipment creation UI
CDEK shipment creation is available via API (`/api/v1/cdek-shipment`) but not exposed in the admin panel. Admins must use Swagger or Postman.

**Recommendation:** Add shipment creation form in Order Detail page.

### 3.6 No catalog demo data tool
The admin panel has no bulk seeding or demo-data import tool. Adding designs requires clicking through the full `Group → Collection → Design → Variants` flow for each item.

**Recommendation:** Add a CSV/JSON import feature or seed button for testing.

### 3.7 No image upload from admin UI
Design images reference MinIO URLs but the admin panel has no upload widget for design gallery images (design variants).

**Recommendation:** Add file upload field wired to `/api/v1/media/upload` in design forms.

---

## 4. Minor Issues

- `AdminProductsPage` (legacy) is not linked from the catalog admin; it manages a deprecated `Product` entity
- `AdminCategoriesPage` (legacy) manages a deprecated `Category` entity — consider hiding from nav
- Admin pages have no i18n (Russian hardcoded); acceptable for an internal tool

---

## 5. Summary Score

| Area | Score |
|------|-------|
| Auth / Security | ✅ Solid |
| Order management | ✅ Good |
| Payment visibility | ✅ Good |
| Catalog CRUD | ✅ Complete |
| Exchange rate control | ⚠️ USD only (EUR/RUB read-only) |
| CDEK admin | ⚠️ API-only, no UI |
| Payment refunds | ❌ Not implemented |
| Image upload | ❌ Not available |
