# VTB Kazakhstan Internet Acquiring — API Research

**Date:** 2026-06-25  
**Researcher:** Senior Backend Architect  
**Status:** Phase 1–4 Complete — REVISED after full portal study  
**Sources (all official):**
- Main documentation portal: https://sandbox.vtb-bank.kz
- REST API reference: https://sandbox.vtb-bank.kz/ru/integration/api/rest.html
- Redirect integration guide: https://sandbox.vtb-bank.kz/ru/integration/structure/redirect-integration.html
- Test cards: https://sandbox.vtb-bank.kz/ru/integration/structure/test-cards.html
- Action codes: https://sandbox.vtb-bank.kz/ru/integration/api/action_codes.html
- PCI DSS requirements: https://sandbox.vtb-bank.kz/ru/integration/certification.html
- Test-to-production: https://sandbox.vtb-bank.kz/ru/integration/structure/test-to-production.html
- Merchant portal docs: https://sandbox.vtb-bank.kz/ru/integration/mportal3/mp3.html
- VTB Kazakhstan corporate site: https://www.vtb-bank.kz

> **PLATFORM NOTE:** VTB Kazakhstan uses the RBS (Russian Banking System) payment gateway platform.
> The same platform is used by MTS Bank (mts.rbsuat.com), Sberbank (sbrf.ru), and
> Radar Payments (dev.radarpayments.com). All share identical API structure.
> This is important: the sandbox.vtb-bank.kz documentation directly corresponds to the
> production endpoint at payment.vtb.kz.

---

## PHASE 1 — API Research

### 1.1 Authentication

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/rest.html

Two authentication methods are supported. All three can be used per-request — passed in the POST body.

#### Method 1: Username + Password
```
userName = <API username, typically ends in -api suffix>
password = <API user password>
```
The API user account is created in the merchant cabinet. By convention the login has `-api` suffix (e.g., `merchant123-api`).

#### Method 2: Token
```
token = <token value from technical support>
```
Replaces userName/password in all requests. Requested from VTB Kazakhstan technical support after merchant registration.

#### Method 3: Request Signature (P2P/AFT/OCT only)
For sensitive direct transfer operations only (not standard acquiring).
- Header `X-Hash`: SHA-256 hash of request body, encoded as Base64
- Header `X-Signature`: RSA private key signature of `X-Hash`
- Requires 2048-bit RSA key pair; public certificate uploaded via merchant dashboard
- **NOT required for standard internet acquiring flows**

---

### 1.2 Environments

| Environment | Base URL |
|-------------|----------|
| **TEST (Sandbox)** | `https://vtbkz.rbsuat.com/payment/rest/` |
| **PRODUCTION** | `https://payment.vtb.kz/payment/rest/` |

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/rest.html

Sandbox merchant credentials are obtained by creating an account in the merchant cabinet at `https://sandbox.vtb-bank.kz/cabinet/`.

---

### 1.3 Creating a Payment

**Endpoint:** `POST {base_url}register.do`  
**Content-Type:** `application/x-www-form-urlencoded` OR `application/json`

#### Required Parameters

| Parameter | Type | Max Length | Description |
|-----------|------|-----------|-------------|
| `orderNumber` | String | 36 | Unique order ID in merchant's system |
| `amount` | Integer | 12 digits | Amount in **minor units** (tiyn for KZT; 1 KZT = 100 tiyn) |
| `returnUrl` | String | 512 | Success redirect URL (full URL with protocol) |
| `userName` + `password` OR `token` | String | — | Authentication |

#### Optional Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `currency` | String (3 digits) | ISO 4217 numeric code (e.g., `398` for KZT). Default: merchant's base currency |
| `failUrl` | String | Failure redirect URL |
| `description` | String (598) | Order description displayed on payment form |
| `language` | String | `ru`, `en`, `hy`, `az` |
| `ip` | String | Cardholder IP (IPv4/IPv6) |
| `email` | String (40) | Customer email |
| `clientId` | String (255) | Customer identifier for saved card bindings |
| `sessionTimeoutSecs` | Integer | Order lifetime in seconds (1–9 digits) |
| `expirationDate` | String | Order expiry: `yyyy-MM-ddTHH:mm:ss` |
| `dynamicCallbackUrl` | String | Override callback URL per-payment |
| `features` | String | `FORCE_TDS`, `FORCE_SSL`, `FORCE_FULL_TDS`, `FORCE_CREATE_BINDING`, `AUTO_PAYMENT` |
| `jsonParams` | JSON | Custom key-value attributes (for receipt, etc.) |

