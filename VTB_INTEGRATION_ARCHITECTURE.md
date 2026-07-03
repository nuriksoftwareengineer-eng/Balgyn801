# VTB Kazakhstan — Integration Architecture

**Date:** 2026-06-25  
**Constraint:** No code. Architecture only.

---

## 1. Existing Architecture Overview

### Current Components (DO NOT BREAK)

```
com.nurba.java/
├── enums/
│   ├── PaymentProvider     (FREEDOM_PAY, PAYPAL)
│   └── PaymentStatus       (PENDING, SUCCEEDED, FAILED)
├── config/
│   └── FreedomPayProperties
├── payment/
│   ├── FreedomPayHttpClient
│   ├── FreedomPaySignature
│   ├── FreedomPayInitResult
│   ├── FreedomPayCheckResult
│   ├── PayPalOrdersClient
│   ├── PayPalTokenClient
│   ├── PayPalWebhookVerifier
│   └── dto/ (PayPal DTOs)
├── service/
│   ├── PaymentService (interface)
│   └── Impl/
│       └── PaymentServiceImpl
├── controller/
│   ├── PaymentController          → /api/v1/payments/init
│   ├── FreedomPayCheckController  → /api/v1/payments/freedom-pay/*
│   ├── PaymentStubController      → /api/v1/payments/stub/*
│   └── (PayPal webhook controller)
└── domain/
    ├── Payment
    ├── Order
    └── ProcessedWebhookEvent
```

### What Must NOT Change

- `PaymentService` interface (only extend with new methods if needed)
- `PaymentServiceImpl` FreedomPay and PayPal logic
- `ProcessedWebhookEvent` — reuse for VTB deduplication
- `Payment` and `Order` domain classes
- `SecurityConfig` permitAll rules for existing endpoints
- All existing tests (134 tests green)

---

## 2. New Components for VTB Kazakhstan

### 2.1 Component Map

```
com.nurba.java/
├── enums/
│   └── PaymentProvider     + VTB_KZ  ← ADD
├── config/
│   └── VtbProperties       ← NEW
├── payment/vtb/
│   ├── VtbHttpClient       ← NEW
│   ├── VtbCallbackVerifier ← NEW
│   ├── VtbCurrencyMapper   ← NEW
│   ├── VtbOrderStatus      ← NEW (enum)
│   └── dto/
│       ├── VtbRegisterRequest      ← NEW
│       ├── VtbRegisterResponse     ← NEW
│       └── VtbOrderStatusResponse  ← NEW
├── service/
│   └── Impl/
│       └── PaymentServiceImpl  + VTB_KZ branch  ← EXTEND
└── controller/
    └── VtbCallbackController  ← NEW
```

---

## 2.2 VtbProperties

**Purpose:** Typed Spring `@ConfigurationProperties` binding for all VTB Kazakhstan configuration.

**Prefix:** `app.payment.vtb`

**Fields:**

| Field | Type | Default | Source ENV |
|-------|------|---------|-----------|
| `apiUrl` | String | `https://payment.vtb.kz/payment/rest/` | `VTB_API_URL` |
| `userName` | String | `""` | `VTB_USERNAME` |
| `password` | String | `""` | `VTB_PASSWORD` |
| `hmacSecret` | String | `""` | `VTB_HMAC_SECRET` |
| `returnUrl` | String | `""` | `VTB_RETURN_URL` |
| `failUrl` | String | `""` | `VTB_FAIL_URL` |
| `callbackUrl` | String | `""` | `VTB_CALLBACK_URL` |
| `supportedCurrencies` | List\<Integer\> | `[398]` | `VTB_SUPPORTED_CURRENCIES` |
| `fallbackToKzt` | boolean | `false` | `VTB_FALLBACK_TO_KZT` |
| `sandboxMode` | boolean | `false` | `VTB_SANDBOX` |

**Computed:**

| Method | Returns | Logic |
|--------|---------|-------|
| `isStubMode()` | boolean | `userName.isBlank() \|\| password.isBlank()` |
| `getEffectiveApiUrl()` | String | If `sandboxMode` → `https://vtbkz.rbsuat.com/payment/rest/` else `apiUrl` |

