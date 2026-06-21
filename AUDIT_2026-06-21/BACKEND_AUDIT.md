# BACKEND_AUDIT.md
> Balgyn — Pre-Launch Backend Audit · 2026-06-21

---

## 1. Stack

| Item | Detail |
|------|--------|
| Java | 21 (LTS) |
| Spring Boot | 4.0.6 |
| Spring Security | 6.x (via Spring Boot BOM) |
| Spring Data JPA | via Spring Boot BOM |
| MapStruct | 1.6.3 |
| JWT | JJWT 0.12.6 (HS256) |
| Flyway | via Spring Boot BOM |
| springdoc-openapi | 3.0.3 |
| AWS SDK v2 | 2.29.45 (MinIO/S3) |
| Test DB | H2 in-memory |
| Test framework | JUnit 5 + Spring Boot Test |

---

## 2. Critical Bugs (P0)

### BUG-B01 — Late payment webhook re-confirms CANCELLED orders
**File:** `PaymentServiceImpl.java` and `PayPalServiceImpl.java`

When admin cancels an order (`PENDING_PAYMENT → CANCELLED`), the backend marks the order CANCELLED and cancels pending payments. However, if the buyer has the Freedom Pay or PayPal checkout page open and completes payment after the admin cancels, the payment webhook arrives and calls `handleFreedomPayCallback()` / `handleCaptureCompleted()`. These methods check that `order.status == PENDING_PAYMENT || order.status == NEW` but do NOT check `order.status != CANCELLED`. The payment succeeds at the provider level and the webhook sets the order back to CONFIRMED. A CANCELLED order becomes CONFIRMED, inventory that was released is now potentially oversold, and the admin has no record of the issue.

**Fix needed:** Payment handlers must check `order.getStatus() == OrderStatus.CANCELLED` and reject the confirmation, returning a refund trigger or flagging for manual review.

### BUG-B02 — No CDEK webhook signature verification
**File:** `CdekWebhookController.java` + `CdekWebhookServiceImpl.java`

`POST /api/v1/delivery/cdek/webhook` accepts all incoming requests without verifying any CDEK-provided signature or token. Any attacker who knows the URL can POST fake CDEK status updates, marking orders as DELIVERED or RETURNED without any real shipment.

**Fix needed:** CDEK provides webhook authentication via an authorization token or HMAC. Implement signature verification before processing.

### BUG-B03 — Stateless refresh tokens with no revocation
**File:** `AuthServiceImpl.java`, `JwtService.java`

Refresh tokens are stateless JWTs. Once issued, they are valid for their full 14-day TTL regardless of logout, password change, or account deletion. A stolen refresh token cannot be invalidated without key rotation (which would invalidate all tokens globally).

**Fix needed:** Persist refresh token IDs in a `refresh_tokens` table and verify on each use. On logout or explicit revoke, delete the record.

### BUG-B04 — CdekWebhookServiceImpl.handle() scans entire table on every webhook
**File:** `CdekWebhookServiceImpl.java`

Every incoming CDEK webhook call invokes `shipmentRepository.findAll()` and then stream-filters by UUID in memory. With N shipments, each webhook is an O(N) full-table read. This will degrade linearly with order volume.

**Fix needed:** Add `findByCdekOrderUuid(String uuid)` to `CdekShipmentRepository` with an index on `cdek_order_uuid`.

---

## 3. High Severity Issues

### B-05 — Anyone can cancel a PayPal payment without authentication
**SecurityConfig.java:** `POST /api/v1/payments/paypal/cancel/**` is `permitAll`.

Any caller who knows or guesses a PayPal order ID (a UUID) can call this endpoint, marking the payment as CANCELLED and triggering inventory release and order expiry. PayPal order IDs are sent to the browser in the PayPal SDK initialization, making them discoverable.

### B-06 — PayPal webhookId defaults to blank — production webhooks rejected
**File:** `application.properties`

```properties
app.payment.paypal.webhook-id=${PAYPAL_WEBHOOK_ID:}
```

Empty default. If `PAYPAL_WEBHOOK_ID` is not set in production, the `verifyWebhookSignature` call to PayPal includes an empty ID. PayPal returns `FAILURE`, and all PayPal webhooks are rejected with a `BusinessRuleException`. All PayPal payments appear stuck at PENDING server-side even though the buyer completed payment. This is a silent failure mode.

