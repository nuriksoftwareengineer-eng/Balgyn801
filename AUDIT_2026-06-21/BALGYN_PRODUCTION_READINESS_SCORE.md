# BALGYN_PRODUCTION_READINESS_SCORE.md
> Balgyn — Pre-Launch Readiness Assessment · 2026-06-21
>
> Assessment by: External Technical Audit (Claude Sonnet 4.6)
> Scope: Full codebase review — backend, frontend, database, security, infrastructure, i18n, performance, operations

---

## Domain Scores (0–10)

| # | Domain | Score | Rationale |
|---|--------|-------|-----------|
| 1 | Backend Architecture | **7 / 10** | Solid foundation: state machine, inventory protection, payment idempotency, publication guards, 84 integration tests. Key gaps: N+1 queries, no pagination, no exception handler for lock timeout. |
| 2 | Frontend Architecture | **6 / 10** | Correct lazy-loading, React Query, i18n on customer pages. Critical bugs: PaymentReturnPage broken for Freedom Pay, cart total omits delivery fee, dead Zustand stores, auth cold-start issue. |
| 3 | Security | **4 / 10** | Serious deficiencies dominate: no HTTPS, real credentials in git, no CDEK webhook verification, stateless refresh tokens, no CSP. Strong positives: payment signature verification, BCrypt, rate limiting, Swagger off in prod. |
| 4 | Payments | **6 / 10** | Freedom Pay: signature verified, idempotency, XML escaping, stub guard — excellent. PayPal: webhook ID blank by default, capture event order ID fallback bug, cancel endpoint unauthenticated — needs attention. Critical: late payment re-confirms cancelled orders. |
| 5 | Database | **7 / 10** | 26 well-structured migrations, Flyway ownership, good index coverage (V24), idempotency table, pessimistic lock. Gaps: no quantity CHECK constraint, no enum CHECK on status, V7 ON DELETE SET NULL risk. |
| 6 | Localization (i18n) | **6 / 10** | Three locales (ru/kk/en), all customer pages covered, language + currency switcher. Critical: duplicate `product` key in all files. Missing: CustomDesignPage, TrackOrderPage, no fallbackLng. |
| 7 | Admin UX | **5 / 10** | Functional admin panel covering full catalog lifecycle. Major issues: archive with no confirmation, inventory save 30 sequential requests, CDEK actions unconfirmed, KPI links to wrong list, gallery images not shown in storefront. |
| 8 | Operations | **3 / 10** | Docker Compose is functional; secrets enforced for prod. Critical gaps: no HTTPS, no backup strategy, no Spring Actuator, no log rotation, no CI/CD. |
| 9 | Performance | **5 / 10** | Code splitting excellent. Backend N+1 on admin orders and design page. Admin endpoints unbounded. CDEK webhook full table scan. Inventory save loop sequential. |
| 10 | Test Coverage | **8 / 10** | 84 integration tests across 22 classes covering orders, payments, inventory, delivery, auth, expiry — comprehensive domain coverage. No frontend tests. No load/stress tests. |

---

## Weighted Score

| Domain | Weight | Score | Weighted |
|--------|--------|-------|----------|
| Security | 25% | 4 | 1.00 |
| Backend Architecture | 15% | 7 | 1.05 |
| Payments | 15% | 6 | 0.90 |
| Frontend Architecture | 15% | 6 | 0.90 |
| Database | 10% | 7 | 0.70 |
| Operations | 10% | 3 | 0.30 |
| Performance | 5% | 5 | 0.25 |
| Localization | 3% | 6 | 0.18 |
| Admin UX | 2% | 5 | 0.10 |
| Test Coverage | 5% | 8 | 0.40 |
| **TOTAL** | **100%** | — | **5.78 / 10** |

---

## Launch Readiness Verdict

### ❌ NOT READY FOR LAUNCH

**The platform cannot go live until 6 P0 blockers are resolved:**

1. **No HTTPS** — payment data travels over HTTP. This violates PCI DSS compliance, Freedom Pay and PayPal terms of service, and exposes every user session to interception.

2. **Real credentials in git** — `FREEDOMPAY_SECRET_KEY` in `.env.example` is compromised. Anyone who has ever cloned this repository can forge Freedom Pay payment callbacks. **Rotate the secret immediately.**

3. **Late payment re-confirms CANCELLED orders** — an admin can cancel an order while the buyer is on the payment page; the arriving webhook then revives the cancelled order, triggering fulfillment of an order the admin explicitly cancelled.