---

## 2.3 VtbHttpClient

**Purpose:** Low-level HTTP client for VTB Kazakhstan REST API.

**Technology:** `java.net.http.HttpClient` (same as existing FreedomPayHttpClient)

**Responsibilities:**
1. Execute `register.do` — register order, return gateway UUID + formUrl
2. Execute `getOrderStatusExtended.do` — query payment status
3. Execute `refund.do` — initiate refund
4. Execute `reverse.do` — cancel pre-authorized payment
5. Execute `cancel.do` — cancel unregistered order

**Request format:** `application/x-www-form-urlencoded` (as documented by VTB)

**Authentication:** Always include `userName` + `password` in request body (or `token`).

**Error handling:**
- HTTP 4xx/5xx → throw `VtbApiException` with status + body
- `errorCode != 0` in response → throw `VtbApiException` with errorCode + errorMessage
- Network timeout → throw `VtbApiException("VTB API timeout")`

**Stub mode behavior (when `isStubMode()` == true):**
- `register.do` → return fake formUrl pointing to `/api/v1/payments/stub/vtb/{orderId}`
- All other operations → return success responses without real API call
- Same pattern as `PaymentStubController` for FreedomPay

---

## 2.4 VtbCallbackVerifier

**Purpose:** Optional first-line checksum verification. The API call is always the primary security check.

---

### Background: Checksum vs API verification

**Finding from official plugin source code (both WordPress v5.6.0 and Magento v2.3.x):**

| Plugin | Checksum handling |
|--------|-------------------|
| WordPress | Completely ignored — `checksum` param not read at all |
| Magento | `isPaymentValid()` method exists as a placeholder but body is `return true;` |

Both proceed directly to `getOrderStatusExtended.do` after receiving a callback.

**Conclusion from three sources:**
1. The gateway *generates* a checksum (the portal has a "Generate token" button)
2. The Magento method stub says BPC *designed* the verification hook
3. But neither official plugin *implements* it
4. `getOrderStatusExtended.do` with merchant credentials is the definitive security check

**Documentation:** The full callback algorithm text is unreachable via WebFetch (SPA/JS-rendered).
The section "Алгоритм обработки уведомлений" exists in the TOC but content isn't extractable.

---

### Architecture: Two-layer security

```
CALLBACK RECEIVED
        │
        ▼
┌───────────────────────────────────────┐
│  LAYER 1: Checksum (OPTIONAL)         │
│  if callbackSecret is configured:     │
│    compute SHA256(sorted;secret)      │
│    if mismatch → log WARN             │
│    DO NOT reject — proceed to layer 2 │
│  if callbackSecret is blank:          │
│    skip entirely                      │
└───────────────────────────────────────┘
        │  (always proceed)
        ▼
┌───────────────────────────────────────┐
│  LAYER 2: API Verification (REQUIRED) │
│  Extract mdOrder from callback params │
│  Call getOrderStatusExtended.do       │
│  with merchant credentials            │
│  → Trust ONLY this response           │
│  → Map orderStatus → PaymentStatus    │
└───────────────────────────────────────┘
        │
        ▼
   Update Payment + Order in DB
```

**Why this is correct:**
- An attacker cannot fake `getOrderStatusExtended.do` — it requires valid merchant credentials
- The checksum adds defense-in-depth but is not the gate
- If checksum algorithm turns out wrong (format unclear until sandbox test), behavior is unchanged
- Can promote checksum to hard-required gate later without architecture changes

---

### VtbCallbackVerifier Interface

```java
/**
 * Verifies VTB callback checksum as optional defense-in-depth.
 * Returns ChecksumResult, not boolean — caller decides how to handle.
 */
VtbChecksumResult verifyChecksum(Map<String, String> params, String callbackSecret);

enum VtbChecksumResult {
    VALID,       // checksum present and matches
    INVALID,     // checksum present but mismatch — log WARN
    SKIPPED,     // callbackSecret is blank — skip (stub/dev mode)
    ABSENT       // checksum param missing from callback — log INFO
}
```

