# VTB Kazakhstan — Pre-Implementation Architecture Audit

**Date:** 2026-06-25  
**Auditor role:** Senior Java Backend Architect + Senior Payments Engineer  
**Verdict:** READY FOR IMPLEMENTATION (after fixes documented below)

---

## Sources audited

| Document | Status |
|----------|--------|
| `VTB_API_RESEARCH.md` | ✅ Audited — 2 corrections applied |
| `VTB_FINAL_AUDIT.md` | ✅ Audited — no changes needed |
| `VTB_INTEGRATION_ARCHITECTURE.md` | ✅ Audited — 4 corrections applied |
| `VTB_IMPLEMENTATION_PLAN.md` | ✅ Audited — 5 additions applied |
| `VTB_PAYMENT_FLOW.md` | ✅ Audited — 2 diagram corrections applied |
| `VTB_MULTI_CURRENCY_DESIGN.md` | ✅ Audited — 1 correction applied |
| `ZERO_CONFIG_SETUP.md` | ✅ Updated — VTB stub section added |

Existing Java code also reviewed:
- `PaymentServiceImpl.java` — critical issues found
- `PaymentService.java` — extension strategy confirmed
- `PaymentInitRequest.java` — critical missing field found
- `PaymentProvider.java` — ready for VTB_KZ addition
- `PaymentStatus.java` — REFUNDED already exists ✅
- `Payment.java` — compatible as-is ✅
- `ProcessedWebhookEvent.java` — reusable for VTB ✅
- `PaymentRepository.java` — `findByOrderAndProviderAndStatus` ready ✅
- `SecurityConfig.java` — issue found (GET callback not covered)
- `PaymentStubController.java` — pattern to replicate for VTB stub
- `OrderExpiryService.java` — must be called on VTB FAILED

---

## Issues Found

### 🔴 CRITICAL (2) — Must fix before ANY implementation

---

#### CRITICAL-1: `PaymentInitRequest` missing `provider` field

**File:** `src/main/java/com/nurba/java/dto/request/PaymentInitRequest.java`

**Current state:**
```java
public record PaymentInitRequest(
    @NotNull @Positive Long orderId,
    String returnUrl
) {}
```

**Problem:** `initPayment()` in `PaymentServiceImpl` has no way to determine which provider
to route to. VTB_KZ, FreedomPay, and future providers all use the same endpoint.
Without a `provider` field, the routing switch `case VTB_KZ → initVtbPayment()` is impossible.

**Fix:** Add `PaymentProvider provider` field to `PaymentInitRequest`.
If backward compatibility is needed: annotate with `@JsonSetter(nulls = Nulls.SKIP)` and
default to `FREEDOM_PAY` when absent.

**Fixed in:** `VTB_IMPLEMENTATION_PLAN.md` Step 1.0

---

#### CRITICAL-2: Idempotency hardcoded to `FREEDOM_PAY` in `initPayment()`

**File:** `src/main/java/com/nurba/java/service/Impl/PaymentServiceImpl.java:69-72`

**Current state:**
```java
return paymentRepository
    .findByOrderAndProviderAndStatus(order, PaymentProvider.FREEDOM_PAY, PaymentStatus.PENDING)
    .map(PaymentServiceImpl::toResponse)
    .orElseGet(() -> toResponse(createNewPayment(order, request)));
```

**Problem:** Two bugs in one:
1. VTB_KZ idempotency check will search for FreedomPay payments → never finds VTB_KZ → creates new payment every call
2. If user attempts FreedomPay then VTB_KZ on same order, FreedomPay PENDING is returned as VTB_KZ result

**Fix:** The routing switch must happen BEFORE this check. Each provider branch handles its own
idempotency check with the correct provider enum value.

**Fixed in:** `VTB_IMPLEMENTATION_PLAN.md` Step 1.0b

---

### 🟠 HIGH (8) — Documentation errors that would cause runtime bugs

---

#### HIGH-1: VTB callback HTTP method wrong in 3 documents

**Problem:** VTB callback is GET (confirmed from plugin), not POST.

