# MASTER_PRODUCTION_BACKLOG.md
> Balgyn — Pre-Launch Master Backlog · 2026-06-21
>
> **Priority definitions:**
> - **P0 — Launch Blocker:** Must be fixed before accepting real money from real customers
> - **P1 — Launch Critical:** Must be fixed within the first week of operation; high risk if ignored
> - **P2 — Launch Important:** Fix within the first month; meaningful quality/security impact
> - **P3 — Post-Launch:** Backlog improvements; low immediate risk

---

## P0 — Launch Blockers (Fix Before Go-Live)

| ID | Domain | Module | Issue | Effort |
|----|--------|--------|-------|--------|
| P0-01 | Security | Infrastructure | **No HTTPS** — nginx port 80 only; all payment data, JWT tokens, and customer data transmitted in plain text | M |
| P0-02 | Security | Secrets | **Real credentials in `.env.example`** — FreedomPay secret key and PayPal sandbox credentials committed to git; rotate `FREEDOMPAY_SECRET_KEY` immediately | S |
| P0-03 | Payment | Backend | **Late payment re-confirms CANCELLED orders** — `handleFreedomPayCallback` and PayPal capture handler check `status == PENDING_PAYMENT` but not `status != CANCELLED`; a late payment after admin cancellation revives the order | S |
| P0-04 | Delivery | Backend | **No CDEK webhook signature verification** — `POST /delivery/cdek/webhook` accepts all requests without any auth; forged status updates can mark orders as DELIVERED | M |
| P0-05 | Payment | Frontend | **PaymentReturnPage broken for Freedom Pay** — FP redirects to `/payment-return` which only handles PayPal; FP users see an infinite spinner and cannot complete checkout | S |
| P0-06 | UX | Frontend | **CartPage "Итого" total omits delivery fee** — grand total shows item subtotal only; actual charge from backend includes delivery; customers see a lower total than what they are charged | S |

---

## P1 — Launch Critical (Fix Within First Week)

| ID | Domain | Module | Issue | Effort |
|----|--------|--------|-------|--------|
| P1-01 | Security | Backend | **Stateless refresh tokens with no revocation** — stolen token valid for 14 days with no way to invalidate without key rotation | L |
| P1-02 | Security | Backend | **CdekWebhookServiceImpl.findAll()** — full table scan on every CDEK webhook; add `findByCdekOrderUuid()` with DB index | S |
| P1-03 | Security | Backend | **`POST /payments/paypal/cancel/{id}` unauthenticated** — any caller can cancel a pending PayPal payment by guessing the order ID | S |
| P1-04 | Payment | Backend | **PayPal webhookId defaults to blank** — if `PAYPAL_WEBHOOK_ID` env var is not set, all PayPal webhooks are rejected; no PayPal payments confirm | S |
| P1-05 | Payment | Backend | **PayPal CAPTURE event order ID extraction fallback** — if supplementary path is missing, fallback uses capture ID as order ID; webhook silently discarded | S |
| P1-06 | Security | Backend | **No CSP header** — no Content-Security-Policy anywhere; no browser XSS protection depth | S |
| P1-07 | Auth | Frontend | **ProfilePage admin link never shows** — `ROLE_ADMIN` check fails because JWT contains `ADMIN` (no prefix); admins cannot navigate to admin panel from their profile | XS |
| P1-08 | Auth | Backend | **No rate limiting on `/auth/refresh`** — stolen refresh token can generate unlimited access tokens | S |
| P1-09 | Operations | Infrastructure | **No backup strategy** — no automated pg_dump, no MinIO backup; one disk failure = total data loss | M |
| P1-10 | Payment | Backend | **FreedomPay stub mode silently active if FREEDOMPAY_MERCHANT_ID is blank** — orders proceed through fake checkout; add startup fail-fast on non-test profile | S |
| P1-11 | UX | Frontend | **Auth cold-start session loss** — expired access token on page load clears session without attempting HttpOnly cookie refresh | M |
| P1-12 | Checkout | Frontend | **No phone number validation** — any non-empty string accepted; invalid phones sent to CDEK and carrier | S |
| P1-13 | Performance | Backend | **PessimisticLockingFailureException returns HTTP 500** — inventory lock timeout shows as server error; should be 409 with retry message | S |
| P1-14 | Localization | Frontend | **Duplicate `product` key in all 3 locale files** — `product.soldOut` and first `product.addToCart` silently overridden; raw key strings shown to users | S |