The verifier returns a result enum, not a boolean. The service layer logs the result and
proceeds to API verification regardless.

---

### Checksum Algorithm (best-effort from available sources)

**Source:** VTB Kazakhstan sandbox docs (indirect extraction) + FreedomPay analogy (same RBS platform family)

```
Formula (from sandbox.vtb-bank.kz, exact format TBD):
  SHA256(param1 ; param2 ; ... ; paramN ; callbackSecret)

Step 1: Extract "checksum" value → receivedChecksum
Step 2: Remove "checksum" from params map
Step 3: Sort remaining params alphabetically by KEY
Step 4: Build string — ⚠️ FORMAT UNCONFIRMED:
        Option A (values only, semicolons):  "val1;val2;val3;callbackSecret"
        Option B (key=value, semicolons):    "k1=v1;k2=v2;k3=v3;callbackSecret"
Step 5: Compute SHA256(string, UTF-8)
Step 6: Uppercase hex digest
Step 7: Compare case-insensitively with receivedChecksum
```

**Both options must be tried if checksum fails in sandbox.** Implement as:
```java
// Try Option A first (values only — consistent with FreedomPay pattern)
// If INVALID, log both computed values for sandbox debugging
```

**After getting sandbox credentials:** send one real payment, capture the callback,
log all params + both computed checksums → determine correct format empirically.

---

### Callback secret management

- TEST portal: `https://vtbkz.rbsuat.com/mportal3`
- PROD portal: `https://payment.vtb.kz/generalmp3/auth/login`
- Path: Settings → Merchant → Callback Notifications → Generate token
- Type: Symmetric (SHA256 token) — default; Asymmetric available via VTB KZ support
- ENV var: `VTB_HMAC_SECRET` (name preserved from earlier plans for compatibility)

---

## 2.5 VtbOrderStatus (enum)

**Purpose:** Maps VTB `orderStatus` integer values to meaningful Java constants.

| VTB Value | Constant | Description |
|-----------|----------|-------------|
| 0 | `REGISTERED` | Order created, awaiting payment |
| 1 | `PRE_AUTHORIZED` | Hold placed (two-phase) |
| 2 | `DEPOSITED` | Payment complete |
| 3 | `DECLINED` | Declined |
| 4 | `REVERSED` | Cancelled/reversed |
| 5 | `REFUNDED` | Refunded |
| -1 | `UNKNOWN` | Unknown/unrecognized value |

**Mapping to project's `PaymentStatus`:**

| VtbOrderStatus | PaymentStatus |
|----------------|---------------|
| DEPOSITED | SUCCEEDED |
| DECLINED | FAILED |
| REVERSED | FAILED |
| REFUNDED | FAILED (or new REFUNDED status) |
| REGISTERED, PRE_AUTHORIZED | PENDING |

---

## 2.6 VtbCurrencyMapper

**Purpose:** Maps order currency string to VTB ISO 4217 numeric code and checks merchant support.

**Fields:**

| Currency String | ISO Numeric |
|-----------------|-------------|
| "KZT" | 398 |
| "RUB" | 643 |
| "USD" | 840 |
| "EUR" | 978 |

**Logic:**
1. Look up ISO numeric for `order.currency`
2. Check if numeric is in `vtbProperties.supportedCurrencies`
3. If yes → return that currency
4. If no and `fallbackToKzt=true` → return 398 (KZT), log WARN
5. If no and `fallbackToKzt=false` → throw `BusinessRuleException`

---

## 2.7 VTB DTOs

### VtbRegisterRequest (sent to register.do)

| Field | Type | Note |
|-------|------|------|
| `orderNumber` | String | Merchant order ID |
| `amount` | Long | `totalPrice × 100` in minor units |
| `currency` | int | ISO 4217 numeric code (e.g., 398) |
| `returnUrl` | String | Success redirect |
| `failUrl` | String | Failure redirect |
| `description` | String | "Order #11" |
| `language` | String | "ru" or "en" |
| `email` | String | Customer email (if available) |
| `dynamicCallbackUrl` | String | Callback URL from properties |
| `userName` | String | From VtbProperties |
| `password` | String | From VtbProperties |

