# SECURITY_REVIEW.md
> Balgyn — Pre-Launch Security Review · 2026-06-21

---

## Executive Summary

The platform has a solid security foundation (stateless JWT, BCrypt, signature verification on payment callbacks, rate limiting, Swagger disabled in prod, HSTS headers). However, **four critical issues** must be resolved before accepting real payments:

1. No HTTPS/TLS — all traffic, including payment data, transmitted in plain text
2. Real credentials committed to `.env.example` (git-tracked)
3. Late payment on a cancelled order re-confirms it (no CANCELLED status check in payment handlers)
4. No CDEK webhook signature verification — delivery status can be forged

---

## 1. Transport Security

### SEC-01 — No HTTPS (CRITICAL)
**File:** `nginx/nginx.conf`

nginx listens only on port 80. There is no HTTPS listener, no TLS certificate, and no HTTP→HTTPS redirect. All traffic — including JWT tokens in `Authorization` headers, payment provider responses, and customer personal data — is transmitted in plain text.

**Impact:** Payment data, session tokens, and customer PII can be captured by any network observer between the client and server.

**Fix:** Configure nginx with Let's Encrypt (certbot) or a load-balancer TLS terminator. Add HTTP→HTTPS redirect.

### SEC-02 — HSTS set but ineffective
**File:** `SecurityConfig.java` line 100–103

```java
.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
```

HSTS is correctly configured in the Spring `headers` DSL, but HSTS only takes effect over HTTPS. Since there is no HTTPS, browsers never see this header and it provides no protection.

---

## 2. Secrets Management

### SEC-03 — Real credentials in git-tracked .env.example (CRITICAL)
**File:** `.env.example` (tracked by git, whitelisted in .gitignore via `!.env.example`)

The following real credentials are present in `.env.example`:

| Key | Value | Risk |
|-----|-------|------|
| `FREEDOMPAY_MERCHANT_ID` | `587060` | Real merchant ID |
| `FREEDOMPAY_SECRET_KEY` | `TOSTcm0z9miUgpyC` | Real secret key — can forge callbacks |
| `FREEDOMPAY_RESULT_URL` | `https://probing-padlock-bunkbed.ngrok-free.dev/...` | Exposed ngrok URL |
| `PAYPAL_CLIENT_ID` | `AXymWxzrWktAxw9...` | Sandbox client ID |
| `PAYPAL_CLIENT_SECRET` | `EFPZr6A1Vg2nbFT...` | Sandbox secret |
| `PAYPAL_WEBHOOK_ID` | `7AS768482S5495201a` | Webhook ID |
| `JWT_SECRET` | `dev-local-jwt-secret-change-me-please...` | Weak dev secret |

**Immediate action required:**
- Rotate `FREEDOMPAY_SECRET_KEY` (most critical — anyone with this can forge valid MD5 callbacks)
- Rotate `PAYPAL_CLIENT_SECRET`
- Replace all values in `.env.example` with placeholder text (e.g., `FREEDOMPAY_SECRET_KEY=REPLACE_WITH_YOUR_SECRET_KEY`)

### SEC-04 — DB password fallback '1234' in application.properties
**File:** `src/main/resources/application.properties` line 5

```properties
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:1234}
```

If `SPRING_DATASOURCE_PASSWORD` env var is not set, the application connects to PostgreSQL with password `1234`. The docker-compose.prod.yml enforces the env var via `:?`, but if the app is run outside of Docker, this fallback allows a silent connection with a trivially guessable password.

**Fix:** Remove the fallback — use `${SPRING_DATASOURCE_PASSWORD:?}` to fail fast.

---

## 3. Payment Security

### SEC-05 — Late payment re-confirms CANCELLED order (CRITICAL)
**File:** `PaymentServiceImpl.java` (`handleFreedomPayCallback`) and PayPal service

Both payment handlers check:
```java
if (order.getStatus() == PENDING_PAYMENT || order.getStatus() == NEW) {
    // confirm the order
}
```

They do NOT check `order.getStatus() != CANCELLED`. If admin cancels an order while the buyer has the checkout page open, a subsequent completed payment triggers this code path and sets the CANCELLED order back to CONFIRMED. The admin has no knowledge of this reversal.

### SEC-06 — Anyone can cancel a PayPal payment without authentication
**File:** `SecurityConfig.java`

`POST /api/v1/payments/paypal/cancel/{paypalOrderId}` is `permitAll`. PayPal order IDs are sent to the browser during checkout initialization. An attacker who observes or guesses a paypal order ID can cancel it, causing inventory to be released and the customer's order to expire.

### SEC-07 — Freedom Pay stub mode silently active on blank merchant ID
If `FREEDOMPAY_MERCHANT_ID` is blank (e.g., omitted from .env.prod), orders proceed to checkout and receive a fake payment URL. No real charge is made. The merchant loses revenue without knowing why.

---

## 4. Authentication & Authorization

