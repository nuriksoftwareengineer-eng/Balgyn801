# PERFORMANCE_REVIEW.md
> Balgyn — Pre-Launch Performance Review · 2026-06-21

---

## 1. Frontend Performance

### 1.1 Code Splitting (POSITIVE)

All 30+ pages use `React.lazy()` + `Suspense`:

```ts
const DesignPage = lazy(() => import("@/pages/DesignPage").then(m => ({ default: m.DesignPage })));
// ... repeated for all pages
```

Each page loads its own chunk only when navigated to. Initial bundle is limited to the app shell, router, and shared providers.

**Rating: Excellent.** The most important frontend performance pattern is correctly implemented.

### 1.2 Dead Code in Bundle (MEDIUM)

Two Zustand stores are bundled but never imported:
- `stores/cart.store.ts` — `useCartStore` (Zustand, persisted to `balgyn-cart-v1`)
- `stores/checkout.store.ts` — `useCheckoutStore` (in-memory)

These add bundle weight and their persistence keys pollute localStorage. They should be deleted.

### 1.3 Framer Motion Bundle Weight (LOW)

`framer-motion@11.18` (~80–100KB gzipped) is included for cart drawer animations and a few page transitions. This is a significant dependency for a relatively light animation requirement. If bundle size is a concern, consider replacing with CSS transitions.

### 1.4 React Query Stale Times (MEDIUM)

`staleTime` is unset or set to low values on most queries, causing re-fetches on every component mount. Key examples:

- Catalog groups, collections, and design detail are fetched fresh on every navigation. Catalog data changes only when the admin publishes a design. A `staleTime: 5 * 60 * 1000` (5 min) on catalog queries would eliminate redundant fetches without compromising freshness.
- `sizeCharts` uses `staleTime: 10 * 60 * 1000` — good pattern, extend to catalog queries.

### 1.5 BestSellers Fetches All Designs Then Slices to 4 (MEDIUM)

`HomePage` BestSellers section calls `getCatalogDesigns()` (all published designs) and slices the first 4. As the catalog grows to 100+ designs, this fetches the entire catalog on every homepage load to display 4 items.

**Fix:** Either create a `?limit=4` parameter on the catalog endpoint, or use a dedicated "featured designs" endpoint.

### 1.6 AdminDashboardPage — Count Queries Load All Data (LOW)

The dashboard loads all orders (`getOrders()`), all customers (`getCustomers()`), and all designs (`listDesigns()`) to count them for KPI cards. These three queries run in parallel via separate `useQuery` hooks (correct pattern), but each returns the full dataset just to display a count.

**Fix:** Create lightweight `/api/v1/admin/stats` endpoint returning `{ orderCount, customerCount, publishedDesignCount }` in a single query.

---

## 2. Backend Performance

### 2.1 N+1 Query: Admin Orders List (HIGH)

**File:** `OrderServiceImpl.getAll()` + `OrderMapper`

```java
orderRepository.findByStatusNotInOrderByCreatedAtDesc(...)  // 1 query
    .stream()
    .map(orderMapper::toResponse)  // each order: loads customer (1), deliveryAddress (1), orderItems (N)
    .toList();
```

With 500 orders averaging 2 items each: `1 + 500*2 + 500 + 500 = ~1,501 queries` per admin page load.

**Fix:** Add `@EntityGraph(attributePaths = {"customer", "deliveryAddress", "orderItems", "orderItems.designGarment", "orderItems.designGarment.design"})` to the order list query.

### 2.2 N+1 Query: Design Detail Page (MEDIUM)

**File:** `CatalogStorefrontServiceImpl.getDesignBySlug()`

```java
garmentRepository.findByDesign_IdAndActiveTrue(design.getId())  // 1 query
    .stream()
    .map(garment -> {
        List<Inventory> rows = inventoryRepository.findByDesignGarment_Id(garment.getId());  // N queries
        // also accesses garment.colors, garment.sizes, garment.prices lazily
    })
```

For a design with 3 garment variants: 1 design query + 3 inventory queries + 3×3 lazy collection queries = ~13 queries per product page view.

**Fix:** Use `inventoryRepository.findByDesignGarment_IdIn(garmentIds)` to batch-load all inventory in one query, then group in Java.

### 2.3 No Pagination on Admin Endpoints (HIGH)

All admin list endpoints return unbounded `List<>`:

| Endpoint | Repository Query | Risk |
|----------|-----------------|------|
| `GET /admin/catalog/designs` | `findAllWithGarments()` | All designs in one response |
| `GET /order` | `findByStatusNotInOrderByCreatedAtDesc()` | All orders in one response |
| Payments list | `paymentRepository.findAll()` | All payments in one response |
| Customers list | `customerRepository.findAll()` | All customers in one response |
| Users list | `appUserRepository.findAll()` | All users in one response |