### VtbRegisterResponse

| Field | Type | Note |
|-------|------|------|
| `orderId` | String | Gateway UUID (= `mdOrder` in callbacks) |
| `formUrl` | String | Redirect URL for customer |
| `errorCode` | int | 0 = success |
| `errorMessage` | String | Error description (if errorCode != 0) |

### VtbOrderStatusResponse

| Field | Type | Note |
|-------|------|------|
| `orderNumber` | String | Merchant's order number |
| `orderStatus` | int | 0–5 |
| `amount` | Long | Amount in minor units |
| `currency` | String | ISO numeric code |
| `authCode` | String | Bank auth code |
| `ip` | String | Cardholder IP |
| `errorCode` | int | 0 = success |

---

## 2.8 VtbCallbackController

**Endpoint:** `GET /api/v1/payments/callback/vtb-kz`

> ⚠️ **GET, not POST.** Confirmed from official WooCommerce plugin source:
> `$data['callback_http_method'] = "GET";`
> VTB sends callbacks as GET requests with parameters in the URL query string.

**Permit:** `permitAll()` (same as FreedomPay callback)

**Responsibilities:**

1. Accept all GET query params as `Map<String, String>` (`@RequestParam`)
2. Log all params at `INFO` level (mask any sensitive values)
3. Call `paymentService.handleVtbCallback(params)`
4. Return HTTP 200 OK (no body) — **always**, even on checksum mismatch or error
   (returning non-200 causes VTB to retry at 10*N minute intervals)

**Rate limiting:** Apply same rate limiting as other callback endpoints.

**Checksum behavior:**
- If `VTB_HMAC_SECRET` is configured: verify checksum → log result → proceed to API check
- If `VTB_HMAC_SECRET` is blank: skip checksum → proceed to API check
- Checksum mismatch does NOT stop processing — API verification is always performed
- Only hard error (e.g., `mdOrder` missing) causes a no-op with 200 response

**Callback parameters arriving (confirmed from plugin source):**
```
GET /api/v1/payments/callback/vtb-kz
  ?mdOrder={vtb-gateway-uuid}
  &orderNumber={merchant-order-number}
  &operation={deposited|approved|declinedByTimeout|reversed|refunded}
  &status={1|0}
  &checksum={sha256-hex}
  [&amount={minor-units}]  ← may be absent
```

---

## 2.9 PaymentServiceImpl — VTB_KZ branch

**Extends** existing `PaymentServiceImpl`. Add new methods WITHOUT touching FreedomPay or PayPal branches.

### New method: `initVtbPayment(Order order, PaymentInitRequest request)`

Called from `initPayment()` when `request.provider == VTB_KZ`.

Steps:
1. Idempotency: check for existing `PENDING` payment for same order + VTB_KZ
2. Map currency via `VtbCurrencyMapper`
3. Compute amount in minor units
4. Call `vtbHttpClient.register(...)` → get `VtbRegisterResponse`
5. Create `Payment` entity: status=PENDING, providerPaymentId=vtbResponse.orderId
6. Save and return `PaymentResponse`

### New method: `handleVtbCallback(Map<String, String> params)`

Called from `VtbCallbackController`.

Steps:
1. Extract `mdOrder` from params; throw if missing (return 200 with no-op)
2. Check `ProcessedWebhookEvent` deduplication by `(VTB_KZ, mdOrder)` → return if already processed
3. [OPTIONAL] Call `vtbCallbackVerifier.verifyChecksum(params, vtbProps.hmacSecret)`
   - `SKIPPED` (blank secret) → log INFO, continue
   - `ABSENT` (no checksum in params) → log INFO, continue
   - `VALID` → log INFO, continue
   - `INVALID` → log WARN, **continue** (checksum failure does NOT block processing)
4. Call `vtbHttpClient.getOrderStatus(mdOrder)` → `VtbOrderStatusResponse`
5. Validate: `response.amount` (minor units ×100) should match `payment.amount * 100`; log WARN on mismatch
6. Map `VtbOrderStatus` → `PaymentStatus`
   - DEPOSITED (2) → SUCCEEDED
   - PRE_AUTHORIZED (1) → PENDING (one-phase: not expected)
   - DECLINED (3), REVERSED (4), REFUNDED (5) → FAILED