---

## P2 — Launch Important (Fix Within First Month)

| ID | Domain | Module | Issue | Effort |
|----|--------|--------|-------|--------|
| P2-01 | Performance | Backend | **Admin orders list N+1** — `getAll()` triggers ~3N+1 queries with no EntityGraph; add `@EntityGraph` to order list query | S |
| P2-02 | Performance | Backend | **No pagination on admin endpoints** — orders, designs, customers, payments all return unbounded lists | M |
| P2-03 | Performance | Backend | **getDesignBySlug N inventory queries** — N SELECT per garment; add batch inventory load | S |
| P2-04 | Performance | Backend | **DesignGarment lazy collections** — colors/sizes/prices load N+1 per garment; add EntityGraph | S |
| P2-05 | Performance | Frontend | **Admin inventory save: N×M sequential HTTP calls** — add batch inventory endpoint + replace loop with single call | M |
| P2-06 | Auth | Backend | **revokeAdmin() full table scan** — `findAll()` to count admins; replace with count query | S |
| P2-07 | Auth | Backend | **POST /api/v1/catalog/\*\*/reviews NPE risk** — no explicit `@PreAuthorize` on review POST; add annotation for safety | XS |
| P2-08 | Admin UX | Frontend | **Dashboard KPI "pending payment" links to wrong list** — orders list excludes PENDING_PAYMENT orders | S |
| P2-09 | Admin UX | Frontend | **Archive design has no confirmation** — accidental click removes design from storefront immediately | S |
| P2-10 | Admin UX | Frontend | **CDEK create/cancel has no confirmation dialog** — irreversible real logistics actions | S |
| P2-11 | Admin UX | Frontend | **No success feedback on admin CRUD** — create/save resets form silently | S |
| P2-12 | Localization | Frontend | **CustomDesignPage has no i18n** — entire page hardcoded Russian | M |
| P2-13 | Localization | Frontend | **Date locale hardcoded "ru-RU" in OrderHistoryPage** | S |
| P2-14 | Localization | Frontend | **No fallbackLng: "ru" in i18next config** — missing keys show raw key strings | XS |
| P2-15 | UX | Frontend | **Recovery banner "Отменить заказ" doesn't cancel backend order** — misleading label | S |
| P2-16 | UX | Frontend | **OrderHistoryPage uses useEffect/useState** — not React Query; no retry or caching | S |
| P2-17 | Operations | Infrastructure | **No Spring Actuator** — Docker healthcheck uses business endpoint; no proper health, metrics, or readiness probes | S |
| P2-18 | Operations | Infrastructure | **No log rotation** — Docker json-file driver accumulates without size limit | XS |
| P2-19 | Operations | Infrastructure | **No structured logging** — plain text makes log aggregation difficult | M |
| P2-20 | Security | Infrastructure | **spring.datasource.password fallback '1234'** — remove default and use `:?` | XS |
| P2-21 | Database | Backend | **inventory.quantity has no CHECK (quantity >= 0)** | XS |
| P2-22 | Database | Backend | **V7 ON DELETE SET NULL** — deleting catalog items silently nullifies historical order references; change to RESTRICT | M |
| P2-23 | Database | Backend | **No DB index on cdek_order_uuid** — add index; required for BUG-B04 fix | XS |
| P2-24 | Admin UX | Frontend | **No "Unpublish" button in designs list** — API endpoint exists; UI missing | S |
| P2-25 | Admin UX | Frontend | **Gallery images never displayed in storefront** — admin uploads gallery but DesignPage only shows mainImageUrl | M |
| P2-26 | Admin UX | Frontend | **Collection cover images not shown in storefront** — coverImageUrl field exists but not rendered | S |
| P2-27 | Payment | Backend | **FreedomPay callback pg_result=null maps to PENDING** — add explicit null→FAILED mapping | S |

