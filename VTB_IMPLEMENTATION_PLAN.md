# VTB Kazakhstan — Implementation Plan

**Date:** 2026-06-25  
**Constraint:** No code written yet. This is a pre-implementation plan for approval.

---

## Pre-Conditions

Before starting implementation, confirm with VTB Kazakhstan:
- [ ] Sandbox credentials (login, password) from `https://sandbox.vtb-bank.kz/cabinet/`
- [ ] HMAC secret key for callback verification
- [ ] Confirmed supported currencies for this merchant account
- [ ] Callback URL registration in sandbox merchant cabinet
- [ ] Production merchant agreement signed (for production only)

---

## Phase 0 — Flyway Migration

**File:** `V32__add_vtb_kz_provider.sql`

Changes:
- Alter `payment_provider` enum: `ALTER TYPE payment_provider ADD VALUE 'VTB_KZ'`
- No other schema changes needed — all VTB data fits existing `payments` table columns

**Risk:** PostgreSQL enum alteration is DDL but non-destructive. Safe migration.

---

## Phase 1 — Core Infrastructure (No business logic)

**Step 1.0 — PaymentInitRequest: add `provider` field** ⚠️ REQUIRED BEFORE VTB routing works

Current `PaymentInitRequest` has only `orderId` and `returnUrl`. Without a `provider` field,
`initPayment()` cannot route to VTB_KZ.

```java
// PaymentInitRequest.java — add provider field
public record PaymentInitRequest(
    @NotNull @Positive Long orderId,
    @NotNull PaymentProvider provider,           // ← ADD: required for routing
    @Size(max = 500)
    @Pattern(regexp = "^(https?)://[\\w.-].*", message = "returnUrl должен быть валидным http/https URL")
    String returnUrl
) {}
```

**Backward compatibility:** Frontend must send `"provider": "FREEDOM_PAY"` for existing FreedomPay flow.
Alternative: make `provider` optional with `@JsonSetter(nulls = SKIP)` and default to `FREEDOM_PAY`
if absent (fully backward compatible).

**Step 1.0b — Fix initPayment() idempotency hardcoding** ⚠️ REQUIRED

Current code in `PaymentServiceImpl.initPayment()` hardcodes `PaymentProvider.FREEDOM_PAY`:
```java
// EXISTING (WRONG after VTB is added):
.findByOrderAndProviderAndStatus(order, PaymentProvider.FREEDOM_PAY, PaymentStatus.PENDING)

// CORRECT:
.findByOrderAndProviderAndStatus(order, request.provider(), PaymentStatus.PENDING)
```

This must be fixed or VTB_KZ idempotency check will always look for FreedomPay payments.
The routing switch `case VTB_KZ → initVtbPayment(...)` must also branch BEFORE this check,
since `createNewPayment()` is FreedomPay-specific.

**Step 1.1 — PaymentProvider enum**
- Add `VTB_KZ` value

**Step 1.2 — VtbProperties**
- `@ConfigurationProperties("app.payment.vtb")`
- All fields from architecture doc
- `isStubMode()`, `getEffectiveApiUrl()`

**Step 1.3 — VtbOrderStatus enum**
- Values: REGISTERED(0), PRE_AUTHORIZED(1), DEPOSITED(2), DECLINED(3), REVERSED(4), REFUNDED(5), UNKNOWN
- Static `fromCode(int)` factory method

**Step 1.4 — VTB DTOs**
- `VtbRegisterRequest` — fields per register.do spec
- `VtbRegisterResponse` — orderId, formUrl, errorCode, errorMessage
- `VtbOrderStatusResponse` — full getOrderStatusExtended response fields

**Step 1.5 — application.properties**
- Add all `app.payment.vtb.*` properties with env var defaults

**Tests in this phase:** Unit tests for VtbOrderStatus.fromCode(), VtbProperties.isStubMode()

---

## Phase 2 — Callback Verifier

**Step 2.1 — VtbCallbackVerifier**

Returns `VtbChecksumResult` enum (not boolean):
- `VALID` / `INVALID` / `SKIPPED` (blank secret) / `ABSENT` (no checksum param)

Algorithm: `SHA256(sorted_params_semicolons ; callbackSecret)` — format TBD
- Remove `checksum`, sort by key, build semicolon-delimited string (values-only first attempt)
- Use `MessageDigest.getInstance("SHA-256")`, uppercase hex, case-insensitive compare