7. Load `Payment` by `payment.providerPaymentId == mdOrder`
8. Update `payment.status`, `payment.lastWebhookPayload`, `payment.updatedAt`
9. If SUCCEEDED and order status is PENDING_PAYMENT or NEW:
   - `order.status = CONFIRMED`
   - `order.updatedAt = now()`
   - save order
10. If FAILED and order status is PENDING_PAYMENT:
    - call `orderExpiryService.expire(order)` (releases inventory, marks EXPIRED)
11. Save payment
12. Save `ProcessedWebhookEvent(provider=VTB_KZ, eventId=mdOrder, payment=saved)`

### New method: `refundVtbPayment(Long paymentId, BigDecimal amount)`

Steps:
1. Load Payment, verify it's SUCCEEDED and provider=VTB_KZ
2. Call `vtbHttpClient.refund(payment.providerPaymentId, amountInMinorUnits)`
3. Update payment.status=REFUNDED (or create new REFUNDED payment record)

---

## 3. Integration Points

### 3.1 PaymentController (existing, minimal changes)

`POST /api/v1/payments/init` currently hardcodes FreedomPay in idempotency check. Needs to route by provider:

```
if request.provider == FREEDOM_PAY → existing flow
if request.provider == PAYPAL → existing flow  
if request.provider == VTB_KZ → new vtb flow
```

### 3.2 PaymentProvider enum

Add `VTB_KZ` to existing enum. No other changes to the enum.

### 3.3 SecurityConfig

Add one new `permitAll()` rule:
```java
// VTB callback is GET (confirmed from official plugin)
.requestMatchers(HttpMethod.GET, "/api/v1/payments/callback/vtb-kz").permitAll()
// Stub endpoint (already covered by existing GET /stub/** rule):
// .requestMatchers(HttpMethod.GET, "/api/v1/payments/stub/vtb/**").permitAll()
```

> **Note:** The existing `SecurityConfig` already has:
> ```java
> .requestMatchers(HttpMethod.POST, "/api/v1/payments/callback/**").permitAll()
> .requestMatchers(HttpMethod.GET, "/api/v1/payments/stub/**").permitAll()
> ```
> The stub endpoint is already covered. Only need to add the VTB GET callback rule.
> The existing POST wildcard does NOT cover VTB callback (different HTTP method).

### 3.4 application.properties

```properties
# VTB Kazakhstan Internet Acquiring
app.payment.vtb.api-url=${VTB_API_URL:https://payment.vtb.kz/payment/rest/}
app.payment.vtb.user-name=${VTB_USERNAME:}
app.payment.vtb.password=${VTB_PASSWORD:}
app.payment.vtb.hmac-secret=${VTB_HMAC_SECRET:}
app.payment.vtb.return-url=${VTB_RETURN_URL:${app.frontend.base-url}/payment-return}
app.payment.vtb.fail-url=${VTB_FAIL_URL:${app.frontend.base-url}/payment/failed}
app.payment.vtb.callback-url=${VTB_CALLBACK_URL:}
app.payment.vtb.supported-currencies=${VTB_SUPPORTED_CURRENCIES:398}
app.payment.vtb.fallback-to-kzt=${VTB_FALLBACK_TO_KZT:false}
app.payment.vtb.sandbox-mode=${VTB_SANDBOX:false}
```

---

## 4. Comparison: FreedomPay vs PayPal vs VTB KZ

| Aspect | FreedomPay | PayPal | VTB Kazakhstan |
|--------|------------|--------|----------------|
| API style | XML POST form | REST JSON | Form-encoded POST |
| Auth | MD5 signature per request | OAuth2 Bearer | username+password in body |
| Callback auth | MD5 signature | RSA webhook | SHA256(values;secret) |
| Payment URL | From API response | From API response | From register.do formUrl |
| Status check | check_payment.php (blocked) / redirect sig | Capture response | getOrderStatusExtended.do |
| Refund | Not implemented yet | REST API | refund.do |
| Cancel | N/A | REST API | reverse.do / cancel.do |
| Currencies | KZT | KZT→USD converted | Native per order |
| Cards | Visa/MC/Mir | Visa/MC/AmEx | Visa/MC/Mir/UnionPay |
| Stub mode | Empty merchantId | Empty clientId | Empty userName |