**The docker-compose.prod.yml correctly requires `PAYPAL_WEBHOOK_ID` via `:?` — but if the app is run outside of Docker, nothing enforces this.**

### B-07 — PayPal CAPTURE event order ID extraction may use wrong ID
**File:** PayPal service `extractOrderId()` method

The primary path reads `supplementary_data.related_ids.order_id`. The fallback uses `resource.id`. For a PAYMENT_CAPTURE.COMPLETED event, `resource.id` is the CAPTURE ID, not the ORDER ID. If the supplementary path is absent, the fallback will look up a payment by capture ID (not stored), the lookup returns nothing, and the webhook is silently discarded with a log warning. PayPal payments complete but are never confirmed server-side.

### B-08 — PessimisticLockingFailureException returns HTTP 500
**File:** `RestExceptionHandler.java` — no handler for `PessimisticLockingFailureException`

When the inventory lock times out (3000ms hint on the query), Spring throws `PessimisticLockingFailureException`. The global `RestExceptionHandler` has no handler for it, so Spring Boot's default error handling returns HTTP 500 with a generic error body. The user (or frontend) gets no indication that this was a lock contention issue vs. a real server crash.

**Fix needed:** Add an `@ExceptionHandler(PessimisticLockingFailureException.class)` returning HTTP 409 with "Please retry — item temporarily unavailable."

### B-09 — POST /api/v1/catalog/\*\*/reviews returns 500 without auth
**SecurityConfig.java:** Only `GET /api/v1/catalog/**` is explicitly `permitAll`. `POST` requests fall through to `anyRequest().authenticated()`.

However, the `ReviewController` uses `@AuthenticationPrincipal UserDetails userDetails` which is `null` for unauthenticated requests. Since Spring correctly requires authentication for `POST /catalog/*/reviews`, an unauthenticated POST returns 401 (not 500) — but the controller code assumes a non-null `userDetails`. This is only safe because Spring's security filter blocks the request before reaching the controller. If the security config ever changes this path to `permitAll`, the controller will NPE.

**Fix needed:** Add explicit `@PreAuthorize("isAuthenticated()")` on the review controller method; do not rely on implicit `anyRequest()` chain for correctness.

### B-10 — authService.revokeAdmin() does full users table scan
**File:** `AuthServiceImpl.java`

`revokeAdmin()` calls `appUserRepository.findAll()` and streams to count admins. If there are 100,000 users, this loads all of them to count admins. Use a count query: `countByRolesContaining("ADMIN")`.

### B-11 — FreedomPay stub mode silently active if merchant ID is blank in prod
**File:** `FreedomPayHttpClient.java`

When `FREEDOMPAY_MERCHANT_ID` is empty, the client enters stub mode: calls return a fake URL without making any actual API call. If the env var is accidentally omitted from production, all "Freedom Pay" checkouts return a fake URL. Users proceed through payment without being charged. Orders get confirmed if a signed callback arrives (which it never will from stub mode), or they expire. This is a silent misconfiguration with a severe financial impact.

**Fix needed:** On non-test profiles, startup should fail-fast if `FREEDOMPAY_MERCHANT_ID` is blank.

---

## 4. Medium Severity Issues