### SEC-08 — No refresh token revocation (HIGH)
Refresh tokens are stateless JWTs valid for 14 days. Logout only clears the client-side cookie. The server cannot invalidate an issued refresh token. A stolen refresh token can generate valid access tokens for 14 days regardless of password changes or account deletion.

### SEC-09 — No rate limiting on /auth/refresh (MEDIUM)
The `/auth/refresh` and `/auth/refresh-cookie` endpoints are `permitAll` with no rate limiting. A stolen refresh token can generate unlimited access tokens per minute.

### SEC-10 — authService.revokeAdmin() full table scan (HIGH)
`revokeAdmin()` loads all users to count admins. On a database with many users this is an O(N) operation. Should use a count query.

### SEC-11 — No audit log for role grants (LOW)
`POST /auth/admin/grant` and `DELETE /auth/admin/revoke` have no audit trail. Who promoted whom is not recorded.

---

## 5. Webhook Security

### SEC-12 — No CDEK webhook verification (CRITICAL)
`POST /api/v1/delivery/cdek/webhook` accepts all incoming requests unconditionally. CDEK provides signing tokens. Without verification, any caller can forge CDEK status updates and mark shipments as DELIVERED or RETURNED.

### SEC-13 — Freedom Pay callback signature verification (POSITIVE)
MD5 HMAC using constant-time `MessageDigest.isEqual()`. Blank secretKey → callback rejected. XML response uses proper escaping. ✓

### SEC-14 — PayPal webhook verification (POSITIVE)
Delegates to PayPal's own `/v1/notifications/verify-webhook-signature` API. Rejection on failure. ✓

### SEC-15 — Webhook idempotency (POSITIVE)
`processed_webhook_events` table with unique `(provider, event_id)` prevents double-processing. ✓

---

## 6. Security Headers

| Header | Status | Notes |
|--------|--------|-------|
| `X-Content-Type-Options: nosniff` | ✓ Set | Spring Security default |
| `X-Frame-Options: DENY` | ✓ Set | `frameOptions(fo -> fo.deny())` |
| `Referrer-Policy: strict-origin-when-cross-origin` | ✓ Set | — |
| `Strict-Transport-Security` | Set but ineffective | No HTTPS |
| `Content-Security-Policy` | **ABSENT** | Not configured anywhere |
| `Permissions-Policy` | **ABSENT** | Not configured anywhere |

### SEC-16 — No Content-Security-Policy (HIGH)
No CSP header is configured in nginx or Spring. CSP is the primary browser defense against XSS. While React's JSX auto-escaping protects against stored XSS, the absence of CSP means there is no defense-in-depth.

**Recommended minimum CSP:**
```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://media.balgyn.kz; frame-ancestors 'none'
```

---

## 7. Input Validation

| Area | Status |
|------|--------|
| SQL injection | ✓ Not possible — Spring JPA parameterized queries throughout |
| XSS via React | ✓ JSX auto-escapes all user content |
| CSRF | ✓ Disabled (correct for stateless JWT — no session cookies for mutations) |
| File upload MIME | ✓ Magic-byte validation on image upload |
| File upload size | ✓ 8MB limit enforced at Spring + nginx level |
| Phone number validation | ✗ No format validation — any non-empty string accepted |
| Email validation | ✗ `RegisterRequest.email` has no `@Email` constraint |
| Order amount validation | ✓ Amount verified against stored order total (0.01 tolerance) |
| XML injection in Freedom Pay response | ✓ `escapeXml()` used in callback response |

---

## 8. Data Privacy

| Area | Status |
|------|--------|
| Password storage | ✓ BCrypt |
| JWT secret in logs | ✓ Not logged |
| Payment data in logs | ✓ Only `pg_order_id` and `pg_result` logged; no card data |
| `lastWebhookPayload` in API response | ⚠ Raw webhook body (may contain PII) stored in `payments` table — verify mapper excludes it from `PaymentResponse` |
| MinIO URLs | Public, permanent — images are always readable by URL; acceptable for catalog images |
| Customer phone in delivery address | Stored in `delivery_addresses` — confirm it is not included in any unauthenticated API response |

---

## 9. CORS

**Status:** Correct. Configured from `ALLOWED_ORIGINS` env var (comma-separated). No wildcards. `allowCredentials=true`. `maxAge=1800s`. Only `/api/**` paths covered.

**Dev default:** `localhost:5173-5175` — appropriate for development.

**Production:** Must set `ALLOWED_ORIGINS=https://balgyn.kz` (enforced by `:?` in docker-compose.prod.yml).

---

## 10. Miscellaneous

| Item | Status |
|------|--------|
| Swagger in prod | ✓ Disabled by `application-prod.properties`; nginx passes requests but Spring returns 403 |
| Cookie Secure flag | ✓ `true` in prod profile; default `false` in dev |
| Trust-proxy (XFF) | ✓ `true` in prod profile; `false` in dev |
| MinIO console exposed | ✓ Port 9001 not exposed in docker-compose.prod.yml |
| PostgreSQL port | ✓ Not exposed in docker-compose.prod.yml (exposed in dev compose) |