#### Response

```json
{
  "orderId": "abc123-uuid",
  "formUrl": "https://vtbkz.rbsuat.com/payment/merchants/.../payment_kz.html?mdOrder=abc123-uuid",
  "errorCode": "0",
  "errorMessage": null
}
```

- `orderId` — internal gateway UUID for this order (called `mdOrder` in callbacks)
- `formUrl` — URL to redirect the customer's browser to
- `errorCode` 0 = success; any other value = error

---

### 1.4 Hosted Payment Page (HPP)

After calling `register.do`, redirect the customer to `formUrl`. VTB Kazakhstan's hosted page:
- Renders the card entry form
- Handles 3DS 2.x challenge
- Redirects to `returnUrl` on success, `failUrl` on failure
- Supports `language` parameter (ru/en)

**PCI DSS level for this flow:** SAQ-A (no card data touches merchant server).

---

### 1.5 Direct Payment via API (Seller API)

**Endpoint:** `POST {base_url}paymentorder.do`

This endpoint allows the merchant to present their own payment form and process card data directly. **Requires PCI-DSS SAQ-D compliance (full audit), ASV scanning, and Attestation of Compliance (AoC).**

**Source:** https://sandbox.vtb-bank.kz/ru/integration/structure/advanced-integration.html

Required parameters for direct payment:
- `MDORDER` — gateway orderId from register.do
- `$PAN` or `seToken` — card number or tokenized card
- `$CVC` — CVV/CVC
- `YYYY` + `MM` or `$EXPIRY` — expiry date
- `TEXT` — cardholder name
- Authentication (userName/password or token)

**⚠️ NOT RECOMMENDED** unless merchant has full PCI DSS certification. Use HPP instead.

---

### 1.6 Payment Status Check

**Endpoint:** `POST {base_url}getOrderStatusExtended.do`

**Required:** `orderId` (gateway UUID) OR `orderNumber` (merchant order ID) + authentication.

**Key response fields:**

| Field | Values | Meaning |
|-------|--------|---------|
| `orderStatus` | 0 | Registered, not yet paid |
| | 1 | Pre-authorized (hold placed, 2-phase only) |
| | 2 | Deposited — **PAYMENT COMPLETE** |
| | 3 | Declined |
| | 4 | Reversed (cancelled) |
| | 5 | Refunded |
| `amount` | Integer | Amount in minor units |
| `currency` | String | ISO 4217 numeric code |
| `orderNumber` | String | Original merchant order ID |
| `authCode` | String | Bank authorization code |
| `ip` | String | Cardholder IP |

**Success condition:** `orderStatus == 2` (Deposited).

> **⚠️ CRITICAL:** HTTP 200 from register.do or callback does NOT mean payment succeeded.
> Always verify via `getOrderStatusExtended.do` after receiving callback.

---

### 1.7 Webhook / Callback

**Source (authoritative):** Official VTB Kazakhstan WooCommerce plugin v5.6.0  
**Evidence:** `$data['callback_http_method'] = "GET";` from plugin `_updateGatewayCallback`

> ⚠️ **CORRECTION**: VTB sends callback as a **GET** request (NOT POST).
> Callback parameters arrive in the **URL query string**.

#### Callback URL Format

```
GET {merchantCallbackUrl}?mdOrder={vtbUUID}&orderNumber={merchantOrderNumber}&operation={operation}&status={status}&checksum={sha256hex}[&amount={minorUnits}]
```

#### Callback Parameters

| Parameter | Description |
|-----------|-------------|
| `mdOrder` | Gateway UUID (= `orderId` from register.do response) |
| `orderNumber` | Merchant's original order number |
| `checksum` | SHA256 signature — see §1.14 (optional to verify) |
| `operation` | Event: `deposited`, `approved`, `declinedByTimeout`, `reversed`, `refunded` |
| `status` | 1 = success, 0 = failure |
| `amount` | Amount in minor units (may NOT be present — do not rely on it) |

#### Callback Types

