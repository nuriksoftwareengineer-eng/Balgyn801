# I18N_AUDIT.md
> Balgyn — Pre-Launch Localization Audit · 2026-06-21

---

## 1. Overview

| Item | Detail |
|------|--------|
| i18n library | i18next 26.3 + react-i18next 17.0 |
| Locales | `ru` (primary), `kk` (Kazakh), `en` |
| Namespace | `translation` (single namespace) |
| Loading | `i18next-http-backend` — loads from `/locales/{lng}/translation.json` |
| Detection | `i18next-browser-languagedetector` — reads browser preference |
| Switcher | `CompactDropdown` in `SiteNavbar` (ru/kk/en) |
| Currency | Separate context: KZT / USD / EUR / RUB |

---

## 2. Critical Issue — Duplicate `product` Key

**Files:** All three locale files: `frontend/public/locales/ru/translation.json`, `kk/translation.json`, `en/translation.json`

Each file defines `product` twice at the top level. The first occurrence (around line 52) contains `soldOut` and `addToCart`. The second occurrence (around line 395) contains `inStock`, `outOfStock`, `selectSize`, `addToCart`, `comingSoon`. In JSON, the second object silently overrides the first — `product.soldOut` and the first `product.addToCart` are dead keys that are never loaded.

**Impact:**
- `t("product.soldOut")` returns the key string `"product.soldOut"` rather than the translation
- Any component using these keys shows raw key text to users

**Fix:** Merge both `product` objects into one and remove the duplicate top-level key.

---

## 3. Pages With No i18n

### I18N-01 — CustomDesignPage (HIGH)
**File:** `frontend/src/pages/CustomDesignPage.tsx`

The entire page is hardcoded Russian: breadcrumb ("Главная", "Свой дизайн"), form labels ("Имя *", "Телефон *", "Описание"), button text ("Открыть Telegram"), all body copy. No `t()` calls in any JSX. When the user switches to English or Kazakh, this page remains entirely in Russian.

### I18N-02 — TrackOrderPage (MEDIUM)
**File:** `frontend/src/pages/TrackOrderPage.tsx`

All labels, buttons, headings, and order status descriptions are hardcoded Russian. No `t()` calls in the JSX. Status descriptions ("Ожидает оплаты", "Подтверждён", etc.) are hardcoded Russian text blocks.

### I18N-03 — All Admin Pages (LOW for admin audience)
All 14 admin pages use hardcoded Russian throughout (labels, error messages, status badges, date formatting). This is intentional for a Russian-speaking admin team, but worth noting if the admin audience expands.

---

## 4. Hardcoded Strings in Customer-Facing Pages

| File | Issue | Severity |
|------|-------|----------|
| `DesignPage.tsx` | `useSeoMeta` title fallback: `"Дизайн — Balgyn"` | MEDIUM |
| `CollectionPage.tsx` | `useSeoMeta` title fallback: `"Коллекция — Balgyn"` | MEDIUM |
| `OrderHistoryPage.tsx` | `toLocaleDateString("ru-RU")` — date always in Russian | MEDIUM |
| `CatalogIndexPage.tsx` | `t("catalog.metaDesc", "Коллекции вышивки Balgyn")` — Russian fallback bypasses locale | LOW |
| `OrderHistoryPage.tsx` | `🧵` emoji hardcoded — not a translation key | LOW |
| `CartPage.tsx` | `"СДЭК ПВЗ «${point.name}»"` — Russian abbreviation in address | LOW |
| `CartPage.tsx` | Phone placeholder `"+7 …"` — Kazakhstan-centric | LOW |

---

## 5. Locale File Structure

All three locale files are structurally aligned: same top-level keys and same nesting depth. No keys are missing at the top level. Coverage areas:

`nav`, `header`, `auth`, `profile`, `payment`, `orders`, `recovery`, `notFound`, `currency`, `language`, `cart`, `design`, `errors`, `home`, `footer`, `catalog`

Plus the duplicate `product` key (see section 2).

### i18n Coverage Summary

| Area | Coverage |
|------|----------|
| Navigation (SiteNavbar, SiteHeader) | All t() ✓ |
| Auth (LoginPage, RegisterPage) | All t() ✓ |
| Cart and checkout (CartPage) | All t() ✓ |
| DesignPage | Most t(); title fallback hardcoded ⚠ |
| OrderHistoryPage | Labels t(); date hardcoded "ru-RU" ⚠ |
| ProfilePage | All t() ✓ |
| Payment result pages | All t() ✓ |
| NotFoundPage | All t() ✓ |
| Footer | All t() ✓ |
| CustomDesignPage | Entirely hardcoded Russian ✗ |
| TrackOrderPage | Entirely hardcoded Russian ✗ |
| Admin pages | All hardcoded Russian (intentional) ✗ |

---

## 6. Date and Number Formatting

| Context | Current | Issue |
|---------|---------|-------|
| OrderHistoryPage date | `toLocaleDateString("ru-RU")` | Always Russian regardless of language setting |
| AdminOrdersPage date | `new Intl.DateTimeFormat("ru-RU")` | Russian-only (admin, low priority) |
| Currency amounts | `formatMoney()` with `useCurrency()` context | Correct ✓ |
| Prices in catalog | `formatMoney()` | Correct ✓ |

**Fix for OrderHistoryPage:**
```ts
const { i18n } = useTranslation();
toLocaleDateString(i18n.language, { day: "numeric", month: "long", year: "numeric" })
```

---

## 7. Missing i18next Configuration

No `fallbackLng` is configured in the i18next initialization. When a translation key is missing in the active locale (`kk` or `en`), i18next shows the raw key string (e.g., `"product.soldOut"`) rather than falling back to the Russian translation.

**Recommendation:** Add `fallbackLng: "ru"` to the i18next init options so missing Kazakh or English keys display Russian text instead of raw key strings.

---

## 8. Kazakh Locale Completeness

The `kk` locale file has the same top-level structure as `ru`. A deep key-by-key comparison was not performed but spot-checks showed all main customer flows are covered. A CI tool like `i18next-parser` or automated key diffing is recommended before launch to confirm no keys are missing in `kk` or `en`.

---

## 9. Summary

| # | Issue | Severity |
|---|-------|----------|
| 1 | Duplicate `product` key in all 3 locale files — first definition silently overridden | HIGH |
| 2 | CustomDesignPage — no i18n; entirely hardcoded Russian | HIGH |
| 3 | TrackOrderPage — no i18n; entirely hardcoded Russian | MEDIUM |
| 4 | Date locale hardcoded `"ru-RU"` in OrderHistoryPage | MEDIUM |
| 5 | SEO title fallbacks hardcoded Russian (DesignPage, CollectionPage) | MEDIUM |
| 6 | No `fallbackLng: "ru"` in i18next config — missing keys show raw key strings | MEDIUM |
| 7 | `t()` second-arg Russian fallback in CatalogIndexPage bypasses locale files | LOW |
| 8 | 🧵 emoji hardcoded in OrderHistoryPage | LOW |
| 9 | Admin pages all hardcoded Russian (low impact for admin audience) | LOW |
