# SYSTEM_ARCHITECTURE.md
> Balgyn — Pre-Launch Technical Inventory · 2026-06-21

---

## 1. Product Summary

**BALGYN** is an embroidery design e-commerce platform.
The **design** is the product. Garments (hoodie, t-shirt, sweatshirt, etc.) are variants within a design.
Customers browse a catalog → select a design → choose garment / color / size → checkout.

---

## 2. Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend language | Java | 21 (LTS) |
| Backend framework | Spring Boot | 4.0.6 |
| ORM / persistence | Spring Data JPA + Hibernate | via Spring Boot BOM |
| DB migrations | Flyway | via Spring Boot BOM |
| Database | PostgreSQL | 16 |
| Security | Spring Security 6 + JWT (HS256, JJWT 0.12.6) | — |
| DTO mapping | MapStruct | 1.6.3 |
| API docs | springdoc-openapi | 3.0.3 |
| File storage | MinIO (AWS SDK v2 S3-compatible) | SDK 2.29.45 |
| Test DB | H2 in-memory | via Spring Boot BOM |
| Frontend language | TypeScript | ~6.0.2 |
| Frontend framework | React | 19.2.5 |
| Build tool | Vite | 8.0.10 |
| Routing | React Router DOM | 7.15.0 |
| Server state | TanStack React Query | 5.100.9 |
| Client state | Zustand | 5.0.0 |
| Styling | Tailwind CSS | 4.3.0 (Vite plugin) |
| Animations | Framer Motion | 11.18.2 |
| i18n | i18next + react-i18next | 26.3 / 17.0 |
| Icons | Lucide React | 1.18.0 |
| Reverse proxy | nginx | 1.27-alpine |
| Containerization | Docker Compose | — |

---

## 3. High-Level Architecture

```
Internet
   │
   ▼ :80 (HTTP only — no TLS)
┌─────────────────────────────────────────────────┐
│  nginx:1.27-alpine  (reverse proxy)             │
│  /api/**  → app:8080                            │
│  /**      → frontend:80                         │
└─────────────────────────────────────────────────┘
         │                    │
         ▼                    ▼
   ┌───────────┐       ┌──────────────────┐
   │  app:8080 │       │  frontend:80     │
   │ Spring Boot│      │  nginx serving   │
   │  (Java 21)│       │  React SPA dist  │
   └─────┬─────┘       └──────────────────┘
         │
    ┌────┴─────────────────┐
    │  db:5432             │  minio:9000 (internal)
    │  PostgreSQL 16        │  minio:9001 (console, internal)
    └──────────────────────┘
```

**External integrations:**
- **Freedom Pay** — KZT payment gateway (MD5-signed callbacks)
- **PayPal** — USD payments (API-based signature verification)
- **CDEK API v2** — logistics / delivery point lookup / tariff calculation
- **National Bank of Kazakhstan RSS** — hourly exchange rate refresh (NBK feed)

---

## 4. Database

- **30 tables**, managed by 26 Flyway migrations (V1–V26)
- `ddl-auto=validate` — Flyway owns schema, Hibernate only validates
- `baseline-on-migrate=true` for safe zero-downtime adoption on existing DBs

### Table Inventory

| Domain | Tables |
|--------|--------|
| Auth | `users`, `user_roles`, `user_addresses` |
| Customers | `customers` |
| Legacy catalog | `products` (kept, not migrated) |
| Catalog | `catalog_groups`, `collections`, `designs`, `design_garments`, `design_garment_prices`, `design_garment_colors`, `design_garment_sizes`, `colors`, `sizes`, `size_chart_images` |
| Inventory | `inventory` |
| Orders | `orders`, `order_items`, `order_history` |
| Payments | `payments`, `processed_webhook_events` |
| Delivery | `delivery_addresses`, `delivery_settings`, `delivery_tariffs`, `garment_type_weights`, `countries` |
| CDEK | `cdek_shipments` |
| Exchange rates | `exchange_rates` |
| Custom design | `custom_designs` |
| Reviews | `reviews` |

---

## 5. Backend Modules

| Module | Key Classes | Status |
|--------|-------------|--------|
| JWT Auth | `JwtService`, `JwtAuthenticationFilter`, `AuthController` | Live |
| Security | `SecurityConfig`, `SensitiveEndpointRateLimiterFilter`, `PaymentRateLimiterFilter` | Live |
| Users / Roles | `AppUser`, `AdminUserController`, `BootstrapAdminInitializer` | Live |
| Refresh Tokens | HttpOnly cookie (`/auth/refresh-cookie`) | Live |
| Catalog (Admin) | `DesignController`, `CollectionController`, `CatalogGroupController` | Live |
| Catalog (Storefront) | `CatalogStorefrontController` | Live |
| Catalog Hardening | `DesignReadinessService`, `DesignStatus` state machine, V26 migration | Live |
| Orders | `OrderController`, `OrderServiceImpl`, `OrderExpiryService` | Live |
| Order History | `OrderHistoryController`, `order_history` table | Live |
| Inventory | `InventoryController`, pessimistic-write lock on checkout | Live |
| Payments | `PaymentController`, `FreedomPayCallbackController`, `PayPalOrderController`, `PayPalWebhookController` | Live |
| Delivery | `DeliveryController`, CDEK integration, tariff pricing | Live |
| Custom Design | `CustomDesignController` | Live (unused by frontend) |
| Admin APIs | Various `Admin*Controller` classes | Live |
| Media / MinIO | `MediaUploadController`, `MinioMediaStorageService` | Live |
| Exchange Rates | `ExchangeRateController`, NBK RSS cron | Live |

---

## 6. Frontend Structure

