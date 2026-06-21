# Production Readiness Report — Balgyn

**Date:** 2026-06-20  
**Auditor:** Claude Code (Phase 3)  
**Backend tests:** 84 ✅ / 0 ❌  
**Audit method:** 5 parallel deep-read agents + direct code inspection. No assumptions — everything verified.

---

## Overall Score: 62 / 100

**Not ready for production.** The core commerce engine is solid, but three critical blockers must be resolved before launch: stolen refresh tokens cannot be invalidated server-side, ~80% of the frontend is hardcoded Russian with no translation, and the frontend runs on a dev server (Vite) in Docker instead of a production build.

---

## Scoring Breakdown

| Area | Score | Notes |
|------|-------|-------|
| Payment flow | 14/20 | Signature, replay, double-capture all protected. Cancel/amount bugs fixed. |
| Security | 11/20 | No default secrets. But refresh tokens are stateless — can't revoke. |
| Database | 6/10 | Critical indexes now added (V24). Nullable critical columns remain. |
| i18n / UX | 4/10 | Infrastructure exists. CartPage, DesignPage, auth pages still hardcoded RU. |
| SEO | 3/5 | robots.txt, sitemap, OG tags added. All client-side only (no SSR). |
| Admin panel | 6/10 | Full CRUD. No image upload, no refunds, no EUR/RUB manual edit. |
| Docker / Infra | 7/10 | Near-one-command deploy. Frontend uses Vite dev server (not production). |
| Demo data | 4/5 | 28 designs across 14 collections. No images (NULL main_image_url). |
| Error handling | 3/5 | Payment pages, 404 page present. Many pages show raw errors. |
| CDEK integration | 4/5 | Live API confirmed. Webhook signature not verified. |

---

## Critical Blockers (must fix before launch)

### BLOCKER 1 — Refresh tokens cannot be revoked
**Files:** `AuthServiceImpl.java`, no `refresh_tokens` table  
**Risk:** HIGH — stolen refresh token stays valid for 14 days

The refresh flow is pure JWT: the server validates signature and expiry only. After logout, the cookie is cleared client-side, but a captured token remains usable until it expires.

**Fix required:** Create a `refresh_tokens` table with `(token_hash, user_id, expires_at)`. On refresh, validate the row exists, delete it, issue a new token and row. On logout, delete the row. This blocks a stolen-token attack that a session-based design would not have.

**Estimated effort:** 1 day.

---

### BLOCKER 2 — Frontend is not production-ready (Vite dev server in Docker)
**Files:** `docker-compose.yml:81`, `Dockerfile`  
**Risk:** HIGH — `npm run dev` in Docker leaks source maps, has no compression, and is not designed for production traffic

The frontend container runs `npm ci && npm run dev -- --host 0.0.0.0 --port 5173`. This is a hot-reload development server.

**Fix required:** Add an nginx stage to the Dockerfile (or a second `frontend.Dockerfile`) that runs `npm run build` and serves `/dist` via nginx. Update docker-compose to use the production image.

**Estimated effort:** 2 hours.

---

### BLOCKER 3 — ~80% of frontend strings are hardcoded Russian
**Files:** `CartPage.tsx`, `DesignPage.tsx`, `LoginPage.tsx`, `RegisterPage.tsx`, `SiteFooter.tsx`, `home/*`, and 12 more files  
**Risk:** HIGH — The store cannot serve English or Kazakh customers, despite the language switcher being present in the navbar

The language switcher exists and works for the keys that are translated (nav, payment pages, order status). But `CartPage.tsx` alone has ~100+ hardcoded Russian strings in the checkout flow. Switching to EN or KK shows Russian labels throughout.

**Most critical untranslated pages:**
- `CartPage.tsx` — entire 5-step checkout flow in Russian
- `DesignPage.tsx` — product selection (garment, color, size, add to cart)
- `LoginPage.tsx` / `RegisterPage.tsx` — auth flow
- `SiteFooter.tsx` — footer links and text
- All `home/*` widgets — hero, about band, categories, value strip

**Fixed in Phase 3:** CartDrawer, OrderHistoryPage, plus ~30 new keys added covering cart, auth, design, and error sections.