**CALLER BEHAVIOR:** Result is logged but does NOT gate the API verification.
`getOrderStatusExtended.do` is ALWAYS called regardless of checksum result.

> **Checksum verification is OPTIONAL** (confirmed from both official CMS plugins).
> The Magento plugin has `isPaymentValid()` as a placeholder that returns `true`.
> The WordPress plugin ignores `checksum` entirely.
> Both proceed directly to `getOrderStatusExtended.do` as the only security gate.

**Tests:**
- callbackSecret blank → returns `SKIPPED`
- checksum absent from params → returns `ABSENT`
- Known params + known secret → returns `VALID` (verify format against sandbox)
- Tampered params → returns `INVALID`
- Both results do NOT change payment processing flow (service always calls API)

---

## Phase 3 — HTTP Client

**Step 3.1 — VtbHttpClient**
- `register(VtbRegisterRequest): VtbRegisterResponse`
- `getOrderStatus(String vtbOrderId): VtbOrderStatusResponse`
- `refund(String vtbOrderId, Long amountMinorUnits): void`
- `reverse(String vtbOrderId): void`
- `cancel(String vtbOrderId): void`

**Stub mode:**
- `register()` → return fake VtbRegisterResponse with formUrl = `{stubBaseUrl}/api/v1/payments/stub/vtb/{orderId}?mdOrder=stub-{UUID}`
- `getOrderStatus()` → return DEPOSITED (orderStatus=2) for stub orders
- `refund()`, `reverse()`, `cancel()` → no-op with log

**Tests (unit, no network):**
- Mock HTTP responses for each endpoint
- Error handling: errorCode != 0 → throws VtbApiException
- Stub mode returns fake responses

---

## Phase 4 — Currency Mapper

**Step 4.1 — VtbCurrencyMapper**
- `mapCurrency(String currency, VtbProperties props): int`
- Uses static ISO map
- Throws `BusinessRuleException` if currency unknown
- Throws `BusinessRuleException` if not in supported list AND fallback=false
- Returns 398 if fallback=true

**Step 4.2 — Amount conversion utility**
- `toMinorUnits(BigDecimal amount): long`
- `amount.multiply(100).setScale(0, HALF_UP).longValueExact()`

**Tests:**
- "KZT" → 398
- "USD" → 840 (when in supported list)
- "RUB" → throw (when not in list and no fallback)
- "XYZ" → throw (unknown currency)
- 12000.00 → 1200000L

---

## Phase 5 — Payment Service Integration

**Step 5.1 — Extend PaymentServiceImpl**

Add to `initPayment()`:
```
case VTB_KZ → initVtbPayment(order, request)
```

Implement `initVtbPayment()`:
1. Idempotency check for existing VTB_KZ PENDING payment
2. Currency mapping
3. VtbHttpClient.register()
4. Create Payment entity
5. Return PaymentResponse

**Step 5.2 — handleVtbCallback()**