| Document | Section | Error | Fixed |
|----------|---------|-------|-------|
| `VTB_API_RESEARCH.md` | §1.7 | "VTB sends a **POST** request" | ✅ |
| `VTB_PAYMENT_FLOW.md` | Diagram 1, 2 | `POST {callbackUrl}` | ✅ |
| `VTB_INTEGRATION_ARCHITECTURE.md` | §3.3 SecurityConfig | `HttpMethod.POST` for callback | ✅ |

**Runtime impact if unfixed:** `VtbCallbackController` mapped as `@GetMapping` would never receive
the VTB callback. SecurityConfig `POST /callback/**` permitAll doesn't cover GET requests.
VTB retries would accumulate and orders would stay PENDING_PAYMENT forever.

---

#### HIGH-2: Callback operation name `authorized` → `approved`

**Problem:** `VTB_API_RESEARCH.md` §1.7 listed `authorized` as an operation name.
Correct value: `approved` (confirmed from plugin: `"deposited,approved,declinedByTimeout,..."`).

**Runtime impact:** If code switches on operation name, `approved` would fall into `default` branch
and be treated incorrectly.

**Fixed in:** `VTB_API_RESEARCH.md` §1.7 ✅

---

#### HIGH-3: Callback diagram shows "Verify HMAC-SHA256" as required step

**Problem:** `VTB_PAYMENT_FLOW.md` Diagram 1 showed checksum verification as a mandatory step.
Final architecture decision: verification is optional (official plugins skip it entirely).

**Runtime impact:** If implemented as mandatory block, any callback with missing or incorrect
checksum would reject a legitimate payment and leave order in PENDING_PAYMENT.

**Fixed in:** `VTB_PAYMENT_FLOW.md` ✅, `VTB_INTEGRATION_ARCHITECTURE.md` §2.9 ✅

---

#### HIGH-4: `handleVtbCallback` §2.9 said "Verify HMAC-SHA256"

**Problem:** `VTB_INTEGRATION_ARCHITECTURE.md` §2.9 Step 3 said "Verify HMAC-SHA256 via VtbCallbackVerifier".
HMAC-SHA256 is wrong (algorithm is SHA256-concat); and checksum is optional, not mandatory.

**Fixed in:** `VTB_INTEGRATION_ARCHITECTURE.md` §2.9 completely rewritten ✅

---

#### HIGH-5: `orderExpiryService.expire()` not called on VTB FAILED payment

**Problem:** FreedomPay callback handler calls `orderExpiryService.expire()` when payment FAILS
(releases inventory, marks order EXPIRED). VTB implementation plan did not include this step.

**Without this fix:** When a VTB payment fails/declines, the order stays in PENDING_PAYMENT
indefinitely until the scheduler picks it up (up to 60 minutes). Inventory remains locked
during this window.

**Fixed in:** `VTB_IMPLEMENTATION_PLAN.md` Step 5.2 ✅

---

#### HIGH-6: Race condition between callback and returnUrl handler — not documented

**Problem:** Both `/callback/vtb-kz` (GET) and the return URL handler call `getOrderStatusExtended.do`
and update the same `Payment` record. Under normal timing (VTB sends callback before browser redirect
completes), both arrive nearly simultaneously.

**Risk:** Double payment confirmation, possible `ProcessedWebhookEvent` UNIQUE constraint violation
if both try to insert the same `(VTB_KZ, mdOrder)` record.

**Prevention strategy documented in:** `VTB_INTEGRATION_ARCHITECTURE.md` §8 ✅ and `VTB_IMPLEMENTATION_PLAN.md` Step 5.2 ✅

**Solution:** Both paths use `ProcessedWebhookEvent.existsByProviderAndEventId` as the idempotency
gate. The first to insert wins; the second's duplicate attempt causes a `DataIntegrityViolationException`
that must be caught and treated as "already processed" (same pattern as existing FreedomPay code).

---

#### HIGH-7: Amount validation: VTB uses minor units, `validateAmount()` uses standard units

**Problem:** Existing `validateAmount()` in `PaymentServiceImpl` compares `BigDecimal` values
assuming the same unit. VTB callback and `getOrderStatusExtended` return amounts in minor units
(tiyn = 1/100 KZT). `payment.amount` is stored as 12000.00 (KZT). Comparing them directly gives
12000.00 vs 1200000 → always "mismatch".