**Estimated effort to fully translate:** 3–5 days.

---

## High Priority Issues

### H1 — PayPal captured amount not validated (FIXED in Phase 3)
**File:** `PayPalServiceImpl.java:captureOrder()`  
Amount validation added in Phase 3. `captureOrder` now rejects if the PayPal captured amount differs from the stored amount by more than $0.02.

### H2 — PayPal cancel left PENDING payment forever (FIXED in Phase 3)
**File:** `PaymentCancelledPage.tsx`, `PayPalOrderController.java`  
Added `POST /api/v1/payments/paypal/cancel/{paypalOrderId}`. Frontend now calls it when the user lands on the cancel page with a `?token=` parameter. Payment row is marked CANCELLED.

### H3 — Database missing critical indexes (FIXED in Phase 3 via V24)
**File:** `V24__performance_indexes_and_constraints.sql`  
Added indexes on `order_items.order_id`, `payments.order_id`, `order_history.order_id`, `user_roles.user_id`, `processed_webhook_events.payment_id`. Added unique constraints on `delivery_addresses.order_id` and `cdek_shipments.order_id`. Added partial unique on SUCCEEDED payments per order+provider. Added unique on delivery tariff brackets.

### H4 — DB: critical columns nullable (NOT FIXED)
**File:** `V1__baseline.sql`  
`orders.status`, `order_items.quantity`, `order_items.unit_price` are all nullable. A null status silently breaks the order state machine. Adding NOT NULL constraints retroactively requires careful data verification.

### H5 — COOKIE_SECURE default false (FIXED in Phase 3)
**File:** `application-prod.properties`  
`app.security.cookie-secure=true` now forced in prod profile. Refresh cookie will no longer be sent over HTTP when `SPRING_PROFILES_ACTIVE=prod`.

### H6 — Exchange rate refresh was daily, not hourly (FIXED in Phase 3)
**File:** `application.properties:83`  
Changed `0 0 6 * * *` (daily at 6am) to `0 0 * * * *` (hourly). The `@Scheduled` annotation's fallback was being overridden by the properties file default.

### H7 — CDEK webhook not signature-verified
**File:** `SecurityConfig.java:138`  
`POST /api/v1/delivery/cdek/webhook` is `permitAll()` but no signature verification was found. Any actor can POST to this endpoint and trigger delivery state changes.

**Recommendation:** Verify with CDEK documentation whether they provide a webhook signature. If yes, implement verification. If no, add IP allowlist.

### H8 — EUR/RUB exchange rates not admin-editable
**File:** `AdminExchangeRatePage.tsx`  
The admin panel only allows editing KZT/USD. EUR and RUB are fetched automatically from NBK but cannot be manually frozen or overridden if the API is down.

---

## Medium Priority Issues

### M1 — colors and sizes have no UNIQUE constraint at DB level
**File:** `V4__colors_sizes.sql`  
Admin can insert duplicate color names ("Black" twice). UI then shows two identically-named colors in the picker. Simple `CREATE UNIQUE INDEX` migration needed.

### M2 — delivery_tariffs has no UNIQUE constraint on (kind, upto_kg)
**File:** `V16__delivery_tariffs.sql` (FIXED in V24 partial unique)  
Added in V24. Was causing potential non-deterministic tariff pricing.

### M3 — `order_items.design_garment_id ON DELETE SET NULL`
**File:** `V7__order_items_design_support.sql:6`  
Deleting a design garment silently nulls historical order items. Should be RESTRICT (prevent deletion of garments that have orders) to preserve order history integrity.

### M4 — SEO is entirely client-side
**File:** `useSeoMeta.ts`, `index.html`  
All meta tags (description, canonical, OG) are set via `useEffect`. Search engine crawlers that don't execute JavaScript will see only `<title>Balgyn — Вышивка на одежде, Алматы</title>`. For a proper SEO story, React Server-Side Rendering or prerendering is needed.