| Operation | Trigger |
|-----------|---------|
| `deposited` | Payment completed (one-phase or two-phase deposit) |
| `approved` | Pre-authorization placed (two-phase only) — **NOT `authorized`** |
| `declinedByTimeout` | 20-minute payment timeout expired |
| `reversed` | Payment reversed/cancelled |
| `refunded` | Refund processed |

> ⚠️ **CORRECTION**: Operation name is `approved`, NOT `authorized`.  
> Confirmed: `$data['callback_operations'] = "deposited,approved,declinedByTimeout,reversed,refunded";`

#### Processing Security Pattern (confirmed from official plugin)

Both WordPress v5.6.0 and Magento v2.3.x plugins skip checksum verification entirely.
The callback only triggers an API call — it is NOT trusted directly:

```
Callback GET received → extract mdOrder → call getOrderStatusExtended.do → act ONLY on API response
```

`getOrderStatusExtended.do` with merchant credentials is the single source of truth.

#### Retry Logic
If merchant returns non-200, VTB retries at intervals of `10 * attempt_number` minutes.
Example: attempt 1 → 10 min, attempt 2 → 20 min, attempt 3 → 30 min.

#### Merchant Must Return
HTTP 200 OK — no body required.

---

### 1.8 Refunds

**Endpoint:** `POST {base_url}refund.do`

| Parameter | Required | Description |
|-----------|----------|-------------|
| `orderId` | Yes | Gateway UUID |
| `amount` | Optional | Amount to refund in minor units. If omitted = full refund |
| Authentication | Yes | userName/password or token |

- **Full refund**: call without `amount` — refunds entire payment
- **Partial refund**: pass `amount` < original amount
- Can be called multiple times if total ≤ original
- Order status becomes 5 (Refunded) after full refund
- Requires merchant to have refund permission (configured by VTB)

**Response:**
```json
{ "errorCode": 0 }
```

---

### 1.9 Payment Cancellation (Reverse)

**Endpoint:** `POST {base_url}reverse.do`

Cancels an **authorized but not deposited** payment (two-phase flow, `orderStatus == 1`).

| Parameter | Required | Description |
|-----------|----------|-------------|
| `orderId` | Yes | Gateway UUID |
| Authentication | Yes | userName/password or token |

Order status becomes 4 (Reversed) after successful reverse.

---

### 1.10 Cancel Order

**Endpoint:** `POST {base_url}cancel.do`

Cancels a **registered but unpaid** order (`orderStatus == 0`). Frees the order slot.

---

### 1.11 Partial Refund

Supported via `refund.do` with `amount` parameter.

**Confirmed:** The `amount` parameter in `refund.do` is optional — when provided, it's a partial refund.

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/rest.html (refund.do endpoint)

---

### 1.12 Idempotency

**Status: NOT explicitly documented in official VTB Kazakhstan API docs.**

The API uses `orderNumber` (merchant-assigned) as the order identifier. Attempting to register a duplicate `orderNumber` will return an error. Merchants should implement their own idempotency using the `orderNumber` uniqueness constraint.

For callbacks, idempotency must be implemented on the merchant side (deduplicate by `mdOrder` — same as the existing `ProcessedWebhookEvent` pattern in this project).

---

### 1.13 Request Signature (standard acquiring)

For standard HPP acquiring, **no per-request signature is required**. Authentication is via userName/password or token in each request body.

For P2P/direct transfer operations only (not standard e-commerce):
- RSA 2048-bit key pair
- `X-Hash` = Base64(SHA-256(request_body))
- `X-Signature` = Base64(RSA_sign_private_key(X-Hash))

---

### 1.14 Callback Signature Verification

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/rest.html#callback-notifications (official)  
**Source:** https://sandbox.vtb-bank.kz/ru/integration/mportal3/mp3.html (portal configuration)

> ⚠️ **EARLIER VERSION OF THIS DOC WAS WRONG**: Previously stated HMAC-SHA256 (based on
> Radar Payments docs). Official VTB Kazakhstan docs describe a **SHA256 concatenation**
> algorithm — same family as FreedomPay MD5, but with SHA256.

The callback `checksum` is computed using **SHA256 concatenation** (symmetric) or RSA (asymmetric).

**Symmetric SHA256 algorithm (standard configuration):**

Formula from official docs: `SHA256(param1;param2;...;paramN;callbackSecret)`