---

## P3 — Post-Launch Improvements

| ID | Domain | Issue | Effort |
|----|--------|-------|--------|
| P3-01 | Performance | BestSellers loads all designs to show 4; add `?limit=4` or featured endpoint | S |
| P3-02 | Performance | AdminDashboardPage count queries load full data; add `/admin/stats` summary endpoint | S |
| P3-03 | UX | TrackOrderPage is a stub; implement real CDEK tracking integration | L |
| P3-04 | UX | No quantity selector on DesignPage; add before add-to-cart | M |
| P3-05 | UX | ProfilePage has no password change or account deletion | M |
| P3-06 | UX | AdminOrdersPage has no search or filter | M |
| P3-07 | UX | AdminDesignsPage has no search or filter | S |
| P3-08 | UX | No "continue shopping" link after add-to-cart (auto-navigates to cart) | S |
| P3-09 | Database | Customer deduplication; no unique constraint on phone | M |
| P3-10 | Database | orders.status and orders.delivery_type have no NOT NULL or CHECK constraints | S |
| P3-11 | Database | No REFUNDED order status; refund only tracked at payment level | M |
| P3-12 | Database | order_items missing CHECK (product_id IS NOT NULL OR design_garment_id IS NOT NULL) | S |
| P3-13 | Auth | No email verification on registration; any email string accepted | M |
| P3-14 | Auth | No `@Email` annotation on RegisterRequest | XS |
| P3-15 | Localization | TrackOrderPage has no i18n | M |
| P3-16 | Admin UX | Admin delete has no cascade warning; show count of child items before confirming | S |
| P3-17 | Admin UX | No active/inactive toggle per row in categories and collections | S |
| P3-18 | Admin UX | Payment status values shown as raw enum strings; add human-readable labels | S |
| P3-19 | Operations | No CI/CD pipeline; add GitHub Actions for test + build | M |
| P3-20 | Operations | Rate limiting uses in-memory only; add Redis for multi-instance safety | L |
| P3-21 | Security | Admin role grant has no confirmation dialog; add "type email to confirm" | S |
| P3-22 | Security | No audit log for role grants/revocations | M |
| P3-23 | Security | No `Permissions-Policy` header | XS |
| P3-24 | SEO | No OG/social meta tags on any page | M |
| P3-25 | SEO | SEO title fallbacks hardcoded Russian on DesignPage and CollectionPage | S |
| P3-26 | Code | Dead Zustand stores (useCartStore, useCheckoutStore) — remove and clean up balgyn-cart-v1 key | S |
| P3-27 | Code | GARMENT_LABELS includes types absent from admin GARMENT_TYPES selector | XS |
| P3-28 | Code | ConstraintViolationException returns 500 instead of 400; add handler | S |
| P3-29 | Code | DataIntegrityViolationException handler leaks internal constraint name | XS |
| P3-30 | Code | PayPalApiException leaks internal error message in 502 response | XS |
| P3-31 | Code | OrderExpiryService batch expiry should be per-order transactions | M |
| P3-32 | Code | V22 migration is a no-op SELECT 1 — replace with schema comment | XS |
| P3-33 | MinIO | No lifecycle policy; uploaded images accumulate without cleanup | S |
| P3-34 | MinIO | No pre-signed URLs; all media permanently public with no revocation | M |

---

## Effort Scale
- **XS** = < 30 minutes (config change, one-liner, annotation)
- **S** = 1–4 hours (small code change, single class/file)
- **M** = half to full day (multiple files, new endpoint + migration)
- **L** = 2–5 days (new module, significant architecture change)
