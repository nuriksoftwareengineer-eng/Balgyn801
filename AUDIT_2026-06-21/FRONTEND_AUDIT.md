# FRONTEND_AUDIT.md
> Balgyn — Pre-Launch Frontend Audit · 2026-06-21

---

## 1. Stack

| Item | Detail |
|------|--------|
| React | 19.2.5 |
| TypeScript | ~6.0.2 |
| Vite | 8.0.10 |
| React Router DOM | 7.15.0 |
| TanStack React Query | 5.100.9 |
| Zustand | 5.0.0 |
| i18next | 26.3 + react-i18next 17.0 |
| Framer Motion | 11.18.2 |
| Tailwind CSS | 4.3.0 (Vite plugin) |
| Lucide React | 1.18.0 |

---

## 2. Routing

All 30+ pages wrapped in `React.lazy()` + `<Suspense fallback={<PageLoadFallback />}>` — correct bundle splitting.

| Path | Component | Guard |
|------|-----------|-------|
| `/` | HomePage | Public |
| `/catalog` | CatalogIndexPage | Public |
| `/catalog/:param` | CatalogParamPage (routes to GroupPage or legacy ProductPage by numeric detection) | Public |
| `/catalog/:groupSlug/:collectionSlug` | CollectionPage | Public |
| `/catalog/:groupSlug/:collectionSlug/:designSlug` | DesignPage | Public |
| `/cart` | CartPage | Public |
| `/custom-design` | CustomDesignPage | Public |
| `/about`, `/terms`, `/privacy`, `/returns`, `/delivery`, `/contacts`, `/track-order` | Static info pages | Public |
| `/payment-return` | PaymentReturnPage | Public |
| `/payment/success`, `/payment/failed`, `/payment/cancelled` | Payment result pages | Public |
| `*` | NotFoundPage | Public |
| `/orders`, `/profile` | Auth-guarded | `RequireAuth` |
| `/login`, `/register` | Auth shell | `AuthShellLayout` |
| `/admin/**` | Admin pages | `RequireAdmin` |

---

## 3. Critical Bugs (P0)

### BUG-F01 — PaymentReturnPage broken for Freedom Pay
**File:** `frontend/src/pages/PaymentReturnPage.tsx`

Freedom Pay redirects the user to `/payment-return` after payment. `PaymentReturnPage` is exclusively designed for PayPal: it reads `?token=` from the URL and calls `capturePayPalOrder`. When Freedom Pay redirects here (no `?token=`), the capture is never called, the page shows an infinite spinner, and the user has no path forward. They will eventually abandon, even though the payment may have been confirmed server-side via the Freedom Pay callback.

**Severity: CRITICAL — Freedom Pay users cannot complete their checkout flow.**

### BUG-F02 — Dead Zustand stores pollute localStorage forever
**Files:** `frontend/src/stores/cart.store.ts`, `frontend/src/stores/checkout.store.ts`

`useCartStore` (persisted to `balgyn-cart-v1`) and `useCheckoutStore` (in-memory only) are defined but never imported by any page. They are bundled, their localStorage keys are written for any existing data, and their state is never cleaned up. `balgyn-cart-v1` entries from earlier development/testing are silently accumulated in users' browsers.

**Severity: HIGH — unnecessary dead code + localStorage pollution.**

### BUG-F03 — Cart "Итого" total never includes delivery fee
**File:** `frontend/src/pages/CartPage.tsx`, line ~793

```ts
const grandTotal = subtotal;  // delivery is never added
```

The order summary sidebar shows `grandTotal` as item subtotal only. Flat-fee and international delivery charges are shown separately as line items but never added to the bolded "Итого" total. The actual charge (from `order.totalPrice` after server confirmation) includes delivery, creating a visible discrepancy: the customer sees `subtotal` as the total, but is charged `subtotal + delivery`.

**Severity: CRITICAL — customers see a lower total than what they are charged.**

### BUG-F04 — ProfilePage admin link never shows
**File:** `frontend/src/pages/ProfilePage.tsx` line 12 vs `frontend/src/admin/RequireAdmin.tsx`