| ID | Location | Issue |
|----|----------|-------|
| B-12 | `OrderServiceImpl.getAll()` | N+1 queries: `findByStatusNotInOrderByCreatedAtDesc` has no `@EntityGraph`; `OrderMapper` accesses `order.customer`, `order.deliveryAddress`, `order.orderItems` lazily |
| B-13 | `CatalogStorefrontServiceImpl.getDesignBySlug()` | N+1: inventory queried per garment in a stream; no batch fetch |
| B-14 | `OrderServiceImpl.createOrder()` | New `Customer` row created per order; no deduplication; `customers` table will grow unboundedly |
| B-15 | `OrderExpiryService.expireStaleOrders()` | Entire batch in one transaction; one failure rolls back all expirations in that cycle |
| B-16 | `PaymentServiceImpl.initPayment()` | Concurrent calls could both pass the PENDING-check before either inserts, causing DB UNIQUE violation; returned as HTTP 409 but no user-friendly message |
| B-17 | `FreedomPayCallbackController` | `pg_result = null` maps to PENDING status; a callback without `pg_result` leaves the payment permanently PENDING |
| B-18 | V7 migration | `ON DELETE SET NULL` on `order_items.color_id`, `.size_id`, `.design_garment_id` — deleting catalog data silently nullifies historical order items |
| B-19 | `InventoryRepository` | No DB CHECK `(quantity >= 0)` — negative inventory is technically possible |
| B-20 | `Order` entity | `status` and `deliveryType` columns have no `@Column(nullable=false)` in entity or DB CHECK |
| B-21 | `RestExceptionHandler` | No catch-all handler for `Exception` — unhandled exceptions return a different format than ProblemDetail |
| B-22 | `RestExceptionHandler` | No handler for `ConstraintViolationException` (path/param validation fails with 500 instead of 400) |
| B-23 | `DataIntegrityViolationException` handler | Leaks internal constraint name `uq_design_garment_type` in the response body |
| B-24 | Auth | No rate limiting on `/api/v1/auth/refresh` — a stolen refresh token can generate unlimited access tokens |

---

## 5. Low Severity Issues

| ID | Issue |
|----|-------|
| B-25 | `NEW` order status is never set by any code path — dead state in the FSM |
| B-26 | No `REFUNDED` order status — refunds are tracked at payment level only, not in order FSM |
| B-27 | `PayPalApiException` handler includes raw exception message in 502 response (potential detail leak) |
| B-28 | CORS configured in both `SecurityConfig` and `WebConfig` — redundant |
| B-29 | `CustomDesignServiceImpl` has no `@Transactional` — create operations not atomic |
| B-30 | `RegisterRequest.email` has no `@Email` annotation — only `@NotBlank`; "notanemail" passes validation |
| B-31 | No audit log for admin role grants — `POST /auth/admin/grant` has no history trail |
| B-32 | `reviewService.revokeAdmin()` is O(N users) — should use a count query |
| B-33 | V22 migration is a no-op `SELECT 1` — valid but unusual practice |

---

## 6. Positive Patterns

- **Inventory protection:** `PESSIMISTIC_WRITE` lock with 3000ms timeout on checkout ✓
- **Payment idempotency:** `ProcessedWebhookEvent` table with unique `(provider, event_id)` constraint ✓
- **Freedom Pay signature:** MD5 HMAC verified with constant-time comparison ✓; blank secretKey → reject ✓
- **Publication guards:** `DesignReadinessService` enforces KZT price + active garment before publish ✓
- **Order state machine:** explicit allowlist `ALLOWED_TRANSITIONS`; system transitions bypass admin guard ✓
- **Refresh token HttpOnly cookie:** refresh-cookie endpoint uses `SameSite=Strict` cookie ✓
- **JWT secret enforcement:** empty `JWT_SECRET` causes application to fail at startup ✓
- **Bootstrap admin:** no hardcoded admin account; created only if env vars are set ✓
- **Swagger disabled in prod:** `application-prod.properties` forces it off ✓
- **Rate limiting:** 5 sensitive endpoints covered with configurable per-IP limits ✓
- **EntityGraph on DesignRepository:** storefront and admin queries are N+1-free for collection/group ✓
- **Order expiry cleanup:** scheduled job releases inventory for abandoned orders ✓
- **XML escape in FreedomPay response:** `escapeXml()` prevents XSS via callback ✓
- **File upload magic-byte validation:** JPEG/PNG/GIF/WebP verified by header bytes, not just MIME type ✓
- **22 test classes, ~84 integration tests** — comprehensive domain coverage ✓

---

## 7. Order State Machine

```
PENDING_PAYMENT → CANCELLED (admin or expiry)
             ↓ (payment webhook)
NEW ──────────────────┐
             │        ↓
CONFIRMED → IN_PRODUCTION → READY → SHIPPED → DELIVERED
    └────────────────────────────────────────→ CANCELLED (admin only)

EXPIRED (system only; terminal)
```

**Gaps:**
- No `REFUNDED` state — admin cannot reflect a refund in order status
- `NEW` is dead code — no path sets it
- Late payment on a CANCELLED order re-confirms it (BUG-B01 above)