At current scale these are fast. At 10,000 orders these will cause OOM or multi-second response times.

**Fix:** Add `Pageable` parameter to all admin repositories and update endpoints to accept `page` and `size` query parameters.

### 2.4 CDEK Webhook: Full Table Scan on Every Call (CRITICAL)

**File:** `CdekWebhookServiceImpl.handle()`

```java
shipmentRepository.findAll()  // loads ALL cdek_shipments
    .stream()
    .filter(s -> s.getCdekOrderUuid().equals(uuid))  // O(N) filter
```

Every incoming CDEK status update loads the entire `cdek_shipments` table into memory. As shipments accumulate, this becomes O(N) memory and processing per webhook.

**Fix:**
1. Add `Optional<CdekShipment> findByCdekOrderUuid(String uuid)` to `CdekShipmentRepository`
2. Add DB index: `CREATE INDEX idx_cdek_shipments_uuid ON cdek_shipments(cdek_order_uuid)`

### 2.5 Auth Admin Scan (MEDIUM)

**File:** `AuthServiceImpl.listUsers()` and `revokeAdmin()`

Both call `appUserRepository.findAll()` and process in-memory. With 100,000 registered users, `listUsers()` would load the entire users table on every call from `AdminUsersPage`.

**Fix:** Use count query for admin validation; use `findAll(Pageable)` for user listing.

### 2.6 DesignGarment Lazy Collections (MEDIUM)

`DesignGarment.colors` (ManyToMany), `.sizes` (ManyToMany), and `.prices` (OneToMany) are all LAZY. When accessed in the storefront mapper, each fires a separate SELECT per garment. `DesignGarmentRepository.findByDesign_IdAndActiveTrue()` has no `@EntityGraph`.

**Fix:** Add `@EntityGraph(attributePaths = {"colors", "sizes", "prices"})` to this query.

---

## 3. Inventory Save Loop (HIGH)

**File:** `frontend/src/admin/AdminDesignVariantsPage.tsx`

```ts
for (const c of garment.colors) {
  for (const s of garment.sizes) {
    await setInventory(...)  // sequential, one request at a time
  }
}
```

For 5 colors × 6 sizes = 30 sequential HTTP requests. At 100ms/request on a local network = 3 seconds. On a real server = 5–10 seconds. If any request fails, partial state is silently left.

**Fix needed on backend AND frontend:**
1. Backend: add `PUT /api/v1/admin/designs/{designId}/variants/{garmentId}/inventory/batch` accepting `[{colorId, sizeId, quantity}]`
2. Frontend: replace the loop with a single mutation call

---

## 4. Exchange Rate Service

**File:** `ExchangeRateServiceImpl.refreshFromProvider()`

- Scheduled hourly — correct, NBK rate changes daily
- Uses `RestClient` with a 3000ms timeout — appropriate
- Parses NBK RSS XML in-memory — acceptable for this feed size
- Caches result in DB with upsert — correct pattern
- Rates fall back to bootstrap values if DB is empty — correct

**Rating: Good.** No performance concerns here.

---

## 5. Positive Patterns

| Area | Pattern | Notes |
|------|---------|-------|
| Code splitting | `React.lazy()` + `Suspense` on all pages | Excellent |
| EntityGraph on DesignRepository | All storefront queries eager-load collection + catalogGroup | Prevents catalog N+1 |
| Admin designs query | `@EntityGraph(attributePaths = "garments")` | Prevents garment count N+1 |
| Catalog read transactions | `@Transactional(readOnly=true)` on CatalogStorefrontServiceImpl | Correct optimization |
| Payment idempotency | DB unique constraint prevents double-processing | Correct |
| React Query parallel hooks | AdminDashboardPage uses 3 parallel queries | Correct |
| Size chart stale time | 10-minute staleTime on size charts query | Good pattern to extend |
| Pessimistic lock | Inventory reserved with 3000ms timeout + PESSIMISTIC_WRITE | Prevents oversell |
| Nginx static asset caching | 1-year immutable for hashed JS/CSS; no-cache for HTML | Correct |

---

## 6. Summary

| Priority | Issue | Estimated Query Count at Scale |
|----------|-------|-------------------------------|
| CRITICAL | CDEK webhook: findAll() on every call | O(N shipments) per webhook |
| HIGH | Admin orders list N+1 | ~3N queries per page load |
| HIGH | No pagination on admin lists | Full table in RAM per page load |
| HIGH | Inventory save loop: N×M sequential HTTP calls | 30 per save operation |
| MEDIUM | getDesignBySlug N inventory queries | N+1 per product page |
| MEDIUM | DesignGarment lazy collections | 3 extra queries per garment |
| MEDIUM | BestSellers loads all designs to show 4 | Full catalog per homepage load |
| LOW | Dead Zustand stores in bundle | Dead weight |
| LOW | Dashboard KPI loads full data for counts | Full tables per dashboard load |