```tsx
// ProfilePage.tsx:
const isAdmin = user.roles?.includes("ROLE_ADMIN");  // expects "ROLE_ADMIN"

// RequireAdmin.tsx:
user.roles.includes("ADMIN");  // expects "ADMIN" (no prefix)
```

The backend JWT includes roles as `"ADMIN"` (without prefix). `RequireAdmin` correctly checks for `"ADMIN"` — admin routes work. But `ProfilePage` checks for `"ROLE_ADMIN"` which never matches, so the "Панель администратора" link is never rendered for any admin user.

**Severity: HIGH — admins cannot navigate to the admin panel from their profile.**

---

## 4. High Severity Issues

### F-05 — Auth cold-start session loss
**File:** `frontend/src/app/auth-context.tsx`

On page load with an expired access token, `auth-context` calls `getMe()` with the token. If `getMe` returns 401, `auth-context` clears the token and marks the user logged-out — without attempting the HttpOnly cookie refresh first. The refresh-on-401 mechanism in `apiFetch` only kicks in for subsequent API calls, not the `getMe` initialization call. Users are unexpectedly logged out on cold page load instead of being silently re-authenticated.

### F-06 — No phone number format validation
**File:** `frontend/src/pages/CartPage.tsx` — step 4 (address form)

The phone number field only checks `trim().length > 0`. The user can enter a single letter and proceed to checkout. Backend also does not validate phone format. Invalid phone numbers reach the carrier/CDEK with no warning.

### F-07 — CustomDesignPage has no i18n
**File:** `frontend/src/pages/CustomDesignPage.tsx`

The entire page — breadcrumb, form labels, button text, description — is hardcoded in Russian. When the user selects English or Kazakh in the language switcher, all content on this page remains in Russian.

### F-08 — BestSellers section has no error state
**File:** `frontend/src/widgets/home/BestSellers.tsx`

If the catalog API fails, `isPending` shows skeletons but the error branch renders an empty component silently. The homepage hero section appears broken with no explanation.

---

## 5. Medium Severity Issues

### F-09 — OrderHistoryPage uses imperative fetch (not React Query)
Uses `useEffect` + `useState` instead of `useQuery`. No caching, no retry on failure, no refetch-on-focus. Inconsistent with every other page in the app.

### F-10 — Recovery banner "Отменить заказ" is misleading
**File:** `frontend/src/pages/CartPage.tsx` — `RecoveryBanner`

The dismiss button is labeled `t("recovery.dismiss")` which in Russian translates to "Отменить заказ" ("Cancel order"). Clicking it only closes the UI banner — the backend order remains in `PENDING_PAYMENT` status and continues consuming reserved inventory until expiry (60 min). Users who dismiss the banner believing they cancelled the order are surprised when the inventory hold persists.

### F-11 — Gallery images uploaded in admin, never shown in storefront
`AdminDesignsPage` allows uploading gallery images and they are stored in `design.gallery` (JSONB). `DesignPage` only renders `design.mainImageUrl` — the gallery array is fetched but never displayed.

### F-12 — Collection cover images not shown in storefront
`CollectionDetail` API response includes `coverImageUrl` and `bannerImageUrl`. The `CollectionPage` and `CollectionPage` card components do not display these images — collection cards show only a letter placeholder.

### F-13 — DELIVERY_REGIONS uses "US" iso2 for all international customers
**File:** `frontend/src/pages/CartPage.tsx` DELIVERY_REGIONS constant

Non-CIS international customers are assigned `iso2: "US"` which is passed to the backend's delivery pricing endpoint. Kazakhstan and Russian customers are mapped correctly but any other international customer is treated as "USA" by the backend logic.

### F-14 — No quantity selector on DesignPage
Users can only add 1 unit at a time. To add 2 hoodies they must visit the DesignPage twice. No quantity input exists anywhere before the cart.

### F-15 — CatalogParamPage slug-vs-ID routing is fragile
A group slug that starts with a digit (e.g., `"2025-collection"`) will be routed to the legacy `ProductPage` (numeric detection) instead of `GroupPage`. A naming conflict is easily made by non-technical staff in the admin panel.

