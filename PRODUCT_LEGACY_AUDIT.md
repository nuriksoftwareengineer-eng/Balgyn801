# Product Legacy Audit

**Date:** 2026-06-20  
**Verdict: B — Deprecate (do not remove yet)**

---

## Files Referencing Product

### Backend (18 Java files)
| File | Role |
|------|------|
| `Product.java` | Entity |
| `ProductRepository.java` | Repository |
| `ProductService.java` | Service interface |
| `ProductServiceImpl.java` | Service implementation |
| `ProductController.java` | REST controller |
| `ProductApi.java` | API interface |
| `ProductMapper.java` | Mapper |
| `ProductResponse.java` | Response DTO |
| `CreateProductRequest.java` | Request DTO |
| `ProductColorOption.java` | Embedded color type |
| `StoreCategories.java` | Category constants |
| `OrderItem.java` | Holds `product_id` FK (legacy order path) |
| `OrderServiceImpl.java` | Routes orders by `designGarmentId != null` check |
| `CdekDeliveryService.java` | Reads product packaging dimensions |
| `CdekOrderItemRequest.java` | DTO used in CDEK shipping |
| `OrderItemResponse.java` | Includes product data in order response |
| `OrderItemRequest.java` | Accepts `productId` field |
| `MediaStorageService.java` | Handles product image upload path |

### Frontend (5 files)
| File | Role |
|------|------|
| `ProductPage.tsx` | Legacy product detail page |
| `ProductCatalogGrid.tsx` | Legacy product grid widget |
| `CatalogParamPage.tsx` | Dispatches numeric slugs to `ProductPage` |
| `AdminLayout.tsx` | Had nav link (removed in Phase 6) |
| `AdminDashboardPage.tsx` | Has quick-link to `/admin/products` |

---

## Why NOT Remove Yet

1. **OrderItem.product_id FK is live.** Existing orders in the database may reference products. Dropping the `Product` entity would break order history and admin order detail views for those records.

2. **Legacy URL routing.** `CatalogParamPage` routes `/catalog/42` (numeric slugs) to `ProductPage`. If any existing products were ever shared as links, those URLs remain functional.

3. **CDEK integration.** `CdekDeliveryService` reads product packaging dimensions (`weightGrams`, `lengthCm`, etc.) for legacy order items. Removing Product would break CDEK shipment creation for legacy orders.

---

## Safe Deprecation Steps (Phase 6+)

**Immediately (done in Phase 6):**
- [x] Remove "Товары (legacy)" from `AdminLayout` sidebar nav — admins can no longer navigate there directly

**Next sprint:**
- [ ] Add deprecation notice to `AdminProductsPage` header: "Создание новых товаров отключено. Используйте Дизайны."
- [ ] Disable the product creation form in `AdminProductsPage` (keep the list read-only)

**When products table is confirmed empty (or all legacy orders archived):**
- [ ] Drop `product_id` FK from `order_items` via Flyway migration (set FK nullable first, then null-fill, then drop column)
- [ ] Remove: `Product.java`, `ProductRepository`, `ProductService*`, `ProductController`, `ProductApi`, `ProductMapper`, `CreateProductRequest`, `ProductResponse`, `ProductColorOption`, `StoreCategories`
- [ ] Remove: `ProductPage.tsx`, `ProductCatalogGrid.tsx`, numeric slug dispatch in `CatalogParamPage`
- [ ] Remove: product image path in `MediaStorageService`
- [ ] Remove: product CDEK dimension handling in `CdekDeliveryService`

---

## Recommendation

Keep the `products` table and all legacy code in read-only mode. Block new product creation in the admin UI. Schedule full removal once the `products` table has zero rows or all legacy `order_items.product_id` references are archived.
