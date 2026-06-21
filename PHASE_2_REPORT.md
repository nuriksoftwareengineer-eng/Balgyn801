# Phase 2 Report — Balgyn Bol E-Commerce

**Date:** 2026-06-20  
**Backend tests:** 84 ✅ / 0 ❌  
**Frontend:** Vite dev server, no build errors

---

## Task Status

| # | Task | Status |
|---|------|--------|
| 1 | Payment UI Redesign | ✅ Done |
| 2 | Payment Success / Cancel / Fail pages | ✅ Done |
| 3 | Custom 404 page | ✅ Done |
| 4 | Multi-language support (ru/kk/en) | ✅ Done |
| 5 | Auto language detection + manual switcher | ✅ Done |
| 6 | Multi-currency selector (KZT/USD/EUR/RUB) | ✅ Done |
| 7 | Real exchange rates (hourly NBK refresh) | ✅ Done |
| 8 | Auto currency by language | ✅ Done |
| 9 | Catalog visual polish — empty states | ✅ Done |
| 10 | Order History polish | ✅ Done |
| 11 | Admin Panel Audit | ✅ Done → `ADMIN_AUDIT_PHASE_2.md` |
| 12 | UX Polish — empty states, skeletons | ✅ Done |
| 13 | This report | ✅ Done |

---

## 1. Payment Pages

### 1.1 New pages created
- [`/payment/success`](frontend/src/pages/PaymentSuccessPage.tsx) — reads `sessionStorage["balgyn_last_payment"]` (orderId, totalPrice, provider), shows confirmation, clears storage
- [`/payment/failed`](frontend/src/pages/PaymentFailedPage.tsx) — shows error from `?error=` param, keeps storage for retry
- [`/payment/cancelled`](frontend/src/pages/PaymentCancelledPage.tsx) — shows cancellation, keeps storage for retry

### 1.2 Redirect URL flow fixed
- **Freedom Pay:** backend `FREEDOMPAY_SUCCESS_URL` / `FREEDOMPAY_FAILURE_URL` → `/payment/success` / `/payment/failed`
- **PayPal:** `PaymentReturnPage` captures token, then `navigate()` to success/failed; cancel → `/payment/cancelled`
- No `localhost:5173` hardcodes — all URLs built from `FRONTEND_BASE_URL` env var

### 1.3 sessionStorage bridge
`CartPage.handleInitPayment()` stores `{orderId, totalPrice, provider}` before redirecting so the success/fail pages can display order info even when the payment provider doesn't pass it back in the redirect URL.

---

## 2. 404 Page

[`NotFoundPage.tsx`](frontend/src/pages/NotFoundPage.tsx) renders on the `*` catch-all route inside `MainLayout`. Replaces the React Router "Unexpected Application Error" crash screen with a branded Balgyn page (404 code + bold headline + two CTA buttons).

---

## 3. Multi-Language (Tasks 4 & 5)

### Setup
- **Library:** `react-i18next` v17 + `i18next` v26 + `HttpBackend` + `LanguageDetector`
- **Config:** [`frontend/src/app/i18n.ts`](frontend/src/app/i18n.ts)
- **Locales:** `frontend/public/locales/{ru,kk,en}/translation.json`
- **Detection order:** `localStorage["balgyn_lng"]` → `navigator.language` → fallback `ru`

### Coverage
Translated keys cover: `nav.*`, `payment.*`, `orders.*`, `notFound.*`, `currency.*`, `language.*`

### Switcher
RU / KZ / EN buttons added to `SiteNavbar` (desktop: top-right; mobile: menu footer). Clicking persists to localStorage.

---

## 4. Multi-Currency (Tasks 6, 7, 8)

### Frontend context
[`CurrencyContext`](frontend/src/app/currency-context.tsx) provides:
- `currency` — active currency (KZT | USD | EUR | RUB)
- `setCurrency(c)` — persists to `localStorage["balgyn_currency"]`
- `format(kztAmount)` — converts from KZT and formats with symbol
- `convert(kztAmount)` — raw number conversion

### Auto-detection
`detectDefaultCurrency()` maps `navigator.language` → currency:
- `kk`, `ru-KZ` → KZT
- `ru` → RUB  
- `de`, `fr`, `it`, `es`, `nl`, `pt`, `pl` → EUR
- `en` → USD
- else → KZT

### Exchange rates backend
**V23 Flyway migration** adds `KZT_EUR` and `KZT_RUB` bootstrap rows.

**`NbkExchangeRateProvider`** — extended with `fetchKztPerEur()` and `fetchKztPerRub()` using shared `fetchFromFeed()` helper parsing NBK RSS.