1. Extract `mdOrder` from params — return no-op if missing
2. `ProcessedWebhookEvent` dedup by `(VTB_KZ, mdOrder)` — return immediately if already processed
3. `VtbCallbackVerifier.verifyChecksum(params, hmacSecret)` — log result, ALWAYS continue regardless
4. `vtbHttpClient.getOrderStatus(mdOrder)` → `VtbOrderStatusResponse`
5. **Amount validation** (compare with VTB response, not callback params):
   - `vtbResponse.amount` (minor units) == `payment.amount * 100` (with tolerance)
   - Amount is in minor units in VTB response; do NOT pass raw callback `amount` to `validateAmount()`
   - If mismatch → log WARN, continue (don't reject — may be a legitimate rounding difference)
6. Map `VtbOrderStatus → PaymentStatus`
7. Load `Payment` by `providerPaymentId == mdOrder`
8. Update payment status, set `lastWebhookPayload`, `updatedAt`
9. **If SUCCEEDED** and order in PENDING_PAYMENT/NEW:
   - `order.status = CONFIRMED`, `order.updatedAt = now()`, save
10. **If FAILED** (DECLINED/REVERSED/REFUNDED) and order in PENDING_PAYMENT:
    - Call `orderExpiryService.expire(order)` — releases inventory, marks EXPIRED
    - ⚠️ This mirrors FreedomPay behavior: `orderExpiryService.expire()` on payment FAILED
11. Save `ProcessedWebhookEvent(VTB_KZ, mdOrder, payment)`

**Race condition between callback and returnUrl handler:**
Both paths call `getOrderStatusExtended` and update payment. Prevention:
- Callback: uses `ProcessedWebhookEvent` as idempotency gate
- ReturnUrl handler: check `payment.status == SUCCEEDED` first → if already confirmed, return immediately
- If returnUrl handler processes first (callback not yet arrived): update payment+order SUCCEEDED,
  save `ProcessedWebhookEvent` — when callback arrives, dedup check will short-circuit it
- Same pattern as FreedomPay `verifyFreedomPayRedirect` → calls `handleFreedomPayCallback` which
  itself checks `processedEventRepository.existsByProviderAndEventId`

**Tests (integration with H2):**
- initVtbPayment → creates PENDING payment with VTB_KZ provider
- handleVtbCallback DEPOSITED → payment=SUCCEEDED, order=CONFIRMED
- handleVtbCallback DECLINED → payment=FAILED, order=EXPIRED
- handleVtbCallback blank secret → SKIPPED checksum, order still CONFIRMED
- handleVtbCallback invalid checksum → WARN logged, order still CONFIRMED (API is truth)
- handleVtbCallback duplicate → idempotent, no double-update
- handleVtbCallback then returnUrl verify → second call is no-op (already SUCCEEDED)
- Amount mismatch in VTB response → WARN logged, processing continues

---

## Phase 6 — Controller

**Step 6.1 — VtbCallbackController**
- `GET /api/v1/payments/callback/vtb-kz`  ← GET, not POST (confirmed from official plugin)
- Accept `@RequestParam Map<String,String>` from URL query string
- Call paymentService.handleVtbCallback()
- Always return HTTP 200

**Step 6.2 — SecurityConfig**
- Add `GET /api/v1/payments/callback/vtb-kz` → `permitAll()`
  (VTB callback is GET, not POST — existing POST wildcard does NOT cover it)
- Add `POST /api/v1/payments/vtb-kz/verify-return` → `permitAll()`
- Note: `GET /api/v1/payments/stub/**` already covered by existing rule

**Step 6.3 — PaymentStubController**
- Add stub redirect for VTB: `GET /api/v1/payments/stub/vtb/{orderId}`
- Same pattern as FreedomPay stub: auto-confirm + redirect to `/payment/success`

**Step 6.4 — Backend: VtbReturnController**
- `POST /api/v1/payments/vtb-kz/verify-return`
- Accept `{ vtbOrderId: String }` (the `orderId` param VTB appended to returnUrl)
- Permitted: `permitAll()` (customer-facing, no JWT)
- Calls `paymentService.verifyVtbReturn(vtbOrderId)`:
  1. Find `Payment` by `providerPaymentId == vtbOrderId`
  2. If already SUCCEEDED → return immediately (callback may have arrived first)
  3. Call `vtbHttpClient.getOrderStatus(vtbOrderId)` → status
  4. If SUCCEEDED → update payment+order (same logic as step 9 in handleVtbCallback)
     Save `ProcessedWebhookEvent(VTB_KZ, vtbOrderId)` to prevent callback from re-processing
  5. Return `PaymentResponse`

**Step 6.5 — Frontend: PaymentReturnPage.tsx**
- VTB returnUrl redirects back to `/payment-return?orderId={vtbUUID}`
- Detect `orderId` param (from VTB, which is the gateway UUID = mdOrder)
- Call `POST /api/v1/payments/vtb-kz/verify-return` with `{ vtbOrderId: orderId }`
- Show success/failure based on response payment status

> **Note on returnUrl orderId:** VTB appends `?orderId=` (gateway UUID), confirmed from plugin:
> `$args['orderId'] = isset($_GET['orderId']) ? sanitize_text_field($_GET['orderId']) : null;`
> This is the SAME value as `mdOrder` in callbacks — it's the register.do `orderId` response field.

---

## Phase 7 — Testing

### Unit Tests
- `VtbCallbackVerifierTest` — 4+ test cases
- `VtbOrderStatusTest` — enum mapping
- `VtbCurrencyMapperTest` — all currencies
- `VtbHttpClientTest` — mock responses, stub mode

### Integration Tests (H2 + Flyway off)
- `VtbPaymentFlowTest` — full flow: init → callback → confirmed
- `VtbCallbackIdempotencyTest` — duplicate callback handled
- `VtbRefundTest` — refund flow

### Expected result: All existing 134 tests still green + 15–20 new VTB tests

---

## Phase 8 — Configuration & Deployment

**Step 8.1 — docker-compose.yml**
```yaml
# Add VTB environment variables (blank = stub mode)
- VTB_USERNAME=${VTB_USERNAME:-}
- VTB_PASSWORD=${VTB_PASSWORD:-}
- VTB_HMAC_SECRET=${VTB_HMAC_SECRET:-}
- VTB_CALLBACK_URL=${VTB_CALLBACK_URL:-}
- VTB_SANDBOX=${VTB_SANDBOX:-false}
```

**Step 8.2 — .env.example**
```bash
# VTB Kazakhstan (blank = stub mode)
VTB_USERNAME=
VTB_PASSWORD=
VTB_HMAC_SECRET=
VTB_CALLBACK_URL=https://yourngrok.ngrok.io/api/v1/payments/callback/vtb-kz
VTB_SANDBOX=true
VTB_SUPPORTED_CURRENCIES=398
```

---

## Implementation Order Summary (Revised after pre-implementation audit)

```
1.  PaymentInitRequest — add provider field  ← PREREQUISITE
2.  PaymentServiceImpl — fix initPayment() provider routing  ← PREREQUISITE
3.  PaymentProvider enum — add VTB_KZ
4.  VtbOrderStatus enum
5.  VtbProperties
6.  VTB DTOs (VtbRegisterRequest, VtbRegisterResponse, VtbOrderStatusResponse)
7.  application.properties
8.  VtbCallbackVerifier + VtbChecksumResult enum
9.  VtbCurrencyMapper + amount utility
10. VtbHttpClient (with stub mode)
11. PaymentServiceImpl — initVtbPayment() branch
12. PaymentServiceImpl — handleVtbCallback()
13. PaymentServiceImpl — verifyVtbReturn()
14. PaymentServiceImpl — refundVtbPayment()
15. VtbCallbackController (GET /callback/vtb-kz)
16. VtbReturnController (POST /vtb-kz/verify-return)
17. PaymentStubController — add VTB stub
18. SecurityConfig — add GET /callback/vtb-kz + POST /vtb-kz/verify-return
19. Flyway V32 — ALTER TYPE payment_provider ADD VALUE 'VTB_KZ'
20. docker-compose.yml — VTB env vars
21. PaymentReturnPage.tsx — VTB orderId detection
22. All tests (134 existing green + 15-20 new VTB)
```

---

## Estimated Effort

| Phase | Complexity | Est. Lines |
|-------|-----------|-----------|
| Enums + DTO | Low | ~100 |
| VtbProperties | Low | ~60 |
| VtbCallbackVerifier | Medium | ~80 |
| VtbCurrencyMapper | Low | ~60 |
| VtbHttpClient | Medium | ~200 |
| PaymentServiceImpl extension | High | ~150 |
| VtbCallbackController | Low | ~50 |
| Stub controller | Low | ~40 |
| Tests | High | ~400 |
| Frontend | Low | ~30 |
| Config/Migration | Low | ~30 |
| **TOTAL** | | **~1200 lines** |

---

## Risk Register

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| VTB callback checksum format differs from documented | Medium | High | Verify with sandbox test before going live |
| RUB currency not supported by Kazakhstan merchant | High | Medium | Default to KZT only, throw error for RUB |
| VTB return URL params not documented | Medium | Medium | Always verify via getOrderStatusExtended instead |
| Sandbox unavailable or credentials expired | Low | Medium | Use stub mode for local dev |
| HMAC secret format wrong (base64/hex) | Medium | Medium | Test with sandbox callback before launch |
| Mir card acceptance restrictions | Low | Low | Accept Mir, handle decline via normal flow |

---

## Production Checklist

- [ ] Merchant agreement signed with VTB Kazakhstan
- [ ] Production credentials (VTB_USERNAME, VTB_PASSWORD) received
- [ ] HMAC secret (VTB_HMAC_SECRET) received
- [ ] Callback URL registered with VTB merchant cabinet (production)
- [ ] VTB_CALLBACK_URL set to HTTPS production URL
- [ ] VTB_SANDBOX=false in production env
- [ ] VTB_RETURN_URL set to production frontend URL
- [ ] E2E test with real VTB sandbox card before production switch
- [ ] Load test: check VTB API rate limits under expected traffic
- [ ] Monitor: add alerting for VTB callback failures