### F-16 — TrackOrderPage is a stub with no real functionality
The form accepts order number + phone but submits only to a local state. The "submitted" state shows a message saying to contact Telegram. No CDEK tracking integration exists.

### F-17 — CollectionPage has no empty state
If a collection has zero published designs, the page renders no designs with no "No items in this collection yet" message.

### F-18 — Date locale hardcoded "ru-RU" in OrderHistoryPage
```ts
toLocaleDateString("ru-RU", ...)
```
This ignores the i18n language selection. English and Kazakh users see Russian-formatted dates.

### F-19 — PayPal cancel fetch bypasses apiFetch wrapper
`PaymentCancelledPage` makes a raw `fetch()` to cancel the PayPal order, bypassing the `apiFetch` error handling, retry logic, and auth headers. Error handling for this call is absent.

---

## 6. Low Severity Issues

| ID | Description |
|----|-------------|
| F-20 | `DesignPage` auto-navigates to `/cart` on add-to-cart; "Added ✓" confirmation is never visible |
| F-21 | `ProfilePage` has no password change, name edit, or account deletion |
| F-22 | SEO title on DesignPage fallback is hardcoded Russian "Дизайн — Balgyn" (not using `t()`) |
| F-23 | No `<title>` update in CartPage, OrderHistoryPage, ProfilePage |
| F-24 | No OG/social meta tags on any page |
| F-25 | Breadcrumb `href` on DesignPage uses `useParams.groupSlug`, which may differ from `design.groupSlug` from API |
| F-26 | `GARMENT_LABELS` includes `OVERSIZE_TSHIRT`, `LONGSLEEVE`, `ZIP_HOODIE` but these are absent from the admin dropdown `GARMENT_TYPES` |
| F-27 | `balgyn-cart-v1` localStorage key from dead Zustand store accumulates data forever |
| F-28 | `pvzFilter` input uses `autoFocus` — steals focus on every re-render; mobile keyboard pops up unexpectedly |
| F-29 | No breadcrumb on `/cart` page |
| F-30 | `CheckoutStore` (Zustand) imported and bundled but never used in CartPage checkout flow |
| F-31 | `STEP_LABELS` defined twice (CartPage and StepIndicator) — duplicate definitions |

---

## 7. State Management

**Cart state:** `CartProvider` / `CartContext` — localStorage per-identity (`balgyn_cart_v3:u:{email}` / `balgyn_cart_v3:guest`). Supports legacy and design cart line types. Cart merges on login. **This is the active cart implementation.**

**Dead stores (bundled but unused):**
- `useCartStore` (Zustand, `balgyn-cart-v1`) — never imported by any page
- `useCheckoutStore` (Zustand, in-memory) — never imported by any page

**Auth state:** `auth-context.tsx` — access token in `localStorage`, refresh via HttpOnly cookie. Cross-tab sync via `storage` event.

**Pending payment:** `pending-payment.ts` — `localStorage` with 58-min TTL. Survives F5 and tab close. Recovery banner on CartPage.

---

## 8. API Client

**`apiFetch` (`http.ts`):**
- Auto-retry on 401 (refresh once via `accessTokenRefresher`) ✓
- `ApiError` class with `status` + `body` ✓
- Error fallback message hardcoded Russian: `"Запрос завершился с кодом ${response.status}"`
- Unsafe double cast: `body as { detail: unknown }` with no runtime validation

**Code splitting:** All pages lazy-loaded ✓
**Bundle concern:** `framer-motion` 11.18 (~80-100KB gzipped) is a significant addition for light animations only used in the cart drawer and a few transitions.

---

## 9. Positive Patterns

- Full lazy-load + Suspense code splitting on all 30+ pages ✓
- React Query used consistently for all server state on customer pages ✓
- `useTranslation` used on all customer-facing pages (except CustomDesignPage) ✓
- `CartProvider` handles guest/user cart merge correctly ✓
- Checkout recovery localStorage pattern is solid (58-min TTL, clear on success) ✓
- `useSeoMeta` canonical URL generation ✓
- `ApiError` class preserves full response body for structured error handling ✓
- `apiFetch` auto-retry on 401 prevents most session interruption ✓