### M5 — Swagger enabled in Docker by default
**File:** `docker-compose.yml:20`  
`SWAGGER_ENABLED=true` is the compose default. A deployment that doesn't set `SPRING_PROFILES_ACTIVE=prod` will expose the full API documentation at `/swagger-ui.html`. The commented-out `# - SPRING_PROFILES_ACTIVE=prod` line is easy to miss.

**Recommendation:** Set `SWAGGER_ENABLED=false` as the compose default and let developers opt in, OR flip the commented line to active.

### M6 — MinIO console exposed on port 9001
**File:** `docker-compose.yml:64`  
Port 9001 (MinIO admin console) is bound to `0.0.0.0`. In production, this must be behind a firewall or removed from the port binding. Any actor with network access can reach the MinIO management UI.

### M7 — No payment refund capability
**File:** No refund endpoint exists  
Neither Freedom Pay nor PayPal implement refund APIs. Admins have no way to initiate a refund through the system. Must call Freedom Pay/PayPal APIs manually for any refund.

### M8 — No image upload in admin panel
**File:** `AdminDesignsPage.tsx`, `MediaUploadController.java`  
The `/api/v1/media/upload` endpoint exists and works, but no UI file picker is wired to it. All 28 seeded designs have `main_image_url = NULL`, so the catalog shows placeholder initials instead of real images.

### M9 — `order_items.design_garment_id` ON DELETE SET NULL silently corrupts history
See M3.

---

## Security Findings

| Finding | Severity | Status |
|---------|----------|--------|
| Refresh tokens not server-side revocable | HIGH | NOT FIXED — requires session store |
| Real PayPal + FreedomPay credentials in `.env` | HIGH | gitignored; rotate if `.env` was ever committed |
| `spring.datasource.password=1234` in source | MEDIUM | FIXED — now reads from `SPRING_DATASOURCE_PASSWORD` env var |
| `COOKIE_SECURE=false` default | MEDIUM | FIXED — prod profile forces `true` |
| TRUST_PROXY=false by default | MEDIUM | FIXED — prod profile forces `true` |
| Swagger accessible in Docker | MEDIUM | NOT FIXED — set `SWAGGER_ENABLED=false` in compose |
| MinIO console on :9001 open | MEDIUM | NOT FIXED — firewall / remove port binding in prod |
| CDEK webhook unverified | MEDIUM | NOT FIXED |
| CORS restricted to localhost | LOW | OK — needs `ALLOWED_ORIGINS` set in prod |
| HSTS enabled, no HTTP redirect | LOW | OK — expected at proxy layer |

---

## Payment Findings

| Finding | Severity | Status |
|---------|----------|--------|
| PayPal cancel left PENDING forever | HIGH | FIXED — `/paypal/cancel/{id}` endpoint added |
| PayPal captured amount not validated | HIGH | FIXED — amount comparison in `captureOrder` |
| Freedom Pay failure URL was `/payment/failure` | HIGH | FIXED — `.env` corrected to `/payment/failed` |
| Freedom Pay no "cancelled" result code | MEDIUM | NOT FIXED — `pg_result` ≠ "1" or "0" stays PENDING |
| Double SUCCEEDED payment possible | MEDIUM | FIXED — V24 partial unique index added |
| No refund capability | MEDIUM | NOT FIXED |
| PayPal idempotency: webhook before/after capture | LOW | OK — both paths are safe |

---

## Docker Findings

| Finding | Severity | Status |
|---------|----------|--------|
| Frontend uses Vite dev server | CRITICAL | NOT FIXED — blocking for production |
| No `app` health check | HIGH | FIXED — curl to `/api/v1/exchange-rates` |
| `frontend` started before backend was ready | HIGH | FIXED — `depends_on: app: condition: service_healthy` |
| MinIO credentials hardcoded as minioadmin | HIGH | FIXED — now `${MINIO_ROOT_USER:-minioadmin}` |
| Duplicate env vars (FREEDOMPAY, PAYPAL) | MEDIUM | FIXED — duplicates removed |
| No `docker-compose.prod.yml` | MEDIUM | NOT FIXED — prod secrets need a separate strategy |
| No `SPRING_PROFILES_ACTIVE=prod` in compose | MEDIUM | NOT FIXED — commented out, easy to miss |
| MinIO console port 9001 public | MEDIUM | NOT FIXED |
| Tests skipped in Docker build | LOW | OK for now — acceptable in CI separate step |