**Fix:** Amount validation for VTB must convert VTB minor units to standard: `vtbAmount / 100`.

**Fixed in:** `VTB_IMPLEMENTATION_PLAN.md` Step 5.2 ✅

---

#### HIGH-8: SecurityConfig existing POST wildcard does NOT cover VTB GET callback

**Problem:** Existing `SecurityConfig`:
```java
.requestMatchers(HttpMethod.POST, "/api/v1/payments/callback/**").permitAll()
```
Covers FreedomPay callback (POST). Does NOT cover VTB callback (GET).

**Fix:** Add:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/payments/callback/vtb-kz").permitAll()
```

**Fixed in:** `VTB_INTEGRATION_ARCHITECTURE.md` §3.3 ✅ and `VTB_IMPLEMENTATION_PLAN.md` Step 6.2 ✅

---

### 🟡 MEDIUM (3) — Inconsistencies between documents

---

#### MEDIUM-1: `VTB_FALLBACK_TO_KZT` default inconsistency

**Problem:** `VTB_MULTI_CURRENCY_DESIGN.md` §3.2 had `vtb.fallback-to-kzt: ${VTB_FALLBACK_TO_KZT:true}` (default true).
All other documents (`VTB_INTEGRATION_ARCHITECTURE.md`, `VTB_IMPLEMENTATION_PLAN.md`) had default `false`.

**Correct default:** `false` — throw `BusinessRuleException` if currency not supported.
Silent fallback from USD to KZT would charge the customer in the wrong currency without their knowledge.

**Fixed in:** `VTB_MULTI_CURRENCY_DESIGN.md` §3.2 ✅

---

#### MEDIUM-2: returnUrl handler responsibilities unclear

**Problem:** `VTB_IMPLEMENTATION_PLAN.md` Phase 6.4 said "Call new backend endpoint `POST /api/v1/payments/vtb-kz/verify-return` with vtbOrderId".
But the actual logic (what it updates, how it interacts with callback) was not specified.

**Fix:** Added complete specification for `VtbReturnController` with explicit:
- FindPayment → check if already SUCCEEDED → skip if yes
- Call getOrderStatus → update payment AND order (not just payment)
- Save `ProcessedWebhookEvent` to prevent callback re-processing the same event

**Fixed in:** `VTB_IMPLEMENTATION_PLAN.md` Step 6.4 ✅

---

#### MEDIUM-3: `ZERO_CONFIG_SETUP.md` had no VTB section

**Problem:** Document described stub behavior for FreedomPay and PayPal but not VTB.
A developer cloning the repo would see no guidance on VTB stub mode.

**Fixed in:** `ZERO_CONFIG_SETUP.md` — added VTB stub description, activation conditions, stub endpoints ✅

---

## Multi-Currency Audit

### Does current architecture support KZT?

**YES, unconditionally.** KZT (ISO 398) is the default merchant currency for all VTB Kazakhstan
accounts. `VtbCurrencyMapper` always includes 398 in supported set. `BPC_VTBKZ_MANDATORY_CURRENCY=true`
confirms we must always send `currency=398` explicitly.

### Does current architecture support RUB?

**NO for production Kazakhstan merchants without explicit agreement.**

The RBS platform technically accepts `currency=643`. However:
- VTB Kazakhstan is a Kazakhstani legal entity (ARRFR RK license)
- Kazakhstan merchants typically have KZT accounts only
- RUB acquiring requires a separate RUB merchant account + regulatory compliance
- Official documentation does NOT confirm RUB for Kazakhstan merchants

**What will happen if `VTB_SUPPORTED_CURRENCIES=398` and order.currency=RUB:**
With `VTB_FALLBACK_TO_KZT=false` (correct default) → `BusinessRuleException` is thrown → HTTP 422.
Frontend shows error. Customer must switch to KZT or another payment method.

### What should happen on frontend when currency not supported?

FE receives HTTP 422 with message: `"VTB KZ does not support RUB payments..."`.
Should display: "This payment method does not support RUB. Please select KZT or use FreedomPay."
**No silent currency conversion on frontend.** Customer must explicitly change currency.

### Where should currency conversion happen?

**Only in `VtbCurrencyMapper`**, which:
1. Maps currency string to ISO numeric
2. Checks if numeric is in merchant's `supportedCurrencies` list
3. Throws `BusinessRuleException` if not supported and `fallbackToKzt=false`
4. Returns the ISO code to use in `register.do`

**There should be no currency conversion** (e.g., USD→KZT via ExchangeRateService) unless the
merchant has explicitly signed a multi-currency agreement AND the customer has been informed.

### Where is silent fallback NOT allowed?

Silent currency conversion (charging in KZT when customer selected USD) is prohibited because:
1. The customer sees USD price on screen, KZT is charged at bank exchange rate → unexpected amount
2. This is illegal in many jurisdictions (charging different currency than displayed)
3. PCI DSS and banking regulations require the charged currency to match the displayed price

`VTB_FALLBACK_TO_KZT=false` is the correct production default. Never set it to `true` in production.

---

## Callback Security Audit

### Is `getOrderStatusExtended.do` sufficient as the only source of truth?

**YES.** The API call:
1. Requires `userName`+`password` — credentials only the merchant server has
2. Cannot be forged without API credentials
3. Returns the actual payment state from the bank's core system
4. Is the same mechanism both official plugins (WordPress and Magento) use exclusively

An attacker who can send a fake callback GET request to `GET /callback/vtb-kz` cannot control
what `getOrderStatusExtended.do` returns — that response comes from VTB's server authenticated
with our credentials.

### Should we verify the callback checksum?

**Optional defense-in-depth, not a security gate.** Two official plugins skip it entirely.
The checksum mechanism exists (portal generates a token) but was never implemented in shipped plugins.

If configured (`VTB_HMAC_SECRET` set):
- Log the verification result
- INVALID → log WARN, proceed to API call anyway
- Can be promoted to hard gate later by changing one condition

If not configured:
- Skip entirely (SKIPPED result)
- Proceed to API call

### Should checksum INVALID block order confirmation?

**NO.** Reasons:
1. Both official plugins don't verify checksum at all — we'd be blocking valid callbacks
2. The checksum exact format is unconfirmed (values-only vs key=value) — wrong algorithm = always INVALID
3. `getOrderStatusExtended.do` is sufficient security by itself
4. A WARN log on checksum mismatch provides audit trail

### Is skipping checksum verification secure?

**YES, under the following conditions:**
1. `getOrderStatusExtended.do` is ALWAYS called (never short-circuit on callback params alone)
2. The callback URL is HTTPS (prevents network interception of the mdOrder parameter)
3. `ProcessedWebhookEvent` prevents replay attacks (same mdOrder processed only once)
4. Rate limiting is applied to the callback endpoint

An attacker sending fake callbacks can trigger `getOrderStatusExtended.do` calls for arbitrary
order IDs — these will return status=0 (not paid) and the order confirmation won't happen.
This is at most a DoS on API rate limits, not a fraudulent confirmation.

---

## Compatibility with FreedomPay and PayPal

### Does VTB break FreedomPay?

**NO.** FreedomPay uses:
- `POST /api/v1/payments/init` — same endpoint, will route by `provider=FREEDOM_PAY`
- `POST /api/v1/payments/callback/**` — existing POST wildcard still covers it
- `PaymentProvider.FREEDOM_PAY` — enum still exists, VTB_KZ only added

The only code change in existing files: `PaymentInitRequest` gets a new `provider` field.
If optional with default `FREEDOM_PAY`, all existing FreedomPay frontend code is backward compatible.

### Does VTB break PayPal?

**NO.** PayPal uses separate endpoints (`/paypal/create-order`, `/paypal/capture/{id}`, etc.)
and a separate `PayPalService`. The `PaymentProvider.PAYPAL` and related code is untouched.

### Will existing 134 tests pass?

**YES** if `provider` field in `PaymentInitRequest` is optional (defaults to `FREEDOM_PAY`).
All existing FreedomPay and PayPal test requests that don't include `provider` will still work.

---

## No Race Conditions or Duplicate Confirmations

### Duplicate callback prevention

`ProcessedWebhookEvent` has a UNIQUE constraint on `(provider, event_id)`.
For VTB: `provider=VTB_KZ`, `event_id=mdOrder`.
- First callback arrives → processes → inserts `ProcessedWebhookEvent`
- Second callback (retry or duplicate) arrives → `existsByProviderAndEventId` returns true → skipped

### Race between callback and returnUrl handler

Both arrive "at the same time" in production (callback may arrive milliseconds before redirect).

**Strategy:**
1. Both check `ProcessedWebhookEvent` before inserting
2. The first to insert wins; the second sees the record and skips
3. If both execute `getOrderStatus` before either inserts: one will fail with UNIQUE constraint
4. Catch `DataIntegrityViolationException` in the `ProcessedWebhookEvent.save()` call
5. Treat as "already processed" — log INFO, return the current payment state

This is the same pattern as the FreedomPay `verifyFreedomPayRedirect` → `handleFreedomPayCallback`
chain already does (the second call to `handleFreedomPayCallback` is a no-op due to dedup check).

### Stuck orders in PENDING_PAYMENT

**Resolved by three mechanisms:**
1. Callback arrives → marks FAILED → `orderExpiryService.expire()` → order EXPIRED
2. ReturnUrl handler processes SUCCEEDED → order CONFIRMED
3. `OrderExpiryService` scheduled scan (every 5 min) → catches any missed orders

---

## Implementation Order (Revised)

```
Step 1.0   PaymentInitRequest + provider field
Step 1.0b  Fix initPayment() provider routing (must come before VTB branch)
Step 1.1   PaymentProvider.VTB_KZ
Step 1.2   VtbProperties
Step 1.3   VtbOrderStatus enum
Step 1.4   VTB DTOs (VtbRegisterRequest, VtbRegisterResponse, VtbOrderStatusResponse)
Step 1.5   application.properties + docker-compose.yml
Step 2.1   VtbCallbackVerifier + VtbChecksumResult
Step 3.1   VtbHttpClient (with stub mode)
Step 4.1   VtbCurrencyMapper
Step 4.2   Amount minor units utility
Step 5.1   PaymentServiceImpl.initVtbPayment()
Step 5.2   PaymentServiceImpl.handleVtbCallback()
Step 5.3   PaymentServiceImpl.verifyVtbReturn()
Step 5.4   PaymentServiceImpl.refundVtbPayment()
Step 6.1   VtbCallbackController (GET /callback/vtb-kz)
Step 6.2   SecurityConfig updates (GET callback + verify-return)
Step 6.3   VtbReturnController (POST /vtb-kz/verify-return)
Step 6.4   PaymentStubController — add VTB stub
Step 6.5   Flyway V32 (payment_provider enum)
Step 7     All tests (134 existing + 15-20 new VTB tests)
Step 8     Frontend PaymentReturnPage.tsx VTB orderId detection
```

---

## Pre-Implementation Checklist

- [x] All 7 documents audited
- [x] 13 issues found and documented
- [x] 2 CRITICAL issues → fixed in implementation plan
- [x] 8 HIGH issues → fixed in documents + plan
- [x] 3 MEDIUM issues → fixed in documents
- [x] Multi-currency design verified
- [x] Callback security design verified
- [x] FreedomPay/PayPal compatibility verified
- [x] Race conditions analyzed and mitigation documented
- [x] Order lifecycle (PENDING_PAYMENT → CONFIRMED/EXPIRED) verified
- [x] Amount validation (minor units) documented
- [x] `orderExpiryService.expire()` included in FAILED path
- [x] SecurityConfig GET callback rule documented

---

## VERDICT: READY FOR IMPLEMENTATION

All architectural issues have been documented and corrected in the design documents.
Implementation may proceed following the revised order above.

**Before writing first line of code — verify:**
1. `PaymentInitRequest.provider` field added (Step 1.0)
2. `PaymentServiceImpl.initPayment()` routing fixed (Step 1.0b)
3. These two changes are the prerequisite for the entire VTB branch to function correctly
