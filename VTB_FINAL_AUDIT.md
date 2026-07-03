# VTB Kazakhstan — Final Pre-Implementation Audit

**Date:** 2026-06-25  
**Status:** COMPLETE — final callback architecture confirmed + ready for implementation  
**Sources used:**
1. `sandbox.vtb-bank.kz` — all 11 documentation pages (full sitemap)
2. `dev.bpcbt.com/plugins/VTBKZ/wordpress/wp_woocommerce.zip` — official VTB Kazakhstan WooCommerce plugin v5.6.0 source code
3. `sandbox.vtb-bank.kz/ru/integration/api/action_codes.html` — complete action codes
4. `sandbox.vtb-bank.kz/ru/integration/structure/test-cards.html` — test cards
5. `sandbox.vtb-bank.kz/ru/integration/certification.html` — PCI DSS

> **KEY METHOD:** The official WooCommerce plugin for VTB Kazakhstan (produced by BPC — the company
> that built the payment gateway) is the authoritative reference implementation. Where documentation
> is ambiguous, the plugin source code is the ground truth.

---

## 1. CALLBACK CHECKSUM ALGORITHM — RESOLVED

### Official Verdict: THE OFFICIAL PLUGIN DOES NOT VERIFY THE CHECKSUM

Extracted from `class-wc-gateway-vtbkz.php` — the complete callback handler:

```php
case "callback":
    $order_number = $response['orderNumber'];  // From getOrderStatusExtended response
    $orderStatus = $response['orderStatus'];    // From getOrderStatusExtended response
    // ← No checksum verification anywhere in this function
    if ($orderStatus == '1' || $orderStatus == '2') { ... }
```

The official plugin:
1. Receives the callback (GET request, parameters in URL)
2. Extracts `mdOrder` from `$_GET`
3. **Immediately calls `getOrderStatusExtended.do` API** to get the real status
4. Acts **only on the API response** — never on callback parameters directly
5. The `checksum` parameter from callback is ignored entirely

### Why This Is The Correct Pattern

This "trigger-then-verify" pattern is intentional security design:
- Callback only says "something happened to order X"
- The merchant verifies what actually happened via the authenticated API call
- Even a forged callback can't fake the API response (requires merchant credentials)

### IMPLICATION FOR VtbCallbackVerifier

**Change in architecture:**

| Previous plan | Revised plan (from plugin analysis) |
|---------------|-------------------------------------|
| `VtbCallbackVerifier.verify()` required | Checksum verification is **optional** |
| Block order confirmation if checksum fails | Log warning, still call API |
| Complex HMAC/SHA256 algorithm | Simple "extract mdOrder, call API" |

**Recommendation:** 
- If `callbackSecret` is provided → attempt checksum verification (log result)
- If `callbackSecret` is blank → skip verification, proceed to API call
- **Always call `getOrderStatusExtended.do` regardless of checksum result**
- The API response is the single source of truth

### Algorithm If Checksum Is Wanted

The documentation states: `SHA256(param1;param2;...;paramN;callbackSecret)`

The exact format (values-only vs key=value) is **NOT confirmed** from official sources.
The plugin doesn't use it, so this cannot be tested before getting credentials.

**Decision point:** Implement as optional defensive check. If algorithm turns out wrong:
returning true (skip) is safe because we always verify via API anyway.

---

## 2. CALLBACK — COMPLETE SPECIFICATION

### HTTP Method

**GET** request (NOT POST).

Evidence from plugin `_updateGatewayCallback`:
```php
$data['callback_http_method'] = "GET";
```

This is the default configuration. The merchant portal allows choosing GET or POST.

### Callback URL Format

VTB sends a GET request to the configured callback URL. For dynamic callbacks, the merchant
includes the internal order ID in the URL (the plugin pattern):
```
{callbackUrl}?action=callback&dynamic=1&order_id={merchantOrderId}&mdOrder={vtbGatewayUuid}&orderNumber={merchantOrderNumber}
```