```
frontend/src/
├── admin/           Admin pages (15 pages, ~1 file each)
├── app/             Providers, router, auth context, cart context
├── components/ui/   shadcn-style primitives (badge, button, input, etc.)
├── pages/           Customer-facing pages (23 pages)
├── shared/
│   ├── api/         backend-api.ts, catalog-api.ts, http.ts, types.ts
│   ├── constants/   store content constants
│   ├── hooks/       useSeoMeta, etc.
│   ├── lib/         format-money, cn, pending-payment, etc.
│   ├── types/       catalog types
│   └── ui/          container, CompactDropdown, button, etc.
├── stores/          Zustand stores (checkout.store.ts, cart.store.ts — DEAD CODE)
└── widgets/home/    Hero, BestSellers, ValueStrip, InstagramSection
```

**Code splitting:** All 30+ pages wrapped in `React.lazy()` + `Suspense` — correct.

---

## 7. Test Coverage

| Test Class | Focus |
|-----------|-------|
| DesignPublicationIntegrationTest | Publication lifecycle, guard validation |
| InventoryCheckIntegrationTest | Stock reservation on order |
| InventoryDeductionIntegrationTest | Stock deduction on payment |
| InventoryConcurrencyIntegrationTest | Pessimistic lock concurrent order |
| InventoryReleaseIntegrationTest | Inventory release on expiry/cancel |
| OrderStateMachineIntegrationTest | State transition allowlist |
| OrderExpiryIntegrationTest | Auto-expiry of unpaid orders |
| OrderHistoryIntegrationTest | Audit trail creation |
| OrderRateLimitIntegrationTest | Per-IP order rate limit |
| MyOrdersIntegrationTest | Authenticated user order list |
| PaymentSecurityIntegrationTest | Webhook signature, idempotency |
| PayPalPaymentIntegrationTest | PayPal capture + webhook |
| FreedomPayResponseVerificationTest | MD5 signature unit test |
| DeliveryFlowIntegrationTest | Delivery address + pricing |
| DeliveryPricingIntegrationTest | Tariff calculation |
| DeliveryMethodsIntegrationTest | Method listing |
| CdekCalculateOrderIntegrationTest | CDEK tariff endpoint |
| InternationalShippingIntegrationTest | International delivery |
| ExchangeRateAndSettingsIntegrationTest | Exchange rate CRUD |
| GarmentWeightServiceIntegrationTest | Weight lookup |
| CountryServiceIntegrationTest | Country management |
| Balgyn801ApplicationTests | Context loads |

**Total: 22 test classes, ~84 tests (all integration tests against H2, green)**

---

## 8. Key Environment Variables

| Variable | Required In Prod | Purpose |
|----------|-----------------|---------|
| `JWT_SECRET` | Yes (`:?`) | HS256 signing key (min 32 bytes) |
| `POSTGRES_PASSWORD` | Yes (`:?`) | PostgreSQL root password |
| `FRONTEND_BASE_URL` | Yes (`:?`) | Payment redirect base URL |
| `FREEDOMPAY_MERCHANT_ID` | Yes (`:?`) | Freedom Pay merchant |
| `FREEDOMPAY_SECRET_KEY` | Yes (`:?`) | Freedom Pay MD5 secret |
| `FREEDOMPAY_RESULT_URL` | Yes (`:?`) | Freedom Pay callback URL |
| `PAYPAL_CLIENT_ID` | Yes (`:?`) | PayPal API OAuth |
| `PAYPAL_CLIENT_SECRET` | Yes (`:?`) | PayPal API OAuth |
| `PAYPAL_WEBHOOK_ID` | Yes (`:?`) | PayPal webhook verification |
| `CDEK_CLIENT_ID` | Yes (`:?`) | CDEK API |
| `CDEK_CLIENT_SECRET` | Yes (`:?`) | CDEK API |
| `MINIO_ROOT_USER` | Yes (`:?`) | MinIO credentials |
| `MINIO_ROOT_PASSWORD` | Yes (`:?`) | MinIO credentials |
| `MINIO_PUBLIC_URL` | Yes (`:?`) | Public media base URL |
| `ALLOWED_ORIGINS` | Yes (`:?`) | CORS whitelist |
| `ALLOWED_ORIGINS` | Yes (`:?`) | CORS whitelist |
| `SPRING_PROFILES_ACTIVE` | Yes (hardcoded) | Set to `prod` |

---

## 9. Deployment Model

Single-host Docker Compose (`docker-compose.prod.yml`):

```
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

- All services on a single host
- `minio_data` and `postgres_data` are named Docker volumes (no host-path mounts)
- Public entry point: port 80 only (no HTTPS)
- MinIO console (9001) not exposed publicly
- All containers health-checked; `app` waits for `db` and `minio` to be healthy

---

## 10. Flyway Migration History

V1 (baseline) → V2 (catalog groups) → V3 (designs) → V4 (colors/sizes) → V5 (design_garments) → V6 (inventory) → V7 (order_items design link) → V8 (user_addresses) → V9 (orders user link) → V10 (reviews) → V11 (rating type fix) → V12 (garment weights) → V13 (countries) → V14 (payment gate + delivery snapshot) → V15 (delivery_settings + exchange_rates) → V16 (delivery_tariffs) → V17 (payment security / idempotency) → V18 (CDEK integration) → V19 (size chart images) → V20 (collection media + design gallery) → V21 (Freedom Pay provider migration, destructive DELETE) → V22 (schema version marker) → V23 (EUR/RUB rates) → V24 (performance indexes + constraints) → V25 (design status enum) → V26 (design hardening: UNIQUE constraint, sort_order, published_at, archived_at)