---

## 5. What Can Be Reused

| Existing Component | Reuse In VTB |
|-------------------|--------------|
| `ProcessedWebhookEvent` | ✅ Deduplication by `mdOrder` |
| `Payment` entity | ✅ Add `VTB_KZ` to PaymentProvider enum |
| `PaymentStatus` enum | ✅ PENDING/SUCCEEDED/FAILED map correctly |
| `OrderExpiryService` | ✅ Works on any order regardless of provider |
| `PaymentResponse` DTO | ✅ Return same response shape to frontend |
| `PaymentInitRequest` | ✅ Add provider=VTB_KZ |
| `FreedomPaySignature` pattern | ✅ Same idea → new `VtbCallbackVerifier` |
| `FreedomPayProperties` pattern | ✅ Same pattern → new `VtbProperties` |
| `HttpClient` instance | ✅ Reuse java.net.http.HttpClient |
| `BusinessRuleException` | ✅ Throw for auth/validation failures |
| `NotFoundException` | ✅ Throw when order not found |

---

## 6. What Is New (No Reuse Possible)

| Component | Why New |
|-----------|---------|
| `VtbHttpClient` | Different API format, different auth, new endpoints |
| `VtbCallbackVerifier` | HMAC-SHA256 vs MD5 — different algorithm |
| `VtbCurrencyMapper` | New concept — ISO numeric currency codes |
| `VtbOrderStatus` | VTB-specific status codes (0–5) |
| `VtbCallbackController` | New endpoint `/api/v1/payments/callback/vtb-kz` |
| `VtbProperties` | Different config structure |
| VTB DTOs | VTB-specific request/response fields |

---

## 7. Frontend Changes (Minor)

The frontend already handles the `paymentUrl` redirect pattern (from FreedomPay and PayPal). For VTB Kazakhstan:

1. Add `VTB_KZ` as a payment provider option in checkout
2. The flow uses the same redirect pattern: `formUrl` → VTB HPP → `/payment-return?...`
3. `PaymentReturnPage.tsx` needs to detect VTB KZ redirect parameters

**VTB KZ redirect parameters** (on success return to `returnUrl`):
- `orderId` — VTB gateway UUID
- The exact parameters in the return URL are NOT fully documented — must be verified via sandbox test

**Recommended approach:** After VTB redirects back, call `getOrderStatusExtended.do` via backend (same pattern as verify-redirect for FreedomPay), since the return URL parameters from VTB are not documented to include a signature.

---

## 8. Security Considerations

1. **VTB_HMAC_SECRET** must never be logged or exposed in API responses
2. **VTB_USERNAME / VTB_PASSWORD** must never be logged
3. **Checksum verification is optional** (confirmed from official plugins — neither implements it).
   If `VTB_HMAC_SECRET` is configured → attempt SHA256 verification, log result, ALWAYS proceed.
   Checksum INVALID does NOT block order confirmation.
4. **Always call `getOrderStatusExtended.do`** after callback — this is the ONLY source of truth
5. **Deduplicate** callbacks using `ProcessedWebhookEvent` by `(VTB_KZ, mdOrder)`
6. **Validate amount** from `getOrderStatusExtended` response (minor units ×100 must equal payment.amount)
7. **Rate limit** callback endpoint (already done via SecurityRateLimitProperties)
8. **VTB callback URL** must be HTTPS with valid certificate (production requirement)
9. **Race condition prevention**: Both the callback handler and the returnUrl handler call
   `getOrderStatusExtended` and may process the same payment. Use `ProcessedWebhookEvent` in the
   callback handler as the idempotency gate. The returnUrl handler should check the payment's
   current status first and skip if already SUCCEEDED (same pattern as FreedomPay `verifyRedirect`).