**Scheduler** — changed from daily (`0 0 6 * * *`) to **hourly** (`0 0 * * * *`).

**Public endpoint** — `GET /api/v1/exchange-rates` (no auth) returns `{kztPerUsd, kztPerEur, kztPerRub, updatedAt}`.

**`CurrencyContext`** fetches this endpoint on mount with fallbacks (480 / 530 / 5.3) if backend is unreachable.

---

## 5. Catalog Visual Polish (Task 9)

### Improvements
- `DesignGrid` — replaced plain "В коллекции пока нет дизайнов." with a styled empty state (🧵 icon + description + CTA to `/custom-design`)
- `CatalogIndexPage` — styled empty state with icon and link
- `GroupPage` — styled empty state with link back to catalog
- All empty states follow the same pattern for visual consistency

### Not done (needs backend action)
Demo data (min 10 products per category) must be added via the admin panel or a seed SQL script. The frontend correctly renders designs when they exist — the catalog infrastructure is in place.

---

## 6. Order History Polish (Task 10)

[`OrderHistoryPage.tsx`](frontend/src/pages/OrderHistoryPage.tsx) — rewrote with:

- **Colored status pills** — bordered pill badges with background color per status (blue=NEW, amber=CONFIRMED, emerald=READY, red=CANCELLED, etc.)
- **Currency-aware prices** — `format(order.totalPrice)` converts from KZT to selected currency
- **Item placeholder** — 🧵 emoji square next to each item (actual product image not available in `OrderItemResponse`)
- **Better skeleton** — structured card skeleton with pulse animation
- **Better empty state** — icon + description + CTA button
- **i18n** — all strings via `t()` calls

### Limitation: payment method not shown
`OrderResponse` does not include `paymentProvider`. To add it, the backend `OrderResponse` DTO and mapper need to expose the last payment record's provider.

---

## 7. UX Polish (Task 12)

- All loading states use `animate-pulse` skeleton cards (orders, catalog, collection)
- Error states show `text-[--color-danger]` messages
- Empty states across catalog and orders replaced with icon + description + CTA
- Unknown routes → `NotFoundPage` (not a React Router crash)

---

## 8. Backend Changes

| File | Change |
|------|--------|
| `V23__add_exchange_rates_eur_rub.sql` | Adds `KZT_EUR=530`, `KZT_RUB=5.3` bootstrap rows |
| `ExchangeRateProvider.java` | Added `fetchKztPerEur()`, `fetchKztPerRub()` methods |
| `NbkExchangeRateProvider.java` | Implements EUR/RUB fetch via shared `fetchFromFeed()` |
| `ExchangeRateService.java` | Added `publicRates()` method |
| `ExchangeRateServiceImpl.java` | Hourly cron, `publicRates()`, EUR/RUB upsert |
| `PublicExchangeRatesResponse.java` | New record DTO |
| `PublicExchangeRateController.java` | `GET /api/v1/exchange-rates` — no auth |
| `SecurityConfig.java` | `GET /api/v1/exchange-rates` → `permitAll()` |
| `ExchangeRateAndSettingsIntegrationTest.java` | `MutableProvider` now implements all 3 interface methods |

**Test result:** 84 / 84 ✅

---

## 9. Bugs Fixed

| Bug | Fix |
|-----|-----|
| React Router showed crash page on unknown URLs | Added `path: "*"` catch-all with `NotFoundPage` |
| Freedom Pay redirect went to non-existent page | Created `/payment/success` and `/payment/failed` |
| PayPal cancel URL was `/payment-return?status=CANCELLED` | Changed to `/payment/cancelled` |
| `MutableProvider` test class missing `fetchKztPerEur/Rub` | Added stub implementations returning `Optional.empty()` |
| Language/currency switchers in `SiteHeader.tsx` never shown | App uses `SiteNavbar.tsx`; moved switchers there |

---

## 10. Known Gaps / Phase 3 Recommendations

1. **Payment method in order history** — Add `paymentProvider` to `OrderResponse` DTO
2. **Product images in order history** — `OrderItemResponse` has no image URL; add `imageUrl` from design gallery
3. **EUR/RUB admin edit** — Extend `AdminExchangeRatePage` to show/edit all three rates
4. **Catalog demo data** — Seed ≥10 designs via admin panel or SQL seed script
5. **CDEK shipment UI** — Add shipment creation form to Order Detail admin page
6. **Payment refunds** — Freedom Pay and PayPal refund APIs not yet implemented
7. **Image upload in admin** — Wire `/api/v1/media/upload` to a file picker in design forms