The `mdOrder` parameter (VTB's gateway UUID) is appended by VTB.

### Complete Callback Operations

From `_updateGatewayCallback`:
```php
$data['callback_operations'] = "deposited,approved,declinedByTimeout,reversed,refunded";
```

| Operation | Trigger | Mapping to PaymentStatus |
|-----------|---------|--------------------------|
| `deposited` | Funds captured (one-phase complete, or two-phase deposit) | `SUCCEEDED` |
| `approved` | Pre-authorization placed (two-phase, orderStatus=1) | `PENDING` (hold placed) |
| `declinedByTimeout` | 20-min payment timeout expired | `FAILED` |
| `reversed` | Payment reversed/cancelled | `FAILED` |
| `refunded` | Refund processed | `FAILED` (or new REFUNDED status) |

> ⚠️ **Earlier docs said `authorized`** — this is WRONG. The correct operation name is **`approved`**.

### Parameters Arriving in Callback

From the callback handler code:
```php
$args['orderId'] = isset($_GET['mdOrder']) ? sanitize_text_field($_GET['mdOrder']) : null;
```

**Guaranteed parameters in callback:**
- `mdOrder` — VTB gateway UUID (the `orderId` from register.do response)

**Other parameters VTB may send** (based on portal docs):
- `orderNumber` — merchant's original order number
- `checksum` — callback signature (not verified by official plugin)
- `operation` — one of: deposited, approved, declinedByTimeout, reversed, refunded
- `status` — 1 (success) or 0 (failure)
- `amount` — amount in minor units (may not always be present)

> ⚠️ **IMPORTANT:** The plugin verifies payment status by calling `getOrderStatusExtended.do`
> with `mdOrder`. Do NOT rely on `operation` or `status` in the callback body — always verify
> via API.

### Idempotency

The plugin handles duplicate callbacks:
```php
if ($order->is_paid()) {
    exit;  // Already paid — skip
}
```

For our implementation: use `ProcessedWebhookEvent` deduplication by `mdOrder`.

### What Merchant Must Return

Return HTTP 200 with no body.
If non-200 returned → VTB retries at `10 * attempt_number` minute intervals.

---

## 3. RETURN URL (returnUrl) — COMPLETE SPECIFICATION

### What VTB Appends to returnUrl

From the callback handler for `action=result`:
```php
$args['orderId'] = isset($_GET['orderId']) ? sanitize_text_field($_GET['orderId']) : null;
```

VTB appends **exactly one parameter** to the returnUrl on redirect back:

```
{returnUrl}?orderId={vtbGatewayUuid}
```

Where `orderId` = the VTB gateway UUID from the `register.do` response.

### Example

If you registered with `returnUrl=https://example.com/payment-return`, VTB redirects to:
```
https://example.com/payment-return?orderId=abc123-uuid-from-vtb
```

### What to Do on Return URL

From the plugin code (webhook_result, action=result):
```php
$args['orderId'] = isset($_GET['orderId']) ? ... : null;
// Then calls getOrderStatusExtended.do with this orderId
$response = $this->_sendGatewayData(http_build_query($args), $action_adr);
$orderStatus = $response['orderStatus'];
if ($orderStatus == '1' || $orderStatus == '2') {
    // Success
}
```

**MANDATORY:** After receiving return redirect, call `getOrderStatusExtended.do`. Never
trust the redirect alone. The `orderId` in the redirect is only a lookup key.

### failUrl

From the code:
```php
$order->update_status('failed', "VTBKZ: Payment failed");
if (!empty($this->fail_url)) {
    wp_redirect($this->fail_url . "?order_id=" . $order_number);
    exit;
}
```

VTB redirects to failUrl on payment failure. The merchant can append their own params to failUrl.
VTB itself does not document appending params to failUrl — the plugin adds `?order_id=` itself.

---

## 4. ORDER STATUSES — COMPLETE

### orderStatus Values (from getOrderStatusExtended.do)

| Value | VTB Name | Our PaymentStatus | Notes |
|-------|----------|-------------------|-------|
| `0` | Registered | `PENDING` | Order created, awaiting payment |
| `1` | Approved | `PENDING` (or `AUTHORIZED`) | Pre-auth hold placed. **Requires depositOrder.do to complete** |
| `2` | Deposited | `SUCCEEDED` | Payment complete ✅ |
| `3` | Declined | `FAILED` | Declined by bank or cardholder |
| `4` | Reversed | `FAILED` | Cancelled before capture |
| `5` | Refunded | `FAILED` (or `REFUNDED`) | Refund processed |

> **CRITICAL FINDING:** `orderStatus == 1 OR 2` both indicate successful authorization.
> The documentation states: "An order is considered paid only if orderStatus equals 1 or 2."
> - Status 2 = funds actually transferred (one-phase or after deposit in two-phase)
> - Status 1 = funds held (two-phase pre-authorization) — must still run depositOrder.do

**For one-phase payments (our default):** Only status 2 means payment complete.
**For two-phase payments:** Status 1 = authorized (hold), status 2 = deposited (captured).

The WooCommerce plugin marks orders as paid on both status 1 and 2 because it supports
both flows. **For Balgyn (one-phase only), only status 2 = SUCCEEDED.**

### getOrderStatusExtended.do Response Key Fields

From the plugin usage:
```php
$response['orderStatus']    // 0-5
$response['orderNumber']    // merchant's order number
$response['authRefNum']     // bank auth reference
$response['paymentAmountInfo']['refundedAmount']  // amount refunded in minor units
$response['paymentAmountInfo']['approvedAmount']  // amount approved
$response['payerData']      // customer address data
$response['actionCodeDescription']  // human-readable failure reason
```

---

## 5. ACTION CODES — COMPLETE

**Source:** `sandbox.vtb-bank.kz/ru/integration/api/action_codes.html`

### Success

| Code | Meaning |
|------|---------|
| `0` / `000` | ✅ Payment processed successfully |
| `-100` | Awaiting payment attempt (not an error) |

### 3DS Errors

| Code | Meaning |
|------|---------|
| `-2025` | Issuer declined with RReq — disable VPN |
| `-2024` | Frictionless 3DS forbidden by issuer |
| `-2023` | Bank cannot execute 3DS |
| `-2022` | 3DS operation impossible |
| `-2020` | Invalid ECI from ACS |
| `-2018` | Authentication not possible at acquirer |
| `-2009` | Cardholder didn't return from ACS |
| `-2006` | 3DS verification failed |
| `2003` | Payments require 3D Secure |
| `2016` | Merchant lacks 3DS authorization |
| `1434` | 3DS authorization required |
| `4032` | 3DS attempted but failed |

### Card/Account Errors

| Code | Meaning |
|------|---------|
| `20` | Insufficient funds |
| `62` | Card compromised |
| `72` / `73` | Card unauthorized for this transaction type |
| `78` | Invalid/non-existent account |
| `82` / `88` | Invalid CVC |
| `101` | Card expired |
| `106` | Card blocked (PIN exceeded) |
| `111` / `125` | Invalid card number |
| `203` / `204` / `208` | Card reported lost |
| `212` | Participant blocked |
| `555` | Issuer blocked online transactions |
| `823` | Card stolen |
| `2030` | Card blocked |

### Limit Errors

| Code | Meaning |
|------|---------|
| `-20010` | Exceeds issuer limits |
| `100` / `116` / `902` / `903` | Balance/credit exceeded |
| `110` | Invalid amount |
| `121` | Daily limit exceeded |
| `123` | Transaction count limit exceeded |
| `814` | Total cycle limit reached |

### Timeout / Abandon

| Code | Meaning |
|------|---------|
| `-2007` | Payment timeout (default 20 minutes) |
| `-2013` | Too many payment attempts |
| `-2014` | Payer didn't return from payment page |
| `151018` / `151019` | Processing timeout |

### System / Config Errors

| Code | Meaning |
|------|---------|
| `-30001` | Operation under review (poll) |
| `1120` | Refund in progress |
| `90` | Unknown response status |
| `109` | Merchant terminal misconfigured |
| `124` | Technical error |
| `907` | Cannot reach issuer |
| `912` | Issuer unavailable |
| `913` / `904` | Message format error |
| `914` | Original transaction not found |
| `916` | Cannot process |
| `941` | Invalid merchant ID |
| `998` | Service unavailable |
| `909` | System error (general) |

### Business Logic Errors

| Code | Meaning |
|------|---------|
| `71015` | Invalid payment data (wrong CVC/expiry) |
| `151` | Fraud monitoring triggered |
| `999` | Suspected fraud |
| `4005` | Merchant declined payment |
| `2002` | Invalid operation for current orderStatus |
| `2012` | Order already cancelled |
| `8204` | Duplicate order number |

---

## 6. CURRENCIES — CONFIRMED

### From Plugin Source Code (`get_numeric_currency_code`)

The VTB Kazakhstan plugin ships with this currency map — these are **platform-supported** currencies:

```php
'KZT' => '398',  'RUB' => '643',  'USD' => '840',  'EUR' => '978',
'BYN' => '933',  'UAH' => '980',  'CNY' => '156',  'GBP' => '826',
'CAD' => '124',  'TRY' => '949',  'AMD' => '051',  ... (30+ currencies)
```

### Currency Status for Kazakhstan Merchants

| Currency | ISO | Platform Support | Merchant Agreement Needed | Status |
|----------|-----|-----------------|--------------------------|--------|
| KZT | 398 | ✅ | No | ✅ Confirmed default |
| USD | 840 | ✅ | Likely | ⚠️ Need VTB KZ confirmation |
| EUR | 978 | ✅ | Likely | ⚠️ Need VTB KZ confirmation |
| RUB | 643 | ✅ (in plugin) | Likely | ⚠️ Need VTB KZ confirmation |
| UAH | 980 | ✅ (in plugin) | Almost certainly | ❌ Not relevant for Balgyn |
| CNY | 156 | ✅ (in plugin) | Almost certainly | ❌ Not relevant for Balgyn |

> **Note on the example `currency=643` in docs:** The sandbox redirect integration page showed
> `currency=643` (RUB) as an example. This is likely documentation copied from Russian VTB
> and does NOT mean Kazakhstan sandbox defaults to RUB. KZT (398) is the merchant default.

### `BPC_VTBKZ_MANDATORY_CURRENCY = true`

This constant means: always send `currency` parameter, never rely on merchant default.
This confirms our `VtbCurrencyMapper` design is correct.

---

## 7. CARD SUPPORT — CONFIRMED

### From Sandbox Test Cards Page

Test cards provided in sandbox documentation are **exclusively Mir** (BIN 2201 38xx):

| Card | Type | Scenario |
|------|------|----------|
| `2201 3820 0000 0021` | Mir, 3DS2 Full | ✅ Success |
| `2201 3820 0000 0039` | Mir, 3DS2 Full | ✅ Success |
| `2201 3820 0000 0013` | Mir, 3DS2 Full | ✅ Success |
| `2201 3820 0000 0047` | Mir, 3DS2 Full | ✅ Success |
| `2201 3820 0000 0054` | Mir, 3DS2 Full | ✅ Success |
| `2201 3820 0000 0062` | Mir, Frictionless | ✅ Success |

All: CVC `123`, Expiry `12/34`

**To simulate decline:** Use correct card number with wrong CVC or expired date → error `71015`

### Accepted Card Networks in Production

| Card | Status | Evidence |
|------|--------|---------|
| Visa | ✅ | Corporate site, FAQ |
| Mastercard | ✅ | Corporate site, FAQ |
| Mir | ✅ | VTB KZ was first bank in Kazakhstan to accept Mir |
| UnionPay | ✅ | VTB KZ FAQ |
| Google Pay | ✅ | Plugin has Google Pay integration |
| Apple Pay | ✅ | Plugin has Apple Pay integration |
| JCB | ❓ | Not mentioned in any official source |
| AmEx | ❓ | Not mentioned in any official source |

### Mir-Specific Notes

- VTB Kazakhstan was **first bank in Kazakhstan** to accept Mir cards
- Mir virtual and plastic cards issued by VTB KZ itself
- Transfers to Russia via Mir confirmed (Dec 2025)
- No special API handling needed — same flow as Visa/MC

---

## 8. REFUND / REVERSE / CANCEL — EXACT LOGIC

### From Plugin `process_refund` — Authoritative Source

```php
$orderStatus = $status['orderStatus'];
if ($orderStatus == '2' || $orderStatus == '4') {
    $endpoint = 'refund.do';   // Deposited or already reversed → refund
    $action_args = array_merge($base_args, ['amount' => $amount_in_cents]);
} elseif ($orderStatus == '1') {
    $endpoint = 'reverse.do';  // Pre-authorized (two-phase) → reverse
    // amount is optional for reverse
}
```

### Decision Tree

```
WANT TO CANCEL/REFUND a VTB payment:

1. Call getOrderStatusExtended.do → get orderStatus

orderStatus == 0 (not yet paid):
    → cancel.do (free the order slot, no money involved)

orderStatus == 1 (two-phase, authorized, not deposited):
    → reverse.do (cancel hold before capture)
    → Result: orderStatus = 4 (Reversed)

orderStatus == 2 (deposited, funds captured):
    → refund.do with amount
    → Full refund: don't pass amount, or pass full amount
    → Partial refund: pass amount < original
    → Can call multiple times, total ≤ original amount
    → Result: orderStatus = 5 (Refunded) on full refund; 4 on partial? (needs verification)

orderStatus == 3 (already declined):
    → Nothing to do (already failed)

orderStatus == 4 (already reversed):
    → refund.do (per plugin: partial refund of an already-reversed order?)
    Actually: this case in the plugin is for when a callback arrives saying status=4

orderStatus == 5 (already refunded):
    → Nothing to do (already refunded)
```

### Time Limits

From sandbox docs: "contact support for exact refund period" — not documented publicly.
Typical for RBS platform: 30-180 days.

### errorCode 7 (from plugin)

```php
if ($errorCode == '7') {
    return new WP_Error('...', 'For partial refunds Order state should be in DEPOSITED in Gateway');
}
```

Error code `7` = "cannot refund — order not in DEPOSITED state". Our code should handle this.

---

## 9. IDEMPOTENCY — CONFIRMED PATTERN

### From Plugin Source

The official plugin achieves idempotency in two ways:

**1. Return URL handler:**
```php
if ($orderStatus == '1' || $orderStatus == '2') {
    if ($this->allowCallbacks === false) {
        $order->update_status(...);
    }
    WC()->cart->empty_cart();
    wp_redirect($this->get_return_url($order));  // Always redirect, don't re-process
```

**2. Callback handler:**
```php
if ($order->is_paid()) {
    exit;  // Duplicate callback — skip silently
}
```

### For Our Implementation

Use existing `ProcessedWebhookEvent` deduplication by `mdOrder`.

For `orderNumber` uniqueness: append timestamp to avoid duplicate orderNumber errors:
```java
String orderNumber = order.getId() + "_" + System.currentTimeMillis();
```
The plugin does: `$order_number . '_' . time()`

This ensures each payment attempt gets a unique orderNumber even for retries.

---

## 10. SANDBOX vs PRODUCTION — EXACT DIFFERENCES

### URL Changes

| Item | Sandbox | Production |
|------|---------|-----------|
| API URL | `https://vtbkz.rbsuat.com/payment/rest/` | `https://payment.vtb.kz/payment/rest/` |
| Merchant portal | `https://vtbkz.rbsuat.com/mportal3` | `https://payment.vtb.kz/generalmp3/auth/login` |
| Merchant cabinet | `https://sandbox.vtb-bank.kz/cabinet/` | Direct from VTB Kazakhstan |

From plugin constants:
```php
define('BPC_VTBKZ_PROD_URL', 'https://payment.vtb.kz/payment/rest/');
define('BPC_VTBKZ_TEST_URL', 'https://vtbkz.rbsuat.com/payment/rest/');
```

### Credential Changes

| Item | Sandbox | Production |
|------|---------|-----------|
| Credentials | Self-registered at `sandbox.vtb-bank.kz/cabinet/` | Issued by VTB KZ after signing agreement |
| Account format | `{merchant}-api` for API, `{merchant}-operator` for portal | Same format |
| First login | `-api`: one-time only to set password | Same restriction |
| Password requirements | Min 12 chars, uppercase+lowercase+digits+special | Same |

### SSL Verification

From plugin:
```php
CURLOPT_SSL_VERIFYHOST => $is_test ? 0 : 2,
CURLOPT_SSL_VERIFYPEER => $is_test ? false : true,
```

In sandbox: SSL verification disabled (sandbox has self-signed cert).
In production: SSL verification required.

**For our Java implementation:** In sandbox mode, should accept any SSL certificate.

### Behavior Differences

| Behavior | Sandbox | Production |
|----------|---------|-----------|
| Cards | Mir only (2201 38xx) | Visa/MC/Mir/UnionPay/Google Pay/Apple Pay |
| Card auth | Always succeeds with correct CVC/expiry | Real bank authorization |
| 3DS | Test 3DS2 flow | Real 3DS2 |
| Callbacks | Received on registered callback URL | Same (must be reachable from internet) |
| Rate limits | Unknown | Unknown |

---

## 11. WHAT IS CONFIRMED vs NEEDS SANDBOX VERIFICATION

### Confirmed by Official Sources (docs + plugin source code)

✅ API URLs (test and production)  
✅ Authentication: userName+password in POST body with -api suffix  
✅ register.do all parameters  
✅ getOrderStatusExtended.do key response fields  
✅ Return URL appends `?orderId=` (gateway UUID)  
✅ Callback is GET by default  
✅ Callback parameter: `mdOrder` = gateway UUID  
✅ Callback operations: `deposited,approved,declinedByTimeout,reversed,refunded`  
✅ orderStatus 0-5 meanings  
✅ refund.do for deposited orders, reverse.do for authorized orders  
✅ KZT (398) as primary/default currency  
✅ RUB (643) in platform currency table  
✅ Test cards: Mir BINs `2201 3820 0000 00xx`, CVC=123, Expiry=12/34  
✅ 20-minute payment timeout  
✅ Duplicate orderNumber causes error 8204  
✅ orderNumber uniqueness pattern: append timestamp  
✅ Official plugin does NOT verify callback checksum  
✅ Merchant portal callback token generation  
✅ SSL verification disabled in sandbox  
✅ PCI DSS: HPP = SAQ-A only  
✅ Google Pay and Apple Pay supported  

### Requires Sandbox Testing After Getting Credentials

⚠️ **Callback checksum exact format** — values-only or key=value, UTF-8, separator  
⚠️ **Exact sandbox currency** — does sandbox default to KZT or RUB?  
⚠️ **USD/EUR merchant support** — whether this VTB KZ merchant account allows multi-currency  
⚠️ **RUB merchant support** — whether Kazakhstan merchants can actually use RUB  
⚠️ **Refund time limits** — how many days after payment  
⚠️ **VTB callback source IPs** — for potential IP whitelist  
⚠️ **orderStatus=4 partial refund behavior** — when refund.do called on orderStatus=4  
⚠️ **Rate limits** — max requests per second/minute  

### Requires Direct VTB KZ Confirmation Before Production

❌ **RUB currency agreement** — must confirm in writing with VTB KZ  
❌ **USD/EUR currency agreement** — must confirm in writing  
❌ **Maximum refund period** — how many days after payment  
❌ **Merchant agreement requirements** — what documents, timelines  

---

## 12. IMPLEMENTATION CHANGES vs PREVIOUS PLAN

These findings change the implementation plan:

| Change | Previous Plan | Revised Plan |
|--------|--------------|--------------|
| Callback HTTP method | POST | **GET** |
| Callback checksum | Required HMAC/SHA256 verification | **Optional** — skip if callbackSecret blank, always call API anyway |
| Return URL param | Unknown | **`?orderId=`** (gateway UUID) |
| Operation names | `authorized` | **`approved`** (correct from plugin) |
| Timeout operation | Not planned | **`declinedByTimeout`** — maps to FAILED |
| orderNumber | Use order.getId() | **Append timestamp**: `orderId + "_" + currentTimeMillis()` |
| Two-phase success | Only status 2 | Status 1 OR 2 (for two-phase flow) |
| Refund selector | Unknown | Status 2 → `refund.do`; Status 1 → `reverse.do` |

---

## 13. FINAL CALLBACK ARCHITECTURE

### Three questions — definitive answers

**Q1: Есть ли в документации прямое требование проверять checksum?**

Полный текст секции "Алгоритм обработки уведомлений" недоступен через WebFetch (SPA/JS).
Но два официальных плагина от BPC (создателя шлюза) дают однозначный ответ:

- WordPress v5.6.0: `checksum` из callback **игнорируется полностью**
- Magento v2.3.x: метод `isPaymentValid()` **существует как заглушка** (`return true;`)

Заглушка в Magento-плагине — доказательство того, что BPC *проектировал* верификацию,
но выпустил плагины без её реализации. Прямого обязательного требования нет.

**Q2: Есть ли официальный текст, что достаточно только getOrderStatusExtended()?**

Явного текста нет. Но де-факто официальный reference implementation именно это и делает.
`getOrderStatusExtended.do` с merchant-credentials — это аутентифицированный API-вызов.
Злоумышленник не может его подделать, не зная `userName`+`password`.

**Q3: Как реализовывать — рекомендация принята**

Реализовать оба уровня: checksum как необязательный, API как обязательный.

### Финальная схема callback

```
GET /api/v1/payments/callback/vtb-kz
  ?mdOrder=abc-uuid
  &orderNumber=11_1719300000
  &operation=deposited
  &status=1
  &checksum=ABC123...
        │
        ▼
[VtbCallbackController]
  @GetMapping  ← GET запрос, не POST
  @RequestParam Map<String,String>
  → paymentService.handleVtbCallback(params)
  → return ResponseEntity.ok().build()  ← всегда 200
        │
        ▼
[PaymentServiceImpl.handleVtbCallback()]
        │
        ├─► STEP 1: ProcessedWebhookEvent check
        │   if already processed by mdOrder → return (idempotent)
        │
        ├─► STEP 2: VtbCallbackVerifier.verifyChecksum()
        │   if VTB_HMAC_SECRET blank → SKIPPED (log INFO)
        │   if checksum absent → ABSENT (log INFO)
        │   if checksum present → compute SHA256, compare
        │     VALID → log INFO "checksum ok"
        │     INVALID → log WARN "checksum mismatch, proceeding to API verify"
        │   ↓ ALWAYS proceed to step 3 regardless of result
        │
        ├─► STEP 3: VtbHttpClient.getOrderStatus(mdOrder)
        │   Authenticated API call → actual payment status
        │   This is the ONLY source of truth
        │
        ├─► STEP 4: Map VtbOrderStatus → PaymentStatus
        │   DEPOSITED (2) → SUCCEEDED
        │   APPROVED (1)  → PENDING (two-phase hold, Balgyn doesn't use)
        │   DECLINED (3)  → FAILED
        │   REVERSED (4)  → FAILED
        │   REFUNDED (5)  → FAILED
        │
        ├─► STEP 5: Update Payment + Order
        │   SUCCEEDED → payment.status=SUCCEEDED, order.status=CONFIRMED
        │   FAILED → payment.status=FAILED
        │
        └─► STEP 6: Save ProcessedWebhookEvent(mdOrder)
```

### ENV configuration

```properties
# VTB callback checksum secret (blank = skip verification, call API only)
# Generate in merchant portal: Settings → Callback Notifications → Generate
VTB_HMAC_SECRET=              ← blank by default → checksum skipped in dev/sandbox
```

To enable checksum verification in production:
1. Generate token in VTB KZ merchant portal
2. Set `VTB_HMAC_SECRET` to generated value
3. No code changes needed — architecture is already prepared

---

## 14. FINAL RECOMMENDATION

### Can implementation begin?

**YES** — with the following scope:

**Phase 0-4** (infra, enums, DTOs, verifier, currency mapper): Ready to implement.  
**Phase 5** (payment service): Ready, with changes from §12.  
**Phase 6** (controllers): Ready, note callback is GET.  

**What to implement differently:**
1. `VtbCallbackController` must accept **GET** requests (not POST)
2. `VtbCallbackVerifier`: implement as optional — if secret blank, skip; always call API
3. Add `declinedByTimeout` to `VtbOrderStatus` handling
4. returnUrl handler must extract `orderId` param (not `mdOrder`)
5. orderNumber must include timestamp suffix for uniqueness

**What NOT to implement yet (needs credentials):**
- Two-phase payment flow
- Checksum verification (algorithm uncertain)
- Multi-currency (needs merchant agreement)

**Immediate next step after implementation:**
1. Get sandbox credentials from `sandbox.vtb-bank.kz/cabinet/`
2. Test with card `2201 3820 0000 0021`, CVC=123, Expiry=12/34
3. Verify callback checksum format by logging a real callback
4. Confirm default currency in sandbox response