---

## Admin Panel Findings

| Area | Status |
|------|--------|
| Auth guard (RequireAdmin) | ✅ Working |
| Order list + status update | ✅ Working |
| Payment list (FreedomPay + PayPal) | ✅ Working |
| Full catalog CRUD | ✅ Working |
| Exchange rate (KZT/USD set + freeze) | ✅ Working |
| Exchange rate (EUR/RUB) | ❌ View only — no manual set |
| Image upload for designs | ❌ Missing — no file picker |
| Payment refunds | ❌ Not implemented |
| CDEK shipment creation UI | ❌ API only, no admin form |
| Bulk order status update | ❌ One-by-one only |

---

## Phase 3 Changes Made

| Change | File(s) |
|--------|---------|
| Fixed `.env` failure URL (`/payment/failure` → `/payment/failed`) | `.env` |
| Exchange rate cron: daily → hourly | `application.properties:83` |
| DB password parameterized (no longer hardcoded) | `application.properties:3-5` |
| `COOKIE_SECURE=true`, `TRUST_PROXY=true` in prod profile | `application-prod.properties` |
| docker-compose: duplicate vars removed, MinIO via env, app health check, frontend waits for healthy app | `docker-compose.yml` |
| `curl` added to runtime Dockerfile stage (for health check) | `Dockerfile` |
| V24 migration: 9 indexes + constraints | `V24__performance_indexes_and_constraints.sql` |
| PayPal cancel endpoint + SecurityConfig + frontend integration | `PayPalOrderController.java`, `PayPalServiceImpl.java`, `PaymentCancelledPage.tsx` |
| PayPal amount validation in captureOrder | `PayPalServiceImpl.java`, `PayPalCaptureResponse.java` |
| `robots.txt` created | `frontend/public/robots.txt` |
| `sitemap.xml` created | `frontend/public/sitemap.xml` |
| OpenGraph + Twitter Card + meta description | `frontend/index.html` |
| Seed data: 14 new designs (28 total, all 14 collections covered) | `seed_catalog.sql` |
| i18n: CartDrawer translated (was 100% hardcoded) | `CartDrawer.tsx` |
| i18n: OrderHistoryPage last hardcoded string fixed | `OrderHistoryPage.tsx` |
| i18n: ~30 new keys (cart, auth, design, errors) in ru/en/kk | `locales/*/translation.json` |

---

## Production Launch Recommendation

**Do not launch yet.** The three blockers (refresh token revocation, Vite dev server, 80% hardcoded UI) must be addressed first. With a realistic 1-week sprint these can all be resolved.

**Recommended pre-launch checklist:**

1. Implement server-side refresh token invalidation (create `refresh_tokens` table, update `AuthServiceImpl`)
2. Create a production Docker image for the frontend (nginx + `npm run build`)
3. Complete i18n for `CartPage.tsx`, `DesignPage.tsx`, `LoginPage.tsx`, `RegisterPage.tsx`
4. Set `SPRING_PROFILES_ACTIVE=prod` and all secrets in a production `.env` or secrets manager
5. Change `POSTGRES_PASSWORD`, `JWT_SECRET`, `MINIO_ROOT_USER/PASSWORD` from defaults
6. Set `ALLOWED_ORIGINS` to the production domain only
7. Upload design images via admin panel (all 28 designs currently have `main_image_url = NULL`)
8. Close port 9001 (MinIO console) from public internet in production
9. Set `SWAGGER_ENABLED=false` in production environment

**What's already production-quality:**
- Freedom Pay + PayPal payment processing with full signature verification
- Webhook replay protection (idempotent callbacks)
- JWT auth with role-based access (ADMIN guard on all management endpoints)
- CDEK delivery integration (live API confirmed)
- Flyway schema management (V1–V24)
- Rate limiting on auth, payment init, custom design, upload endpoints
- CORS restricted to explicit origin list
- Database performance indexes (V24)