1. Extract the `checksum` value from callback parameters
2. Remove `checksum` from the parameter map
3. Sort remaining parameters alphabetically by key name
4. Build semicolon-delimited string of parameters + append callbackSecret
5. Compute `SHA256(verification_string)` — plain SHA256, NOT HMAC
6. Uppercase the computed hex digest
7. Compare with the received `checksum` value (case-insensitive)

**⚠️ Open question from official docs**: Whether step 4 uses **values only** or **key=value pairs** — must be verified with sandbox callback test before implementation.

**The callbackSecret** is generated in the merchant portal:
- Path: Settings → Merchant → Callback Notifications → Generate token
- Can be regenerated at any time (revert within 5 minutes)
- This is a separate secret from the API password

**Asymmetric RSA verification (optional, more secure):**  
Contact VTB KZ support to obtain asymmetric key. Merchant portal supports selecting between symmetric/asymmetric.

---

### 1.15 Security Checks

1. **Always verify callback checksum** before acting on status change
2. **Always call `getOrderStatusExtended.do`** after receiving callback — do not trust callback status alone
3. **Deduplicate callbacks** by `mdOrder` — VTB may send duplicate callbacks on retry
4. **Validate amounts** — compare callback amount with expected order amount
5. **HTTPS only** — merchant callback URL must use HTTPS with valid SSL certificate
6. **No card data on server** — use HPP flow (SAQ-A) to avoid PCI scope

---

### 1.16 Sandbox (Test Environment)

| Item | Value |
|------|-------|
| **Sandbox API URL** | `https://vtbkz.rbsuat.com/payment/rest/` |
| **Documentation portal** | `https://sandbox.vtb-bank.kz` |
| **Merchant cabinet (sandbox)** | `https://sandbox.vtb-bank.kz/cabinet/` |
| **Merchant portal (sandbox)** | `https://vtbkz.rbsuat.com/mportal3` |
| **Credentials** | Created via merchant cabinet self-registration |

> ⚠️ **CORRECTED TEST CARDS**: Earlier version listed `4000001111111118` — WRONG.
> Official VTB Kazakhstan sandbox test cards are **Mir payment system** (BIN 2201 38xx).

**Test cards (all: CVC=123, Expiry=12/34, 3DS2):**

| Card Number | Type | Result |
|-------------|------|--------|
| `2201 3820 0000 0021` | Mir, Full 3DS2 | ✅ Success |
| `2201 3820 0000 0039` | Mir, Full 3DS2 | ✅ Success |
| `2201 3820 0000 0013` | Mir, Full 3DS2 | ✅ Success |
| `2201 3820 0000 0047` | Mir, Full 3DS2 | ✅ Success |
| `2201 3820 0000 0054` | Mir, Full 3DS2 | ✅ Success |
| `2201 3820 0000 0062` | Mir, Frictionless | ✅ Success |

**To simulate failure**: use any card with incorrect CVC or expired date → error code `71015`

> **Note on sandbox currency**: The redirect integration doc example shows `currency=643` (RUB).
> This may be a copy from Russian VTB docs or indicate sandbox defaults to RUB.
> Verify actual sandbox currency behavior during credential setup.

---

### 1.17 Production

| Item | Value |
|------|-------|
| **Production API URL** | `https://payment.vtb.kz/payment/rest/` |
| **Merchant portal (prod)** | `https://payment.vtb.kz/generalmp3/auth/login` |
| **Merchant onboarding** | Contract with VTB Kazakhstan required |
| **Credentials** | Issued with `-api` and `-operator` suffixes via email |
| **Password requirements** | Min 12 chars; uppercase, lowercase, digits, special chars |
| **First login** | `-api` account: one-time login to set password, then reserved for API use only |
| **Portal access** | Use `-operator` account for daily portal access (transactions, refunds, etc.) |

**Test-to-production steps:**
1. Receive credentials email with `-api` and `-operator` accounts + temp passwords
2. Change passwords (min 12 chars, must include uppercase/lowercase/digits/special)
3. Switch API base URL to `https://payment.vtb.kz/payment/rest/`
4. Configure callback URL in portal
5. Run one test transaction with real card
6. Verify in portal under Transactions

---

## PHASE 2 — Currency Support

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/rest.html (ISO 4217 codes accepted)  
**Source:** MTS Bank documentation (same platform): https://mts.rbsuat.com/sandbox/integration/api/rest.html