4. **No CDEK webhook verification** — delivery status can be forged by any third party, allowing premature DELIVERED status on unshipped orders.

5. **PaymentReturnPage broken for Freedom Pay** — users who pay via Freedom Pay are redirected to a page designed for PayPal and see an infinite spinner. They cannot complete checkout.

6. **CartPage total omits delivery fee** — customers are shown a lower total than what they will be charged, which is a consumer protection violation and will generate chargebacks.

---

## Path to Launch

### Week 1 (P0 — Must be done before first transaction)

| Task | Owner | Hours |
|------|-------|-------|
| Configure HTTPS with Let's Encrypt on nginx | DevOps | 3–4h |
| Rotate FREEDOMPAY_SECRET_KEY; replace .env.example with placeholders | Dev | 1h |
| Fix payment handlers to check `order.status != CANCELLED` before confirming | Backend | 1h |
| Fix PaymentReturnPage to detect Freedom Pay vs PayPal redirect (check absence of `?token=`) | Frontend | 2h |
| Fix CartPage grandTotal to include delivery fee in the displayed total | Frontend | 1h |
| Add CDEK webhook signature verification | Backend | 3h |

**Estimated: 11–12 hours total**

### Week 2 (P1 — Before high transaction volume)

- Fix PayPal cancel endpoint auth
- Add `/actuator/health` (Spring Actuator)
- Fix ProfilePage `ROLE_ADMIN` → `ADMIN` prefix check
- Set up automated daily backups (pg_dump + MinIO mirror)
- Add rate limiting to `/auth/refresh`
- Fix duplicate `product` key in locale files
- Add `PessimisticLockingFailureException` handler
- FreedomPay fail-fast on blank merchant ID in non-test profile
- Auth cold-start: attempt cookie refresh before clearing session

**Estimated: 2–3 days total**

### Month 1 (P2 — Before growth)

- Add EntityGraph to order list (fix N+1)
- Add pagination to admin endpoints
- Add batch inventory save endpoint
- Add CDEK shipment UUID index
- Add confirmation dialogs for archive/delete/CDEK actions
- Add structured logging
- Fix CustomDesignPage i18n
- Add i18next fallbackLng
- Fix date locale in OrderHistoryPage
- Remove dangerous DB password default

---

## What Is Already Well-Built

The platform has a strong functional core that would take weeks to build from scratch:

- **Order lifecycle:** `PENDING_PAYMENT → CONFIRMED → IN_PRODUCTION → READY → SHIPPED → DELIVERED` with explicit state machine, admin controls, and order history audit trail
- **Inventory protection:** Pessimistic write lock on checkout prevents overselling
- **Order expiry:** Automatic cleanup of unpaid orders with inventory release
- **Payment integration:** Two payment providers (Freedom Pay + PayPal) with signature verification and idempotency
- **Publication workflow:** DRAFT → READY → PUBLISHED → ARCHIVED design lifecycle with readiness validation
- **Catalog architecture:** Groups → Collections → Designs → Garment Variants with slugs and media
- **CDEK integration:** City search, PVZ lookup, tariff calculation (live API verified)
- **Checkout recovery:** localStorage TTL + recovery banner + retry from order history
- **Multilanguage:** ru/kk/en with currency switching (KZT/USD/EUR/RUB)
- **84 integration tests:** Orders, payments, inventory, delivery, expiry, auth — all green
- **Docker production build:** Single `docker-compose -f docker-compose.prod.yml up -d` command

The security and operational gaps are real, but they are configuration and targeted code fixes — not architectural rewrites. This project is significantly closer to production than the raw score suggests; the blockers are concentrated and addressable in a focused sprint.

---

## Risk Summary

| Risk | Likelihood | Impact | Blocker? |
|------|-----------|--------|---------|
| Customer data intercepted (no HTTPS) | HIGH on HTTP | CRITICAL | YES |
| Forged Freedom Pay callback (leaked secret) | HIGH if repo is public | CRITICAL | YES |
| Customer charged wrong amount (delivery gap) | CERTAIN for delivery orders | HIGH | YES |
| Freedom Pay users stuck at spinner | CERTAIN for FP payments | CRITICAL | YES |
| Cancelled order revived by late payment | LOW but possible | HIGH | YES |
| Forged CDEK webhook | LOW (URL must be known) | MEDIUM | YES |
| Data loss (no backup) | LOW in short term | CATASTROPHIC | P1 |
| Admin cannot see pending payment orders | CERTAIN | MEDIUM | P2 |
| Inventory save partial failure | LOW | MEDIUM | P2 |
