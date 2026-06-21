# Admin Panel Audit

Date: 2026-06-20

## Summary

| Area | Status |
|---|---|
| Catalog Groups (categories) | ✅ Working |
| Collections | ✅ Working |
| Designs | ✅ Working |
| Design Variants (garments / prices / inventory) | ✅ Working |
| Size Charts | ✅ Working |
| Orders — list view | ✅ Working |
| Orders — detail / status change | ✅ Working |
| Orders — CDEK shipment | ✅ Working |
| Payments — list with filtering | ✅ Working |
| Customers | ✅ Working |
| Legacy Products | ⚠️ Functional but deprecated |
| User role management | ❌ No UI |
| Exchange rate management | ✅ Working |

---

## Feature Details

### Catalog Groups (`/admin/categories`)
- **Create**: name, slug (auto-generated from name), sort order ✅
- **Edit**: inline form re-populated on "Edit" click ✅
- **Delete**: delete button with confirmation implicit in API error handling ✅
- **Notes**: slug auto-derives from name unless manually overridden. Duplicate slug returns 409 from backend.

### Collections (`/admin/collections`)
- **Create**: group selection (cascade), name, slug, description, cover image, banner image ✅
- **Edit**: all fields editable inline ✅
- **Delete**: delete button ✅
- **Image upload**: cover and banner uploaded to MinIO via `/api/v1/media/upload` ✅
- **Notes**: collection image upload requires MinIO to be running. See MinIO Audit section.

### Designs (`/admin/designs`)
- **Create**: category → collection cascade selection, name, slug, description, main image, gallery ✅
- **Edit**: all fields ✅
- **Delete**: ✅
- **Image upload**: main image and multi-image gallery ✅
- **Link to storefront**: "открыть на витрине ↗" button visible in list ✅

### Design Variants (`/admin/designs/:designId/variants`)
- **Add garment type**: dropdown of unused types (HOODIE, T_SHIRT, SWEATSHIRT, etc.) ✅
- **Delete garment**: ✅
- **Set price per garment**: per-currency (KZT) price form ✅
- **Manage colors**: assign global colors to garment variant ✅
- **Manage sizes**: assign global sizes to garment variant ✅
- **Set inventory**: quantity per (garment × color × size) combination ✅
- **Add global color**: inline form to create new global color ✅
- **Add global size**: inline form to create new global size ✅

### Size Charts (`/admin/size-charts`)
- **Create / update**: garment type, title, image upload ✅
- **Delete**: ✅
- **Notes**: one chart per garment type (upsert semantics).

### Orders (`/admin/orders` and `/admin/orders/:id`)
- **List view**: table with order number, date, customer, phone, delivery type, status, total ✅
- **Status change**: dropdown with all `OrderStatus` values, PATCH call to backend ✅
- **Order detail**: shows all items, delivery address, delivery fee, total breakdown ✅
- **CDEK shipment**: create shipment, sync status, cancel shipment ✅
- **Notes**: no pagination — all orders load in one request. May become slow at high order volume.

### Payments (`/admin/payments`)
- **List view**: all payments with date, order link, provider badge, status, amount ✅
- **Filter by provider**: FREEDOM_PAY / PAYPAL / All ✅
- **Filter by status**: PENDING / SUCCEEDED / FAILED / CANCELLED / REFUNDED / All ✅
- **Manual refresh button**: ✅
- **Notes**: no link to the related order from payments list. Add it for faster cross-referencing.

### Customers (`/admin/customers`)
- **List view**: customer name, phone, telegram ✅
- **Create**: name, phone, telegram ✅
- **Edit**: inline ✅
- **Delete**: ✅

### Exchange Rate (`/admin/exchange-rate`)
- **View current KZT/USD rate**: ✅
- **Manual update**: ✅
- **Fetch from NBK**: ✅

---

## Issues and Recommendations

### ❌ No user role management UI
Users cannot be promoted to ADMIN through the frontend. The only way is via the API:
```
POST /api/v1/auth/admin/grant
Authorization: Bearer <admin-token>
{"email": "user@example.com", "role": "ADMIN"}
```
**Recommendation**: Add a simple `/admin/users` page listing `AppUser` rows with a "Grant ADMIN" button.

### ⚠️ Legacy Products page (`/admin/products`)
The `Product` entity is a legacy model that predates the Design / DesignGarment catalog. Products created here:
- Do NOT appear in the new design catalog (`/catalog/*`)
- Are served by the legacy `/api/v1/product/**` endpoints
- Have no garment / color / size variants

**Recommendation**: Hide the Products page from the admin sidebar. Keep the backend for backward compatibility but stop using it for new content.

### ⚠️ No pagination on orders list
All orders load in a single request. At a few hundred orders this will be slow.
**Recommendation**: Add `?page=&size=` parameters to `GET /api/v1/order` and paginate the frontend.

### ⚠️ Payment list does not link to order
The admin payments table shows `orderId` as a number but does not link to `/admin/orders/:id`.
**Recommendation**: Wrap `orderId` in a `<Link to={/admin/orders/${p.orderId}}>`.

### ⚠️ No image deletion / replacement from variants page
Once a garment is assigned, its images live on the design level (main image / gallery). No dedicated "replace image" on the variant level — admin must go back to the Designs page.
This is by design but worth documenting.

---

## Broken Features

None confirmed broken. All CRUD operations for the catalog (groups → collections → designs → variants → inventory) are functional.

---

## Recommended Next Steps

1. Add `/admin/users` page for role management (high priority — no workaround without API knowledge).
2. Paginate orders list.
3. Add order link in payments table.
4. Deprecate/hide legacy Products page.
5. Consider bulk status change for orders (e.g., mark multiple as SHIPPED).