Currency is passed as `currency` parameter (ISO 4217 **numeric** code).

| Currency | ISO 4217 Code | Create Payment | Refund | Notes |
|----------|--------------|----------------|--------|-------|
| **KZT** | `398` | ✅ Confirmed | ✅ Confirmed | Primary currency of VTB Kazakhstan. Default. |
| **USD** | `840` | ✅ Supported by platform | ✅ | May require separate merchant account per VTB KZ policy |
| **EUR** | `978` | ✅ Supported by platform | ✅ | May require separate merchant account per VTB KZ policy |
| **RUB** | `643` | ⚠️ **NOT CONFIRMED** | ⚠️ | See Phase 4 |

> **IMPORTANT:** The platform technically supports multiple currencies. However, whether
> a specific Kazakhstan merchant can accept USD/EUR/RUB depends on their merchant agreement
> with VTB Kazakhstan. **This is NOT documented publicly.** Direct confirmation from VTB KZ
> is required for USD/EUR/RUB acceptance.

---

## PHASE 3 — Card Type Support

**Source:** https://en.vtb-bank.kz/news/1695/ ("VTB launches servicing of 'Mir' cards in the Republic of Kazakhstan")  
**Source:** https://en.vtb-bank.kz/individuals/international-payment-cards/ (lists Visa, Mastercard, UnionPay)  
**Source:** https://mts.rbsuat.com/sandbox/integration/api/rest.html (same platform — confirms Mir/UnionPay support)

| Card Type | Supported | Notes |
|-----------|-----------|-------|
| **Visa** | ✅ Confirmed | Standard international card |
| **Mastercard** | ✅ Confirmed | Standard international card |
| **Мир (Mir)** | ✅ Confirmed | VTB Kazakhstan was **first bank in Kazakhstan** to accept Mir |
| **UnionPay** | ✅ Confirmed | Supported per VTB KZ FAQ and documentation |
| **JCB** | ❓ Not documented | No mention in any official VTB Kazakhstan documentation found |
| **American Express** | ❓ Not documented | Not mentioned in VTB Kazakhstan sources |

### Mir Card — Conditions

VTB Kazakhstan is the first bank in Kazakhstan to have launched Mir card acceptance and servicing. Key conditions:

1. **Issuance**: VTB Kazakhstan issues Mir plastic and virtual cards — confirmed operational
2. **Transfers**: Outgoing transfers via Mir to Russia confirmed (Dec 2025 news)
3. **Internet acquiring (accepting Mir payments from customers)**: ✅ VTB Kazakhstan was first to launch Mir acceptance in Kazakhstan

> **CONTEXT (2022–2026):** Following international sanctions, Mir cards are restricted globally.
> Acceptance is available in: Russia, Belarus, Kazakhstan (VTB KZ confirmed), Armenia, Kyrgyzstan, 
> and select other countries. For Kazakhstan merchants accepting Mir cards from customers — 
> VTB Kazakhstan is one of the few acquirers who can process this.

---

## PHASE 4 — RUB (Russian Ruble) Analysis

**Source:** No explicit official documentation found for RUB acquiring at VTB Kazakhstan merchant terminals.

### Platform Capability
The RBS payment gateway platform (vtbkz.rbsuat.com) accepts ISO 4217 numeric currency codes. RUB = `643`. The platform technically supports multi-currency.

### Legal/Regulatory Reality
VTB Kazakhstan (ДО АО Банк ВТБ (Казахстан)) is a Kazakhstani legal entity regulated by the Agency of the Republic of Kazakhstan for Regulation and Development of the Financial Market (ARRFR RK). Their license number is ARRFR RK №1.2.14/39.

As a Kazakhstan-licensed bank, merchant accounts are typically opened in **KZT**. RUB acquiring would require:
1. Merchant having a separate RUB account at VTB Kazakhstan
2. Explicit contractual agreement for RUB acquiring
3. Possible NBRK (National Bank of Kazakhstan) foreign exchange regulations compliance

### Conclusion on RUB

