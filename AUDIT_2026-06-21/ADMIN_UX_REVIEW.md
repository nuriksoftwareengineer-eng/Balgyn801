# ADMIN_UX_REVIEW.md
> Balgyn — Pre-Launch Admin UX Review · 2026-06-21

---

## 1. Admin Panel Structure

Access: `/admin` — guarded by `RequireAdmin` (requires `ADMIN` role in JWT).

| Route | Page | Purpose |
|-------|------|---------|
| `/admin` | Dashboard | KPI cards + quick links + role management |
| `/admin/orders` | Orders | All non-pending orders list |
| `/admin/orders/:id` | Order Detail | Status changes, CDEK shipment management |
| `/admin/categories` | CatalogGroups | CRUD for top-level catalog groups |
| `/admin/collections` | Collections | CRUD for collections within groups |
| `/admin/designs` | Designs | CRUD + publish/archive lifecycle |
| `/admin/designs/:id/variants` | Variants | Garment types, colors, sizes, pricing, inventory |
| `/admin/products` | Legacy Products | Old product CRUD (pre-catalog system) |
| `/admin/customers` | Customers | Customer view |
| `/admin/size-charts` | Size Charts | Image upload per garment type |
| `/admin/payments` | Payments | Read-only payment list with filters |
| `/admin/exchange-rate` | Exchange Rate | KZT/USD and cross-rate management |
| `/admin/users` | Users | Registered user list (read-only) |

---

## 2. Critical Workflow Issues

### ADM-01 — Dashboard "Pending Payment" KPI links to wrong list (HIGH)
**File:** `frontend/src/admin/AdminDashboardPage.tsx`

The KPI card "Ожидают оплаты" shows the count of PENDING_PAYMENT orders and links to `/admin/orders`. However, `AdminOrdersPage` explicitly filters out PENDING_PAYMENT orders (`findByStatusNotInOrderByCreatedAtDesc(PENDING_PAYMENT, EXPIRED)`). Clicking the KPI opens a list that does not contain any of the orders the admin is trying to see. This is a direct contradiction between what the card promises and what the page shows.

**Fix:** Either link to a filtered orders view, or add a separate "Pending Payments" tab/section on the orders page.

### ADM-02 — Archive Design has NO confirmation dialog (HIGH)
**File:** `frontend/src/admin/AdminDesignsPage.tsx`

The "Архивировать" button calls the archive mutation immediately on click with no `window.confirm()` or any confirmation modal. Archiving a PUBLISHED design removes it from the public storefront immediately. An accidental click has immediate customer-facing impact.

### ADM-03 — Delete uses only window.confirm() with no cascade warning (HIGH)
All delete operations in `AdminDesignsPage`, `AdminCategoriesPage`, `AdminCollectionsPage`, and `AdminDesignVariantsPage` show only `window.confirm("Вы уверены?")` as the only guard before permanent deletion.

For groups and collections: if the item has children (designs, collection items), the deletion will fail at the database level with a generic "Не удалось удалить" error — giving the admin no indication of how many child records are blocking the deletion.

For designs: deletion is permanent. The design's images remain in MinIO storage (no cleanup).

---

## 3. Catalog Workflow (Group → Collection → Design → Variants)

**Correct workflow discovered by reading the code:**

1. Create group (`/admin/categories`) → group is active immediately
2. Create collection (`/admin/collections`) → assign to group → active immediately
3. Create design (`/admin/designs`) → auto-navigates to variants page after creation
4. In variants page: add garment type → assign colors and sizes → set KZT price → set inventory quantities
5. Return to designs list → click "Опубликовать" to go live

**Workflow gaps:**

| ID | Issue | Severity |
|----|-------|----------|
| ADM-04 | After creating a design, the page auto-navigates to variants — there is no "save and stay" option | LOW |
| ADM-05 | No success toast on create/save — form resets silently with no confirmation | MEDIUM |
| ADM-06 | No designs search or filter on the designs list — cannot quickly find DRAFT designs or designs by collection | MEDIUM |
| ADM-07 | Publish button has no confirmation dialog — accidental publish makes design live immediately | MEDIUM |
| ADM-08 | No "Unpublish" button in the designs list — admin must use a separate API call (the API exists, but the UI button is missing in the list; check if it exists at all) | HIGH |
| ADM-09 | Gallery images uploaded in `AdminDesignsPage` are stored in `design.gallery` but never displayed on the storefront `DesignPage` | HIGH (feature completeness) |

---

## 4. Inventory Management

### ADM-10 — Inventory save makes N×M sequential HTTP calls (HIGH)
**File:** `frontend/src/admin/AdminDesignVariantsPage.tsx`

The inventory save loop calls `setInventory()` sequentially for each color × size combination:

```ts
for (const c of garment.colors) {
  for (const s of garment.sizes) {
    await setInventory({ garmentId, colorId: c.id, sizeId: s.id, quantity })
  }
}
```