| Question | Answer |
|----------|--------|
| Does the platform support RUB? | ✅ Yes (ISO 643) |
| Is RUB standard for Kazakhstan merchants? | ❌ No — KZT is standard |
| Can a Kazakhstan merchant accept RUB? | ⚠️ **Unknown** — requires individual agreement |
| Does it require a separate merchant? | ⚠️ **Unknown** — likely yes (separate RUB account) |
| Is RUB flow different? | ⚠️ **Unknown** — if supported, same API but `currency=643` |
| Official documentation confirms RUB? | ❌ **Not found in public docs** |

**RECOMMENDATION:** Contact VTB Kazakhstan directly to determine RUB acquiring availability. Do not assume it is supported without written confirmation.

---

## Key API Error Codes

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/rest.html

| HTTP Status | Meaning |
|-------------|---------|
| 200 | Request received — check `errorCode` in body |
| 400 | Internal system error |
| 404 | Invalid API URL |
| 429 | Rate limit exceeded |
| 500/502 | Server-side error |

| errorCode | Meaning |
|-----------|---------|
| 0 | Success |
| 1–99 | Various errors (authentication, validation, etc.) |

**orderStatus in getOrderStatusExtended.do:**

| Value | Meaning |
|-------|---------|
| 0 | New (registered, not yet paid) |
| 1 | Pre-authorized (2-phase hold) |
| 2 | **Deposited = PAID** |
| 3 | Declined |
| 4 | Reversed (cancelled) |
| 5 | Refunded |

---

## Key Action Codes

**Source:** https://sandbox.vtb-bank.kz/ru/integration/api/action_codes.html

| Code | Category | Meaning |
|------|----------|---------|
| `0` / `000` | ✅ Success | Payment processed successfully |
| `-100` | Pending | Awaiting payment attempt |
| `20` | Card | Insufficient funds |
| `62` | Card | Card compromised (lost/stolen) |
| `82` / `88` | Card | Invalid CVC |
| `101` | Card | Card expired |
| `106` | Card | Card blocked (PIN attempts exceeded) |
| `111` / `125` | Card | Invalid card number |
| `71015` | Input | Invalid payment data (wrong CVC/expiry) |
| `-2006` | 3DS | 3DS verification failed by issuer |
| `-2007` | Timeout | Payment timeout (default 20 minutes) |
| `-2009` | 3DS | Cardholder didn't return from ACS |
| `-2014` | Abandon | Payer didn't return from payment page |
| `-2025` | 3DS | Issuer declined with RReq — disable VPN |
| `2002` | Status | Invalid operation for current order status |
| `2003` | 3DS | Payments require 3D Secure |
| `2012` | Status | Order already cancelled |
| `8204` | Duplicate | Duplicate order number |
| `-20010` | Limits | Payment exceeds issuer limits |
| `121` | Limits | Daily transaction limit exceeded |
| `151` | Fraud | Fraud monitoring triggered |
| `909` | System | System error (general) |
| `912` | System | Issuer bank unavailable |
| `941` | Config | Invalid merchant ID |
| `-30001` | Pending | Operation under review (poll initiated) |

---

## PCI DSS Requirements

**Source:** https://sandbox.vtb-bank.kz/ru/integration/certification.html

| Integration Type | PCI Level | Requirements |
|-----------------|-----------|--------------|
| **Pay-by-link** | None | No PCI DSS actions required |
| **HPP (Redirect)** | SAQ-A | Self-assessment form (mandatory L1–L3, recommended L4) |
| **Direct API** | SAQ-D | Self-assessment + quarterly ASV scans + AoC certificate |

**Recommendation for Balgyn store:** Use HPP (redirect) flow → SAQ-A only.

---

## What Is NOT in Official Documentation

The following were NOT found in any official VTB Kazakhstan documentation and require direct contact or sandbox testing:

1. **Exact callback verification string format** — whether Step 4 uses values-only or key=value pairs (SHA256 formula is documented but param format is ambiguous)
2. **Explicit RUB currency support for Kazakhstan merchants** — platform supports it technically, but merchant agreement required
3. **JCB card support** — not mentioned in any source
4. **Public test credentials** — only obtainable via merchant cabinet self-registration
5. **Rate limiting specifics** — no documented limits beyond HTTP 429
6. **Idempotency guarantees** — not documented
7. **Maximum refund timeframe** — how many days after payment refunds are allowed
8. **Webhook IP whitelist** — whether VTB publishes their callback source IPs
9. **Exact sandbox currency behavior** — docs show `currency=643` example, but sandbox for Kazakhstan should use 398