For a garment with 5 colors × 6 sizes = 30 sequential HTTP requests. This is slow (~3–6 seconds on a fast connection) and brittle — a network error partway through leaves inventory in a partially-updated state with no rollback. Quantities set before the failure remain, quantities after are not set.

**Fix needed:** A batch endpoint `PUT /api/v1/admin/designs/{id}/variants/{garmentId}/inventory` accepting an array of `{colorId, sizeId, quantity}`, called once.

### ADM-11 — Color/size dirty state not indicated (MEDIUM)
The color and size selection checkboxes can be modified without saving. There is no visual indication that the selection is "dirty." If the admin then clicks the active/inactive toggle, the toggle mutation also submits the unsaved color/size selection — effectively applying the unsaved changes without the admin intending to.

### ADM-12 — No price deletion (LOW)
Once a KZT price is set on a garment variant, there is no "remove price" option in the UI. The admin can only update the value. Setting it to 0 would pass the readiness check negatively (KZT price must be > 0 for publication).

---

## 5. Order Management

### ADM-13 — No pagination on orders list (HIGH)
`AdminOrdersPage` loads all non-pending/non-expired orders in a single request. As order volume grows, this causes increasing page load time and memory usage. At 10,000 orders, a response of ~500KB+ JSON returns on every admin page load.

### ADM-14 — No search or filter on orders list (MEDIUM)
The admin cannot filter orders by customer phone, name, or status. The only sort order is newest-first. Finding a specific order requires knowing the ID or scanning the list.

### ADM-15 — CDEK shipment create/cancel has no confirmation (MEDIUM)
**File:** `frontend/src/admin/AdminOrderDetailPage.tsx`

"Создать отправление" triggers a real CDEK API call without any confirmation dialog. "Отменить отправление" also triggers an irreversible CDEK API call without confirmation. These actions have real-world logistics implications.

### ADM-16 — Cannot view PENDING_PAYMENT orders in admin (MEDIUM)
The admin orders list excludes PENDING_PAYMENT orders by design. Combined with the dashboard KPI showing these orders, the admin has a count but no way to inspect them. For customer support scenarios (customer says "I paid but my order shows as pending"), the admin cannot see the order in the list.

---

## 6. Role Management

### ADM-17 — Role grant has no confirmation (MEDIUM)
**File:** `AdminDashboardPage.tsx`

`POST /auth/admin/grant` can promote any registered email to ADMIN. There is no confirmation dialog, no "type the email to confirm" pattern, and no audit log. This is a security-critical irreversible action (revoke exists, but the action is not undoable atomically).

### ADM-18 — revokeAdmin() scans full users table (MEDIUM, performance)
The revoke endpoint loads all users to count remaining admins, preventing removal of the last admin. Use a count query instead.

---

## 7. Payment Admin

### ADM-19 — Payment status values shown as raw enum strings (LOW)
`AdminPaymentsPage` shows statuses like `PENDING`, `SUCCEEDED`, `FAILED` as raw enum values. Human-readable labels ("Ожидает оплаты", "Успешно", "Ошибка") would be more usable.

### ADM-20 — No date range filter on payments (LOW)
All payments are loaded and displayed without pagination or date filtering.

---

## 8. Summary Table

| # | Page | Severity | Issue |
|---|------|----------|-------|
| 1 | Dashboard | HIGH | "Ожидают оплаты" KPI links to list that excludes those orders |
| 2 | Designs | HIGH | Archive has no confirmation dialog |
| 3 | All CRUD | HIGH | Delete only uses window.confirm(); no cascade warning |
| 4 | Designs | HIGH | No "Unpublish" button in designs list UI |
| 5 | Designs | HIGH | Gallery images never shown in storefront |
| 6 | Variants | HIGH | Inventory save: N×M sequential HTTP calls, no batching |
| 7 | Orders | HIGH | No pagination on orders list |
| 8 | Designs | MEDIUM | No success feedback on create/save |
| 9 | Designs | MEDIUM | No search or filter on designs list |
| 10 | Designs | MEDIUM | Publish button has no confirmation |
| 11 | Variants | MEDIUM | Color/size dirty state not indicated; toggle submits unsaved changes |
| 12 | Orders | MEDIUM | No search or filter on orders list |
| 13 | Orders | MEDIUM | Cannot view PENDING_PAYMENT orders |
| 14 | Order Detail | MEDIUM | CDEK actions have no confirmation dialogs |
| 15 | Dashboard | MEDIUM | Role grant has no confirmation |
| 16 | Dashboard | MEDIUM | revokeAdmin() full user table scan |
| 17 | Payments | LOW | Status values shown as raw enum strings |
| 18 | Payments | LOW | No date range filter or pagination |
| 19 | Categories | LOW | No active/inactive toggle per row |
| 20 | Collections | LOW | Collection cover images not shown in admin list |
